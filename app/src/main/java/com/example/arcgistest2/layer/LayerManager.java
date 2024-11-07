package com.example.arcgistest2.layer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.MapView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LayerManager {
    private static final String TAG = "LayerManager";
    private final Context context;
    private final MapView mapView;
    private final ArcGISMap map;
    private final List<FeatureLayer> layers;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    
    // 添加内存管理相的常量
    private static final int MAX_LAYERS = 10; // 最大图层数量
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 最大文件大小（50MB）

    public interface LayerLoadCallback {
        void onSuccess(FeatureLayer layer);
        void onError(String error);
    }

    public LayerManager(Context context, MapView mapView, ArcGISMap map) {
        this.context = context;
        this.mapView = mapView;
        this.map = map;
        this.layers = new ArrayList<>();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void loadShapefileLayer(String path, LayerLoadCallback callback) {
        try {
            // 检查参数
            if (path == null || callback == null) {
                throw new IllegalArgumentException("Path and callback cannot be null");
            }

            // 检查文件是否存在
            File shpFile = new File(path);
            if (!shpFile.exists()) {
                callback.onError("Shapefile不存在: " + path);
                return;
            }

            // 创建要素表
            ShapefileFeatureTable featureTable = new ShapefileFeatureTable(path);
            
            // 加载要素表
            featureTable.loadAsync();
            featureTable.addDoneLoadingListener(() -> {
                if (featureTable.getLoadStatus() == LoadStatus.LOADED) {
                    // 创建要素图层
                    FeatureLayer layer = new FeatureLayer(featureTable);
                    layer.loadAsync();
                    layer.addDoneLoadingListener(() -> {
                        if (layer.getLoadStatus() == LoadStatus.LOADED) {
                            mainHandler.post(() -> {
                                map.getOperationalLayers().add(layer);
                                layers.add(layer);
                                callback.onSuccess(layer);
                            });
                        } else {
                            String error = layer.getLoadError() != null ? 
                                layer.getLoadError().getMessage() : "Unknown error";
                            mainHandler.post(() -> callback.onError("图层加载失败: " + error));
                        }
                    });
                } else {
                    String error = featureTable.getLoadError() != null ? 
                        featureTable.getLoadError().getMessage() : "Unknown error";
                    mainHandler.post(() -> callback.onError("要素表加载失败: " + error));
                }
            });
        } catch (Exception e) {
            mainHandler.post(() -> callback.onError("加载Shapefile失败: " + e.getMessage()));
        }
    }

    // 检查Shapefile相关文件是否完整
    private boolean checkShapefileFiles(String shpPath) {
        String basePath = shpPath.substring(0, shpPath.length() - 4);
        String[] extensions = {".shp", ".shx", ".dbf", ".prj"};
        
        for (String ext : extensions) {
            File file = new File(basePath + ext);
            if (!file.exists()) {
                Log.e(TAG, "缺少文件: " + basePath + ext);
                return false;
            }
        }
        return true;
    }

    private void postError(LayerLoadCallback callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }

    public void removeLayer(FeatureLayer layer) {
        mainHandler.post(() -> {
            map.getOperationalLayers().remove(layer);
            layers.remove(layer);
        });
    }

    public void setLayerVisibility(FeatureLayer layer, boolean visible) {
        mainHandler.post(() -> layer.setVisible(visible));
    }

    public void zoomToLayer(FeatureLayer layer) {
        if (layer != null && layer.getFullExtent() != null) {
            Envelope extent = layer.getFullExtent();
            // 添加缓冲区
            Envelope bufferedExtent = new Envelope(
                extent.getXMin() - extent.getWidth() * 0.1,
                extent.getYMin() - extent.getHeight() * 0.1,
                extent.getXMax() + extent.getWidth() * 0.1,
                extent.getYMax() + extent.getHeight() * 0.1,
                extent.getSpatialReference()
            );
            mainHandler.post(() -> mapView.setViewpointGeometryAsync(bufferedExtent, 100));
        }
    }

    public List<FeatureLayer> getLayers() {
        return new ArrayList<>(layers);
    }

    public void dispose() {
        clearLayers();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    // 清理所有图层
    public void clearLayers() {
        mainHandler.post(() -> {
            for (FeatureLayer layer : layers) {
                map.getOperationalLayers().remove(layer);
            }
            layers.clear();
            System.gc(); // 建议垃圾回收
        });
    }

    // 在 LayerManager 类中添加导出方法
    public void exportLayerToShapefile(FeatureLayer layer, String outputPath) {
        try {
            // 获取要素表
            ShapefileFeatureTable featureTable = (ShapefileFeatureTable) layer.getFeatureTable();
            
            // 创建输出目录
            File outputDir = new File(outputPath).getParentFile();
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // 复制必要的文件
            String basePath = outputPath.substring(0, outputPath.length() - 4);
            String[] extensions = {".shp", ".shx", ".dbf", ".prj"};
            
            for (String ext : extensions) {
                File sourceFile = new File(featureTable.getPath().substring(0, 
                    featureTable.getPath().length() - 4) + ext);
                File destFile = new File(basePath + ext);
                
                if (sourceFile.exists()) {
                    copyFile(sourceFile, destFile);
                }
            }

            Log.d(TAG, "图层导出成功: " + outputPath);
        } catch (Exception e) {
            Log.e(TAG, "导出图层失败: " + e.getMessage());
            throw new RuntimeException("导出图层失败: " + e.getMessage());
        }
    }

    private void copyFile(File source, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }
} 