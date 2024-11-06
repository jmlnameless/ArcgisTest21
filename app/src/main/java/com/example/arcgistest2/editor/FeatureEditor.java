package com.example.arcgistest2.editor;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.esri.arcgisruntime.symbology.Symbol;
import com.example.arcgistest2.layer.LayerManager;
import com.example.arcgistest2.utils.ShapefileCreator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FeatureEditor {
    private static final String TAG = "FeatureEditor";
    private final MapView mapView;
    private final Context context;
    private FeatureLayer editingLayer;
    private GeometryType currentGeometryType;
    private List<Point> tempPoints;
    private boolean isEditing;
    private GraphicsOverlay tempGraphicsOverlay;
    private final List<Feature> editedFeatures = new ArrayList<>();
    private final LayerManager layerManager;

    public FeatureEditor(MapView mapView, Context context) {
        this.mapView = mapView;
        this.context = context;
        this.tempPoints = new ArrayList<>();
        this.isEditing = false;
        this.layerManager = new LayerManager(context, mapView, mapView.getMap());
        setupTempGraphicsOverlay();
    }

    private void setupTempGraphicsOverlay() {
        tempGraphicsOverlay = new GraphicsOverlay();
        mapView.getGraphicsOverlays().add(tempGraphicsOverlay);
    }

    // 创建新的要素图层
    public void createNewFeatureLayer(GeometryType geometryType, String layerName) {
        try {
            // 使用ShapefileCreator创建Shapefile
            boolean created = ShapefileCreator.createShapefile(context, layerName, geometryType);
            
            if (!created) {
                throw new Exception("创建Shapefile失败");
            }

            // 获取创建的Shapefile路径
            File shapefileDir = new File(context.getFilesDir(), "shapefiles");
            String shapefilePath = new File(shapefileDir, layerName + ".shp").getAbsolutePath();

            // 加载新创建的图层
            ShapefileFeatureTable shapefileFeatureTable = new ShapefileFeatureTable(shapefilePath);
            shapefileFeatureTable.loadAsync();
            
            shapefileFeatureTable.addDoneLoadingListener(() -> {
                if (shapefileFeatureTable.getLoadStatus() == LoadStatus.LOADED) {
                    FeatureLayer layer = new FeatureLayer(shapefileFeatureTable);
                    editingLayer = layer;
                    currentGeometryType = geometryType;
                    
                    // 设置图层符号
                    setLayerSymbol(layer, geometryType);
                    
                    // 添加到地图
                    mapView.getMap().getOperationalLayers().add(layer);
                    
                    // 通知用户
                    Toast.makeText(context, "创建图层成功", Toast.LENGTH_SHORT).show();
                } else {
                    String error = shapefileFeatureTable.getLoadError().getMessage();
                    Log.e(TAG, "加载图层失败: " + error);
                    Toast.makeText(context, "加载图层失败: " + error, Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "创建图层失败: " + e.getMessage());
            Toast.makeText(context, "创建图层失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 添加辅助方法
    private void runOnUiThread(Runnable runnable) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(runnable);
        }
    }

    // 添加点
    public void addPoint(Point point) {
        if (!isEditing || editingLayer == null) return;
        
        tempPoints.add(point);
        updateTempGraphics();
        
        switch (currentGeometryType) {
            case POINT:
                createFeature(point);
                tempPoints.clear();
                break;
                
            case POLYLINE:
            case POLYGON:
                // 临时图形已在updateTempGraphics中更新
                break;
        }
    }

    // 更新临时图形
    private void updateTempGraphics() {
        tempGraphicsOverlay.getGraphics().clear();
        
        if (tempPoints.isEmpty()) return;

        Geometry geometry = null;
        Symbol symbol = null;
        
        switch (currentGeometryType) {
            case POINT:
                geometry = tempPoints.get(tempPoints.size() - 1);
                symbol = new SimpleMarkerSymbol(
                    SimpleMarkerSymbol.Style.CIRCLE, 
                    Color.argb(150, 255, 0, 0), 
                    10);
                break;
            case POLYLINE:
                if (tempPoints.size() >= 2) {
                    geometry = createPolyline();
                    symbol = new SimpleLineSymbol(
                        SimpleLineSymbol.Style.DASH, 
                        Color.argb(150, 0, 0, 255), 
                        2);
                }
                break;
            case POLYGON:
                if (tempPoints.size() >= 3) {
                    geometry = createPolygon();
                    SimpleLineSymbol outline = new SimpleLineSymbol(
                        SimpleLineSymbol.Style.DASH, 
                        Color.argb(150, 0, 0, 0), 
                        2);
                    symbol = new SimpleFillSymbol(
                        SimpleFillSymbol.Style.SOLID, 
                        Color.argb(50, 0, 255, 0), 
                        outline);
                }
                break;
        }

        if (geometry != null && symbol != null) {
            Graphic graphic = new Graphic(geometry, symbol);
            tempGraphicsOverlay.getGraphics().add(graphic);
        }

        // 添加顶点标记
        SimpleMarkerSymbol vertexSymbol = new SimpleMarkerSymbol(
            SimpleMarkerSymbol.Style.CIRCLE, 
            Color.RED, 
            5);
        for (Point point : tempPoints) {
            Graphic vertexGraphic = new Graphic(point, vertexSymbol);
            tempGraphicsOverlay.getGraphics().add(vertexGraphic);
        }
    }

    // 完成绘制
    public void completeDrawing() {
        if (!isEditing || tempPoints.isEmpty() || editingLayer == null) return;
        
        try {
            Geometry geometry = null;
            switch (currentGeometryType) {
                case POINT:
                    geometry = tempPoints.get(0);
                    break;
                case POLYLINE:
                    if (tempPoints.size() >= 2) {
                        geometry = createPolyline();
                    }
                    break;
                case POLYGON:
                    if (tempPoints.size() >= 3) {
                        geometry = createPolygon();
                    }
                    break;
            }
            
            if (geometry != null) {
                createFeature(geometry);
            } else {
                Toast.makeText(context, "无效的几何形状", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "创建要素失败: " + e.getMessage());
            Toast.makeText(context, "创建要素失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            stopEditing();
        }
    }

    // 保存为Shapefile
    public void saveAsShapefile(String fileName) {
        if (editingLayer == null || editingLayer.getFeatureTable() == null) {
            Toast.makeText(context, "没有可保存的图层", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            String basePath = new File(context.getFilesDir(), fileName).getAbsolutePath();
            ShapefileFeatureTable sourceTable = (ShapefileFeatureTable) editingLayer.getFeatureTable();
            
            // 复制所有相关文件
            String[] extensions = {".shp", ".shx", ".dbf", ".prj"};
            String sourcePath = sourceTable.getPath();
            if (sourcePath != null) {
                String sourceBasePath = sourcePath.substring(0, sourcePath.length() - 4);
                for (String ext : extensions) {
                    File sourceFile = new File(sourceBasePath + ext);
                    if (sourceFile.exists()) {
                        File targetFile = new File(basePath + ext);
                        Files.copy(
                            sourceFile.toPath(),
                            targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        );
                    }
                }
                Toast.makeText(context, "保存成功: " + fileName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "保存失败: 无法获取源文件路径", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "保存Shapefile失败: " + e.getMessage());
            Toast.makeText(context, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Getter方法
    public boolean isEditing() {
        return isEditing;
    }

    public GeometryType getCurrentGeometryType() {
        return currentGeometryType;
    }

    public FeatureLayer getEditingLayer() {
        return editingLayer;
    }

    // 设置图层符号
    private void setLayerSymbol(FeatureLayer layer, GeometryType geometryType) {
        switch (geometryType) {
            case POINT:
                SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(
                    SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 10);
                layer.setRenderer(new SimpleRenderer(pointSymbol));
                break;
                
            case POLYLINE:
                SimpleLineSymbol lineSymbol = new SimpleLineSymbol(
                    SimpleLineSymbol.Style.SOLID, Color.BLUE, 2);
                layer.setRenderer(new SimpleRenderer(lineSymbol));
                break;
                
            case POLYGON:
                SimpleLineSymbol outlineSymbol = new SimpleLineSymbol(
                    SimpleLineSymbol.Style.SOLID, Color.BLACK, 2);
                SimpleFillSymbol polygonSymbol = new SimpleFillSymbol(
                    SimpleFillSymbol.Style.SOLID, Color.argb(100, 0, 255, 0), outlineSymbol);
                layer.setRenderer(new SimpleRenderer(polygonSymbol));
                break;
        }
    }

    // 开始编辑
    public void startEditing(GeometryType geometryType) {
        currentGeometryType = geometryType;
        isEditing = true;
        tempPoints.clear();
        tempGraphicsOverlay.getGraphics().clear();
        Log.d(TAG, "开始编辑: " + geometryType.name());
    }

    // 停止编辑
    public void stopEditing() {
        isEditing = false;
        tempPoints.clear();
        tempGraphicsOverlay.getGraphics().clear();
        Log.d(TAG, "停止编辑");
    }

    // 创建线要素
    private Polyline createPolyline() {
        PointCollection points = new PointCollection(mapView.getSpatialReference());
        points.addAll(tempPoints);
        return new Polyline(points);
    }

    // 创建面要素
    private Polygon createPolygon() {
        PointCollection points = new PointCollection(mapView.getSpatialReference());
        points.addAll(tempPoints);
        // 确保多边形闭合
        if (!tempPoints.get(0).equals(tempPoints.get(tempPoints.size() - 1))) {
            points.add(tempPoints.get(0));
        }
        return new Polygon(points);
    }

    // 创建要素
    private void createFeature(Geometry geometry) {
        if (editingLayer == null || editingLayer.getFeatureTable() == null) return;
        
        Map<String, Object> attributes = new HashMap<>();
        // 添加必要的属性
        attributes.put("ID", System.currentTimeMillis()); 
        attributes.put("NAME", "要素_" + System.currentTimeMillis());
        
        Feature feature = editingLayer.getFeatureTable().createFeature(attributes, geometry);
        
        try {
            editingLayer.getFeatureTable().addFeatureAsync(feature).get();
            editedFeatures.add(feature);
            Log.d(TAG, "要素添加成功");
            Toast.makeText(context, "要素添加成功", Toast.LENGTH_SHORT).show();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "添加要素失败: " + e.getMessage());
            Toast.makeText(context, "添加要素失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 