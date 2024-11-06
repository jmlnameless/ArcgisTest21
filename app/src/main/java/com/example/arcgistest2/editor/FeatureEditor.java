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
            Log.d(TAG, "开始创建图层: " + layerName + ", 类型: " + geometryType);
            
            // 设置当前编辑的几何类型（在创建文件之前）
            currentGeometryType = geometryType;

            // 在后台线程创建文件
            new Thread(() -> {
                try {
                    createShapefileFiles(layerName, geometryType);
                    
                    // 在主线程加载图层
                    String path = new File(context.getFilesDir(), layerName + ".shp").getAbsolutePath();
                    layerManager.loadShapefileLayer(path, new LayerManager.LayerLoadCallback() {
                        @Override
                        public void onSuccess(FeatureLayer layer) {
                            editingLayer = layer;
                            setLayerSymbol(layer, geometryType);
                            Toast.makeText(context, "创建图层成功", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, error);
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    Log.e(TAG, "创建Shapefile文件失败: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(context, "创建Shapefile文件失败", Toast.LENGTH_SHORT).show());
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "创建要素图层失败: " + e.getMessage());
            Toast.makeText(context, "创建图层失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 创建Shapefile必要文件
    private void createShapefileFiles(String layerName, GeometryType geometryType) throws IOException {
        Log.d(TAG, "开始创建Shapefile文件: " + layerName);
        
        // 创建目录（如果不存在）
        File directory = context.getFilesDir();
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 创建.shp文件
        File shpFile = new File(directory, layerName + ".shp");
        createEmptyFile(shpFile);

        // 创建.shx文件
        File shxFile = new File(directory, layerName + ".shx");
        createEmptyFile(shxFile);

        // 创建.dbf文件并写入基本结构
        File dbfFile = new File(directory, layerName + ".dbf");
        createDBFFile(dbfFile);

        // 创建.prj文件并写入坐标系信息
        File prjFile = new File(directory, layerName + ".prj");
        createPRJFile(prjFile);

        Log.d(TAG, "所有文件创建完成");
    }

    // 创建空文件
    private void createEmptyFile(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // Shapefile文件头（100字节）
            byte[] header = new byte[100];
            
            // 文件代码 (0x0000270a)
            header[0] = (byte) 0x00;
            header[1] = (byte) 0x00;
            header[2] = (byte) 0x27;
            header[3] = (byte) 0x0a;
            
            // 未使用的20字节
            for (int i = 4; i < 24; i++) {
                header[i] = (byte) 0x00;
            }
            
            // 文件长度（初始为100字节）
            int fileLength = 100;
            header[24] = (byte) ((fileLength / 2) & 0xFF);
            header[25] = (byte) (((fileLength / 2) >> 8) & 0xFF);
            header[26] = (byte) (((fileLength / 2) >> 16) & 0xFF);
            header[27] = (byte) (((fileLength / 2) >> 24) & 0xFF);
            
            // 版本
            header[28] = (byte) 0x03;
            header[29] = (byte) 0x00;
            header[30] = (byte) 0x00;
            header[31] = (byte) 0x00;
            
            // 几何类型
            int shapeType;
            switch (currentGeometryType) {
                case POINT:
                    shapeType = 1;
                    break;
                case POLYLINE:
                    shapeType = 3;
                    break;
                case POLYGON:
                    shapeType = 5;
                    break;
                default:
                    shapeType = 0;
            }
            header[32] = (byte) (shapeType & 0xFF);
            header[33] = (byte) ((shapeType >> 8) & 0xFF);
            header[34] = (byte) ((shapeType >> 16) & 0xFF);
            header[35] = (byte) ((shapeType >> 24) & 0xFF);
            
            // 边界框（初始为0）
            double[] bbox = {0.0, 0.0, 0.0, 0.0}; // Xmin, Ymin, Xmax, Ymax
            int offset = 36;
            for (double value : bbox) {
                long bits = Double.doubleToLongBits(value);
                for (int i = 0; i < 8; i++) {
                    header[offset + i] = (byte) ((bits >> (i * 8)) & 0xFF);
                }
                offset += 8;
            }
            
            // Z范围（初始为0）
            for (int i = 68; i < 84; i++) {
                header[i] = (byte) 0x00;
            }
            
            // M范围（初始为0）
            for (int i = 84; i < 100; i++) {
                header[i] = (byte) 0x00;
            }
            
            fos.write(header);
        }
    }

    // 创建DBF文件
    private void createDBFFile(File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // DBF文件头（32字节）
            byte[] header = new byte[32];
            
            // 版本号 (0x03 for dBASE III)
            header[0] = (byte) 0x03;
            
            // 最后更新日期（年月日）
            java.util.Calendar cal = java.util.Calendar.getInstance();
            header[1] = (byte) (cal.get(java.util.Calendar.YEAR) - 1900);
            header[2] = (byte) (cal.get(java.util.Calendar.MONTH) + 1);
            header[3] = (byte) cal.get(java.util.Calendar.DAY_OF_MONTH);
            
            // 记录数（初始为0）
            header[4] = (byte) 0x00;
            header[5] = (byte) 0x00;
            header[6] = (byte) 0x00;
            header[7] = (byte) 0x00;
            
            // 头部长��（包括字段描述符）
            short headerLength = (short) (32 + 32 + 1);  // 头部 + 一个字段描述符 + 终止符
            header[8] = (byte) (headerLength & 0xFF);
            header[9] = (byte) ((headerLength >> 8) & 0xFF);
            
            // 记录长度（每个字段的长度总和 + 1）
            header[10] = (byte) 11;  // ID字段长度为10 + 删除标记1字节
            header[11] = (byte) 0x00;
            
            // 保留字节
            for (int i = 12; i < 32; i++) {
                header[i] = (byte) 0x00;
            }
            
            fos.write(header);
            
            // 写入字段描述符
            byte[] fieldDescriptor = new byte[32];
            // ID字段
            fieldDescriptor[0] = (byte) 'I';
            fieldDescriptor[1] = (byte) 'D';
            fieldDescriptor[2] = (byte) 0x00;
            fieldDescriptor[3] = (byte) 'N';  // 数值类型
            fieldDescriptor[4] = (byte) 0x00;  // 字段长度
            fieldDescriptor[5] = (byte) 0x00;  // 字段位置
            fieldDescriptor[6] = (byte) 0x00;  // 字段长度
            fieldDescriptor[7] = (byte) 0x00;  // 字段小数位数
            fieldDescriptor[16] = (byte) 10;   // 字段长度
            fieldDescriptor[17] = (byte) 0;    // 小数位数
            
            fos.write(fieldDescriptor);
            
            // 写入终止符
            fos.write((byte) 0x0D);
            
            // 写入文件结束标记
            fos.write((byte) 0x1A);
        }
    }

    // 创建PRJ文件
    private void createPRJFile(File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // WGS84坐标系的WKT字符串
            String wkt = "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]]," +
                        "PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]]";
            fos.write(wkt.getBytes());
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
            Graphic graphic = new Graphic();
            graphic.setGeometry(geometry);
            graphic.setSymbol(symbol);
            tempGraphicsOverlay.getGraphics().add(graphic);
        }

        // 添加顶点标记
        SimpleMarkerSymbol vertexSymbol = new SimpleMarkerSymbol(
            SimpleMarkerSymbol.Style.CIRCLE, 
            Color.RED, 
            5);
        for (Point point : tempPoints) {
            Graphic vertexGraphic = new Graphic();
            vertexGraphic.setGeometry(point);
            vertexGraphic.setSymbol(vertexSymbol);
            tempGraphicsOverlay.getGraphics().add(vertexGraphic);
        }
    }

    // 完成绘制
    public void completeDrawing() {
        if (!isEditing || tempPoints.isEmpty() || editingLayer == null) return;
        
        try {
            Geometry geometry = null;
            switch (currentGeometryType) {
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
                Toast.makeText(context, "要素创建成功", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "创建要素失败: " + e.getMessage());
            Toast.makeText(context, "创建要素失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            tempPoints.clear();
            tempGraphicsOverlay.getGraphics().clear();
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
        attributes.put("ID", System.currentTimeMillis()); // 添加一个唯一标识符
        
        Feature feature = editingLayer.getFeatureTable().createFeature(attributes, geometry);
        
        try {
            editingLayer.getFeatureTable().addFeatureAsync(feature).get();
            editedFeatures.add(feature);
            Log.d(TAG, "要素添加成功");
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "添加要素失败: " + e.getMessage());
            Toast.makeText(context, "添加要素失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 