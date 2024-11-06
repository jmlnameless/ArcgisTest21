package com.example.arcgistest2.layer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
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

public class LayerManager {
    private static final String TAG = "LayerManager";
    private final Context context;
    private final MapView mapView;
    private final ArcGISMap map;
    private final List<FeatureLayer> layers;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    
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
        executorService.execute(() -> {
            try {
                File file = new File(path);
                if (!file.exists()) {
                    postError(callback, "文件不存在: " + path);
                    return;
                }

                ShapefileFeatureTable featureTable = new ShapefileFeatureTable(path);
                featureTable.loadAsync();

                featureTable.addDoneLoadingListener(() -> {
                    if (featureTable.getLoadStatus() == LoadStatus.LOADED) {
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
                                postError(callback, "图层加载失败: " + layer.getLoadError().getMessage());
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
        executorService.shutdown();
        layers.clear();
    }
} 