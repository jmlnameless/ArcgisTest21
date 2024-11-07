package com.example.arcgistest2.editor;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureCollection;
import com.esri.arcgisruntime.data.FeatureCollectionTable;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.layers.FeatureCollectionLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.Renderer;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.data.QueryParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureEditor {
    private static final String TAG = "FeatureEditor";
    private final MapView mapView;
    private final Context context;
    private GeometryType currentGeometryType;
    private GraphicsOverlay graphicsOverlay;
    private PointCollection points;
    private boolean isEditing = false;
    private FeatureLayer currentLayer;
    private FeatureCollectionTable currentTable;

    // 添加一个回调接口用于通知图层创建
    public interface LayerCreationCallback {
        void onLayerCreated(FeatureLayer layer, String layerName);
    }
    
    private LayerCreationCallback layerCreationCallback;
    private List<Point> pointFeatures = new ArrayList<>(); // 存储多个点要素

    public FeatureEditor(MapView mapView, Context context) {
        this.mapView = mapView;
        this.context = context;
        this.graphicsOverlay = new GraphicsOverlay();
        this.mapView.getGraphicsOverlays().add(graphicsOverlay);
    }

    public void setCurrentGeometryType(GeometryType geometryType) {
        this.currentGeometryType = geometryType;
    }

    public GeometryType getCurrentGeometryType() {
        return currentGeometryType;
    }

    public void startEditing() {
        if (currentGeometryType == null) {
            Toast.makeText(context, "请先选择要素类型", Toast.LENGTH_SHORT).show();
            return;
        }

        isEditing = true;
        points = new PointCollection(mapView.getSpatialReference());
        graphicsOverlay.getGraphics().clear();
    }

    public void addPoint(Point point) {
        if (!isEditing) return;

        switch (currentGeometryType) {
            case POINT:
                // 对于点要素，直接添加到列表中
                pointFeatures.add(point);
                // 显示预览
                showPointPreview(point);
                break;
            default:
                // 对于线和面要素，添加到点集合中
                points.add(point);
                updatePreviewGraphic();
                break;
        }
    }

    private void showPointPreview(Point point) {
        SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(
            SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 10);
        Graphic graphic = new Graphic(point, pointSymbol);
        graphicsOverlay.getGraphics().add(graphic);
    }

    private void updatePreviewGraphic() {
        graphicsOverlay.getGraphics().clear();
        
        if (points.size() == 0) return;

        Graphic graphic = null;
        switch (currentGeometryType) {
            case POINT:
                SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(
                    SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 10);
                graphic = new Graphic(points.get(points.size() - 1), pointSymbol);
                break;

            case POLYLINE:
                if (points.size() > 1) {
                    SimpleLineSymbol lineSymbol = new SimpleLineSymbol(
                        SimpleLineSymbol.Style.SOLID, Color.BLUE, 2);
                    Polyline polyline = new Polyline(points);
                    graphic = new Graphic(polyline, lineSymbol);
                }
                break;

            case POLYGON:
                if (points.size() > 2) {
                    SimpleLineSymbol outlineSymbol = new SimpleLineSymbol(
                        SimpleLineSymbol.Style.SOLID, Color.BLUE, 2);
                    SimpleFillSymbol fillSymbol = new SimpleFillSymbol(
                        SimpleFillSymbol.Style.SOLID, Color.argb(100, 0, 0, 255), outlineSymbol);
                    Polygon polygon = new Polygon(points);
                    graphic = new Graphic(polygon, fillSymbol);
                }
                break;
        }

        if (graphic != null) {
            graphicsOverlay.getGraphics().add(graphic);
        }
    }

    public void completeDrawing() {
        if (!isEditing) return;

        try {
            if (currentLayer == null || currentTable == null) {
                createNewFeatureLayer();
                // 等待图层创建完成
                Thread.sleep(1000); // 增加等待时间
            }

            if (currentTable == null || currentLayer == null) {
                throw new IllegalStateException("图层未正确创建");
            }

            switch (currentGeometryType) {
                case POINT:
                    // 添加所有点要素
                    if (pointFeatures.isEmpty()) {
                        throw new IllegalStateException("没有要素可添加");
                    }
                    for (Point point : pointFeatures) {
                        Map<String, Object> attributes = new HashMap<>();
                        attributes.put("OBJECTID", generateObjectId());
                        attributes.put("FEAT_NAME", "点要素");
                        attributes.put("FEAT_DESC", "自动创建的点要素");
                        attributes.put("FEAT_ID", generateFeatureId());
                        
                        Feature feature = currentTable.createFeature(attributes, point);
                        ListenableFuture<Void> future = currentTable.addFeatureAsync(feature);
                        future.addDoneListener(() -> {
                            try {
                                future.get();
                                Log.d(TAG, "点要素添加成功");
                            } catch (Exception e) {
                                Log.e(TAG, "添加点要素失败: " + e.getMessage());
                            }
                        });
                    }
                    break;
                    
                default:
                    // 处理线和面要素
                    if (points.size() >= getMinimumPointsRequired()) {
                        Map<String, Object> attributes = new HashMap<>();
                        attributes.put("OBJECTID", generateObjectId());
                        attributes.put("FEAT_NAME", getGeometryTypeName());
                        attributes.put("FEAT_DESC", "自动创建的" + getGeometryTypeName());
                        attributes.put("FEAT_ID", generateFeatureId());
                        
                        com.esri.arcgisruntime.geometry.Geometry geometry = createGeometry();
                        Feature feature = currentTable.createFeature(attributes, geometry);
                        ListenableFuture<Void> future = currentTable.addFeatureAsync(feature);
                        future.addDoneListener(() -> {
                            try {
                                future.get();
                                Log.d(TAG, getGeometryTypeName() + "添加成功");
                            } catch (Exception e) {
                                Log.e(TAG, "添加要素失败: " + e.getMessage());
                            }
                        });
                    } else {
                        throw new IllegalStateException("点数不足以创建要素");
                    }
                    break;
            }

            // 清理绘制状态
            isEditing = false;
            points.clear();
            pointFeatures.clear();
            graphicsOverlay.getGraphics().clear();

            // 在主线程显示提示并刷新图层
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "要素添加成功", Toast.LENGTH_SHORT).show();
                if (currentLayer != null) {
                    currentLayer.clearSelection();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "完成绘制失败: " + e.getMessage());
            new Handler(Looper.getMainLooper()).post(() -> 
                Toast.makeText(context, "完成绘制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }
    }

    private int getMinimumPointsRequired() {
        switch (currentGeometryType) {
            case POINT:
                return 1;
            case POLYLINE:
                return 2;
            case POLYGON:
                return 3;
            default:
                return 0;
        }
    }

    public void cancelDrawing() {
        isEditing = false;
        if (points != null) {
            points.clear();
        }
        pointFeatures.clear();
        graphicsOverlay.getGraphics().clear();
    }

    private void createNewFeatureLayer() {
        try {
            // 创建字段列表
            List<Field> fields = new ArrayList<>();
            
            // 添加 OBJECTID 字段（必需的）
            fields.add(Field.createInteger("OBJECTID", "Object ID"));
            
            // 添加其他字段
            fields.add(Field.createString("FEAT_NAME", "名称", 50));
            fields.add(Field.createString("FEAT_DESC", "描述", 255));
            fields.add(Field.createInteger("FEAT_ID", "编号"));
            
            // 创建要素集合表，确保使用正确的空间参考
            currentTable = new FeatureCollectionTable(fields, currentGeometryType, 
                mapView.getSpatialReference());
            
            // 确保表已经初始化
            currentTable.loadAsync();
            currentTable.addDoneLoadingListener(() -> {
                try {
                    if (currentTable.getLoadStatus() == LoadStatus.LOADED) {
                        // 创建要素图层
                        FeatureLayer featureLayer = new FeatureLayer(currentTable);
                        
                        // 设置图层样式
                        switch (currentGeometryType) {
                            case POINT:
                                SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(
                                    SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 10);
                                featureLayer.setRenderer(new com.esri.arcgisruntime.symbology.SimpleRenderer(pointSymbol));
                                break;
                            
                            case POLYLINE:
                                SimpleLineSymbol lineSymbol = new SimpleLineSymbol(
                                    SimpleLineSymbol.Style.SOLID, Color.BLUE, 2);
                                featureLayer.setRenderer(new com.esri.arcgisruntime.symbology.SimpleRenderer(lineSymbol));
                                break;
                            
                            case POLYGON:
                                SimpleLineSymbol outlineSymbol = new SimpleLineSymbol(
                                    SimpleLineSymbol.Style.SOLID, Color.BLUE, 2);
                                SimpleFillSymbol fillSymbol = new SimpleFillSymbol(
                                    SimpleFillSymbol.Style.SOLID, 
                                    Color.argb(100, 0, 0, 255), 
                                    outlineSymbol);
                                featureLayer.setRenderer(new com.esri.arcgisruntime.symbology.SimpleRenderer(fillSymbol));
                                break;
                        }

                        // 生成图层名称
                        String layerName = "新建" + getGeometryTypeName() + System.currentTimeMillis();
                        featureLayer.setName(layerName);
                        
                        // 添加图层到地图
                        mapView.getMap().getOperationalLayers().add(featureLayer);

                        // 通知主活动图层已创建
                        if (layerCreationCallback != null) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                layerCreationCallback.onLayerCreated(featureLayer, layerName);
                            });
                        }

                        // 保存当前图层引用
                        currentLayer = featureLayer;

                        Log.d(TAG, "新建图层创建成功: " + layerName);
                    } else {
                        String error = currentTable.getLoadError() != null ? 
                            currentTable.getLoadError().getMessage() : "未知错误";
                        Log.e(TAG, "要素表加载失败: " + error);
                        throw new RuntimeException("要素表加载失败: " + error);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "创建图层失败: " + e.getMessage());
                    throw new RuntimeException("创建图层失败: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "创建图层失败: " + e.getMessage());
            throw new RuntimeException("创建图层失败: " + e.getMessage());
        }
    }

    private String getGeometryTypeName() {
        switch (currentGeometryType) {
            case POINT:
                return "点要素";
            case POLYLINE:
                return "线要素";
            case POLYGON:
                return "面要素";
            default:
                return "要素";
        }
    }

    public void saveAsShapefile(String fileName) {
        // 实现保存为Shapefile的逻辑
        // 这部分需要额外的实现...
        Toast.makeText(context, "保存功能待实现", Toast.LENGTH_SHORT).show();
    }

    public boolean isEditing() {
        return isEditing;
    }

    public void setLayerCreationCallback(LayerCreationCallback callback) {
        this.layerCreationCallback = callback;
    }

    // 添加 createGeometry 方法
    private com.esri.arcgisruntime.geometry.Geometry createGeometry() {
        if (points == null || points.size() < getMinimumPointsRequired()) {
            throw new IllegalStateException("点数不足以创建几何体");
        }

        switch (currentGeometryType) {
            case POINT:
                return points.get(0);
            case POLYLINE:
                return new Polyline(points);
            case POLYGON:
                return new Polygon(points);
            default:
                throw new IllegalStateException("不支持的几何类型: " + currentGeometryType);
        }
    }

    // 添加生成 ObjectID 的方法
    private static int nextObjectId = 1;
    private synchronized int generateObjectId() {
        return nextObjectId++;
    }

    // 添加生成 FeatureID 的方法
    private static int nextFeatureId = 1;
    private synchronized int generateFeatureId() {
        return nextFeatureId++;
    }
} 