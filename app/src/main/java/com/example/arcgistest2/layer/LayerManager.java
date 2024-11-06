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
    
    // 添加内存管理相关的常量
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

    public void loadShapefileLayer(@NonNull String path, @NonNull LayerLoadCallback callback) {
        // 检查图层数量限制
        if (layers.size() >= MAX_LAYERS) {
            postError(callback, "图层数量已达到上限，请先移除一些图层");
            return;
        }

        executorService.execute(() -> {
            try {
                // 检查文件是否存在
                File file = new File(path);
                if (!file.exists()) {
                    postError(callback, "文件不存在: " + path);
                    return;
                }

                // 检查文件大小
                if (file.length() > MAX_FILE_SIZE) {
                    postError(callback, "文件过大，超过50MB限制");
                    return;
                }

                // 检查相关文件是否完整
                if (!checkShapefileFiles(path)) {
                    postError(callback, "Shapefile文件不完整，请确保.shp、.shx、.dbf、.prj文件都存在");
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
                        layer.setMaxScale(0); // 不限制最大比例
                        layer.setMinScale(0); // 不限制最小比例
                        
                        // 异步加载图层
                        layer.loadAsync();
                        layer.addDoneLoadingListener(() -> {
                            if (layer.getLoadStatus() == LoadStatus.LOADED) {
                                mainHandler.post(() -> {
                                    // 如果有太多图层，移除最早添加的图层
                                    if (layers.size() >= MAX_LAYERS) {
                                        FeatureLayer oldestLayer = layers.get(0);
                                        map.getOperationalLayers().remove(oldestLayer);
                                        layers.remove(0);
                                    }

                                    map.getOperationalLayers().add(layer);
                                    layers.add(layer);
                                    callback.onSuccess(layer);
                                });
                            } else {
                                postError(callback, "图层加载失败: " + layer.getLoadError().getMessage());
                                // 从地图中移除图层
                                map.getOperationalLayers().remove(layer);
                            }
                        });
                    } else {
                        postError(callback, "要素表加载失败: " + featureTable.getLoadError().getMessage());
                    }
                });
            } catch (Exception e) {
                postError(callback, "加载图层异常: " + e.getMessage());
            }
        });
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
} 