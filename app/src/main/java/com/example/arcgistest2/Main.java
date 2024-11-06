package com.example.arcgistest2;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.BackgroundGrid;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.example.arcgistest2.editor.FeatureEditor;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import android.animation.ObjectAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.esri.arcgisruntime.geometry.Geometry;

import com.example.arcgistest2.layer.LayerManager;

import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;

import android.provider.MediaStore;

import androidx.documentfile.provider.DocumentFile;

public class Main extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private MapView mMapView; // 地图视图组件
    private FeatureLayer mFeatureLayer; // 要素图层
    private ListView layerListView;//图层列表
    private Button layerListButton;
    private Button tuli;
    // 需要检查的文件列表(实际只需要.shx .shp .dbf .prj)
    private final List<String> filesToCheck = Arrays.asList(
            "测试数据.shp",
            "测试数据.xml",
            "测试数据.shx",
            "测试数据.dbf",
            "测试数据.prj",
            "测试数据.shp",
            "测试数据.cpg",
            "测试数据.sbn",
            "测试数据.sbx",
            "sheng2022.shp",
            "sheng2022.prj",
            "sheng2022.shx",
            "sheng2022.dbf",
            "sheng2022.xml",
            "sheng2022.cpg",
            "sheng2022.sbn",
            "sheng2022.sbx",
            "cs2022.shp",
            "cs2022.prj",
            "cs2022.shx",
            "cs2022.dbf",
            "cs2022.xml",
            "cs2022.cpg",
            "cs2022.sbn",
            "cs2022.sbx"
    );

    private DrawerLayout mDrawerLayout;
    private NavigationView navigationView;
    private TextInputEditText searchEditText;

    // 添加图层管理相关变量
    private ArcGISMap map;
    private List<FeatureLayer> featureLayers = new ArrayList<>();
    private ArrayAdapter<String> layerListAdapter;
    private List<String> layerNames = new ArrayList<>();

    // 定义图层配置
    private static class LayerConfig {
        String fileName;
        String displayName;
        int lineColor;
        float lineWidth;
        int fillColor;

        LayerConfig(String fileName, String displayName, int lineColor, float lineWidth, int fillColor) {
            this.fileName = fileName;
            this.displayName = displayName;
            this.lineColor = lineColor;
            this.lineWidth = lineWidth;
            this.fillColor = fillColor;
        }
    }

    // 预定义图层配置
    private final List<LayerConfig> defaultLayers = Arrays.asList(
        new LayerConfig("sheng2022", "省界2022", Color.BLUE, 2.0f, Color.argb(0, 0, 0, 0)),
        new LayerConfig("cs2022", "城市2022", Color.RED, 1.5f, Color.argb(30, 255, 0, 0)),
        new LayerConfig("测试数据", "测试数据", Color.GREEN, 1.0f, Color.argb(50, 0, 255, 0))
    );

    // 在类的开头添加
    private FeatureEditor featureEditor;
    private boolean isDrawing = false;

    // 在 Main 类中添加 LayerManager
    private LayerManager layerManager;

    // 在类的开头添加常量
    private static final int PICK_SHAPEFILE_REQUEST = 1001;

    // 在类的开头添加权限请求码
    private static final int PERMISSION_REQUEST_CODE = 1000;

    // onCreate 方法，当活动创建时调用
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.index);

        // 初始化地图视图
        mMapView = findViewById(R.id.mapView);
        
        // 初始化地图
        map = new ArcGISMap();
        mMapView.setMap(map);

        // 初始化搜索框
        searchEditText = findViewById(R.id.editTextText);

        // 初始化图层列表适配器
        layerListAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_list_item_multiple_choice, 
            layerNames);
        ListView layerListView = findViewById(R.id.layerListView);
        layerListView.setAdapter(layerListAdapter);
        layerListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // 初始化抽屉布局
        mDrawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.left_drawer);

        // 设置地图控件
        setupMapControls();

        // 设置搜索栏
        setupSearchBar();

        // 设置导航抽屉
        setupNavigationDrawer();

        // 设置背景网格
        setBackgroundGrid();

        // 检查权限和复制文件
        handlePermission();
        copyShapefilesToInternalStorage(this);

        // 初始化图层管理器
        layerManager = new LayerManager(this, mMapView, map);

        // 初始化要素编辑器
        featureEditor = new FeatureEditor(mMapView, this);

        // 加载初始图层
        loadInitialLayers();
    }

    private void setupMapControls() {
        MaterialButton magnifyButton = findViewById(R.id.magnify);
        MaterialButton shrinkButton = findViewById(R.id.shrink);

        magnifyButton.setOnClickListener(v -> {
            animateButton(v);
            double scale = mMapView.getMapScale();
            mMapView.setViewpointScaleAsync(scale * 0.5);
        });

        shrinkButton.setOnClickListener(v -> {
            animateButton(v);
            double scale = mMapView.getMapScale();
            mMapView.setViewpointScaleAsync(scale * 2.0);
        });
    }

    private void setupSearchBar() {
        MaterialButton loadLayerButton = findViewById(R.id.Load_layer);
        loadLayerButton.setOnClickListener(v -> {
            animateButton(v);
            openFilePicker();
        });
    }

    private void setupNavigationDrawer() {
        // 设置每个功能区的展开/折叠
        MaterialButton action1 = findViewById(R.id.action1);
        MaterialButton action2 = findViewById(R.id.action2);
        MaterialButton action3 = findViewById(R.id.action3);
        MaterialButton action4 = findViewById(R.id.action4);

        View expandableLayout1 = findViewById(R.id.expandable_layout_1);
        View expandableLayout2 = findViewById(R.id.expandable_layout_2);
        View expandableLayout3 = findViewById(R.id.expandable_layout_3);
        View expandableLayout4 = findViewById(R.id.expandable_layout_4);

        // 设置点击监听器
        action1.setOnClickListener(v -> {
            animateButton(v);
            toggleExpandableLayout(expandableLayout1);
        });

        action2.setOnClickListener(v -> {
            animateButton(v);
            toggleExpandableLayout(expandableLayout2);
        });

        action3.setOnClickListener(v -> {
            animateButton(v);
            toggleExpandableLayout(expandableLayout3);
        });

        action4.setOnClickListener(v -> {
            animateButton(v);
            toggleExpandableLayout(expandableLayout4);
        });

        // 设置图层管理功能
        setupLayerManagement();

        // 设置属性查询功能
        setupAttributeQuery();

        // 设置要素编辑功能
        setupFeatureEdit();

        // 设置GPS数据功能
        setupGPSData();
    }

    private void toggleExpandableLayout(View layout) {
        if (layout.getVisibility() == View.VISIBLE) {
            collapseView(layout);
        } else {
            expandView(layout);
        }
    }

    private void setupLayerManagement() {
        MaterialButton layerListButton = findViewById(R.id.tucengliebiao);
        MaterialButton legendButton = findViewById(R.id.tuli);
        View layerListContainer = findViewById(R.id.layer_list_container);
        View legendContainer = findViewById(R.id.legend_container);

        layerListButton.setOnClickListener(v -> {
            Log.d(TAG, "图层列表按钮被点击");
            animateButton(v);
            if (layerListContainer.getVisibility() == View.VISIBLE) {
                collapseView(layerListContainer);
            } else {
                updateLayerList();
                collapseView(legendContainer);
                expandView(layerListContainer);
            }
        });

        legendButton.setOnClickListener(v -> {
            Log.d(TAG, "图例按钮被点击");
            animateButton(v);
            if (legendContainer.getVisibility() == View.VISIBLE) {
                collapseView(legendContainer);
            } else {
                collapseView(layerListContainer);
                expandView(legendContainer);
            }
        });

        // 修改图层列表点击事件
        ListView layerListView = findViewById(R.id.layerListView);
        layerListView.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(TAG, "图层列表项被点击: " + position);
            FeatureLayer layer = featureLayers.get(position);
            boolean isChecked = layerListView.isItemChecked(position);
            layer.setVisible(isChecked);
            
            // 如果图层被选中，立即缩放到该图层
            if (isChecked) {
                zoomToLayerExtent(layer);
            }
        });
    }

    private void setupAttributeQuery() {
        MaterialButton mapQueryButton = findViewById(R.id.tuchashuxing);
        MaterialButton attributeQueryButton = findViewById(R.id.shuxingchaxun);

        mapQueryButton.setOnClickListener(v -> {
            animateButton(v);
            // 实现图查属性功能
        });

        attributeQueryButton.setOnClickListener(v -> {
            animateButton(v);
            // 实属性查询功能
        });
    }

    private void setupFeatureEdit() {
        MaterialButton templateButton = findViewById(R.id.yaosumoban);
        MaterialButton editToolButton = findViewById(R.id.yaosubianjigongju);

        // 创建要素模板菜单
        PopupMenu templateMenu = new PopupMenu(this, templateButton);
        templateMenu.getMenu().add("点要素");
        templateMenu.getMenu().add("线要素");
        templateMenu.getMenu().add("面要素");

        templateButton.setOnClickListener(v -> {
            animateButton(v);
            templateMenu.show();
        });

        templateMenu.setOnMenuItemClickListener(item -> {
            GeometryType geometryType;
            switch (item.getTitle().toString()) {
                case "点要素":
                    geometryType = GeometryType.POINT;
                    break;
                case "线要素":
                    geometryType = GeometryType.POLYLINE;
                    break;
                case "面要素":
                    geometryType = GeometryType.POLYGON;
                    break;
                default:
                    return false;
            }
            
            // 创建新的要素图层
            String layerName = "新建" + item.getTitle() + System.currentTimeMillis();
            featureEditor.createNewFeatureLayer(geometryType, layerName);
            return true;
        });

        // 创建编辑工具菜单
        PopupMenu editMenu = new PopupMenu(this, editToolButton);
        editMenu.getMenu().add("绘制");
        editMenu.getMenu().add("完成");
        editMenu.getMenu().add("取消");
        editMenu.getMenu().add("保存");

        editToolButton.setOnClickListener(v -> {
            animateButton(v);
            editMenu.show();
        });

        editMenu.setOnMenuItemClickListener(item -> {
            switch (item.getTitle().toString()) {
                case "绘制":
                    startDrawing();
                    break;
                case "完成":
                    completeDrawing();
                    break;
                case "取消":
                    cancelDrawing();
                    break;
                case "保存":
                    saveFeatures();
                    break;
            }
            return true;
        });

        // 设置地图点击事件
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (isDrawing) {
                    android.graphics.Point screenPoint = new android.graphics.Point(
                        Math.round(e.getX()),
                        Math.round(e.getY()));
                    Point mapPoint = mMapView.screenToLocation(screenPoint);
                    featureEditor.addPoint(mapPoint);
                    return true;
                }
                return super.onSingleTapConfirmed(e);
            }
        });
    }

    private void setupGPSData() {
        MaterialButton gps1Button = findViewById(R.id.GPS_1);
        MaterialButton gps2Button = findViewById(R.id.GPS_2);

        gps1Button.setOnClickListener(v -> {
            animateButton(v);
            // 实现GPS_1功能
        });

        gps2Button.setOnClickListener(v -> {
            animateButton(v);
            // 实现GPS_2功能
        });
    }

    // 添加按钮点击动画效果
    private void animateButton(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.9f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f, 1f);
        scaleX.setDuration(200);
        scaleY.setDuration(200);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.start();
        scaleY.start();
    }

    // 展视图的动画
    private void expandView(View view) {
        view.setVisibility(View.VISIBLE);
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    // 折叠视图的动画
    private void collapseView(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
            }
        });
        animator.start();
    }

    private void handleLayerList() {
        // 实现图层列表的显逻
        ListView layerListView = findViewById(R.id.layerListView);
        if (layerListView.getVisibility() == View.VISIBLE) {
            layerListView.setVisibility(View.GONE);
        } else {
            layerListView.setVisibility(View.VISIBLE);
        }
    }

    private void handleLegend() {
        // 实现图例的显示逻辑
        TableLayout tulimoban = findViewById(R.id.tulimoban);
        if (tulimoban.getVisibility() == View.VISIBLE) {
            tulimoban.setVisibility(View.GONE);
        } else {
            tulimoban.setVisibility(View.VISIBLE);
        }
    }

    private void loadLayer() {
        String searchText = searchEditText.getText().toString();
        if (!searchText.isEmpty()) {
            // 创建新的 LayerConfig 对象
            LayerConfig config = new LayerConfig(
                searchText,  // fileName
                searchText,  // displayName
                Color.GRAY,  // 默认线条颜色
                1.0f,       // 默认线宽
                Color.argb(50, 128, 128, 128)  // 默认填充颜色
            );
            String shapefilePath = getInternalStorageFilePath(this, searchText + ".shp");
            loadShapefileLayer(shapefilePath, config);
        }
    }

    @Override
    protected void onPause() {
        if (mMapView != null) {
            mMapView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mMapView != null) {
            mMapView.dispose();
        }
        if (layerManager != null) {
            layerManager.dispose();
        }
        super.onDestroy();
    }

    // onActivityResult 方法，处理从另一个活动返回的结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_SHAPEFILE_REQUEST && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                importShapefile(data.getData());
            }
        }
    }

    // 高亮显示符合条件的要素
    private void highlightFeatures(String fieldName, String fieldValue) {
        // 确保图层已加载
        if (mFeatureLayer != null && mFeatureLayer.getFeatureTable() != null) {
            // 清除之前的选择
            mFeatureLayer.clearSelection();

            // 设置查询条件
            String queryExpression = fieldName + " = '" + fieldValue + "'";
            QueryParameters queryParameters = new QueryParameters();
            queryParameters.setWhereClause(queryExpression);

            // 设置选择模式为新选择
            FeatureLayer.SelectionMode selectionMode = FeatureLayer.SelectionMode.NEW;

            // 发起异步查询和选择操作
            final ListenableFuture<FeatureQueryResult> future = mFeatureLayer.selectFeaturesAsync(queryParameters, selectionMode);
            future.addDoneListener(() -> {
                try {
                    // 获取查询结果
                    FeatureQueryResult result = future.get();

                    // 检查查询结果是否为空
                    if (result == null || !result.iterator().hasNext()) {
                        // 如果没有查询结果，显示提示对话框
                        showAlertDialog("未找到要素");
                    } else {
                        // 处理询结果并高亮显示要素
                        highlightFeaturesFromResult(result);
                    }

                } catch (InterruptedException | ExecutionException e) {
                    // 显示异常信息对话框
                    showAlertDialog("查询要素失败: " + e.getMessage());
                    Log.e("selected", "Select feature failed: " + e.getMessage());
                }
            });
        }
    }

    // 显示提示对话框
    private void showAlertDialog(String message) {
        // 在 UI 线程中执行
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");
            String pattern = "com.esri.arcgisruntime.ArcGISRuntimeException:";
            String pattern_error = "SQLite data type mismatch.: SQLite: 20\n" + "Data type mismatch";
            String message1 = message.replace(pattern, "");
            message1 = message1.replace(pattern_error, "数据类型不匹配");
            Log.d("showAlertDialog", "showAlertDialog: " + message1);
            builder.setMessage(message1);
            builder.setPositiveButton("确定", null);
            try {
                builder.show();
            } catch (WindowManager.BadTokenException e) {
                // 处理异常
                Log.e("AlertDialogError", "Unable to show AlertDialog", e);
            }
        });
    }

    // 处理查询结果并高亮显示要素
    private void highlightFeaturesFromResult(FeatureQueryResult result) {
        // 创建一个列表来存储查询到的要素
        List<Feature> features = new ArrayList<>();

        // 遍历查询结果，并将要素添加到列表中
        for (Feature feature : result) {
            features.add(feature);
        }

        // 在地图上选择和高亮显示这些要素
        if (!features.isEmpty()) {
//            mFeatureLayer.setSelectionColor(R.color.Green_200);
            mFeatureLayer.setSelectionWidth(1);
            mFeatureLayer.selectFeatures(features);
        }

        // 获取要素的范围，并调整地图视角以显示这些要素
        Envelope extent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            extent = GeometryEngine.combineExtents(features.stream()
                    .map(Feature::getGeometry)
                    .filter(Objects::nonNull) // 过滤掉可能的null值
                    .collect(Collectors.toList()));
        }
        if (extent != null) {
            mMapView.setViewpointGeometryAsync(extent, 100);
        }
    }

    // 处理权限请求
    private void handlePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上版本
            String[] permissions = {
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            };
            requestPermissions(permissions);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0到12的版本
            String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            requestPermissions(permissions);
        }
    }

    // 添加请求权限的方法
    private void requestPermissions(String[] permissions) {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsToRequest.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        }
    }

    // 添加权限请求结果处理
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                // 权限都已授予，继续初始化
                copyShapefilesToInternalStorage(this);
                loadInitialLayers();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "需要存储权限才能正常使用应用", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    // 修改复制文件的方法
    public boolean copyAssetToInternalStorage(Context context, String assetName, String destFileName) {
        if (destFileName == null) {
            destFileName = assetName;
        }

        File fileDir = context.getFilesDir();
        File destFile = new File(fileDir, destFileName);

        // 如果文件已存在，先删除
        if (destFile.exists()) {
            destFile.delete();
        }

        try (InputStream in = context.getAssets().open(assetName);
             FileOutputStream fos = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
            Log.d(TAG, "成功复制文件: " + assetName + " 到 " + destFile.getAbsolutePath());
            Log.d(TAG, "文件大小: " + destFile.length() + " bytes");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "复制文件失败: " + assetName + ", 错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 获取内部存储文件路径
    public String getInternalStorageFilePath(Context context, String fileName) {
        File fileDir = context.getFilesDir();
        File file = new File(fileDir, fileName);
        return file.getAbsolutePath();
    }

    // 检查文件是否为空
    public void checkNullFile(String filename) {
        try (RandomAccessFile reader = new RandomAccessFile(getInternalStorageFilePath(this, filename), "r")) {
            long fileLength = reader.length();
            if (fileLength > 0) {
                Log.d(TAG, filename + ":文件大小: " + fileLength + " 字节");
                byte[] bytes = new byte[10];
                reader.read(bytes);
            } else {
                Log.d(TAG, filename + "文件为空");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 设置 Shapefile ��征表
    private void setupShapefileFeatureTable(String shapefilePath) {
        ShapefileFeatureTable shapefileFeatureTable = new ShapefileFeatureTable(shapefilePath);
        shapefileFeatureTable.loadAsync();
        mFeatureLayer = new FeatureLayer(shapefileFeatureTable);
        ArcGISMap map = new ArcGISMap();

        // 设置符号和渲染器
        SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.RED, 1.0f);
        SimpleFillSymbol fillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.GREEN, lineSymbol);
        SimpleRenderer renderer = new SimpleRenderer(fillSymbol);
        mFeatureLayer.setRenderer(renderer);

        map.getOperationalLayers().add(mFeatureLayer);
        mMapView.setMap(map);

    }

    // 设置地图背景
    private void setBackgroundGrid() {
        BackgroundGrid mainBackgroundGrid = new BackgroundGrid();
        mainBackgroundGrid.setColor(Color.WHITE);
        mainBackgroundGrid.setGridLineColor(Color.WHITE);
        mainBackgroundGrid.setGridLineWidth(0);
        mMapView.setBackgroundGrid(mainBackgroundGrid);
    }

    // 复制 Shapefile 文件到内部存储
    private void copyShapefilesToInternalStorage(Context context) {
        for (String file : filesToCheck) {
            boolean copied = copyAssetToInternalStorage(context, file, file);
            Log.d(TAG, file + "文件复制完毕");
            if (!copied) {
                Log.e(TAG, "Failed to copy: " + file);
            }
        }
    }

    // 加载初始图层
    private void loadInitialLayers() {
        for (LayerConfig config : defaultLayers) {
            String shapefilePath = getInternalStorageFilePath(this, config.fileName + ".shp");
            layerManager.loadShapefileLayer(shapefilePath, new LayerManager.LayerLoadCallback() {
                @Override
                public void onSuccess(FeatureLayer layer) {
                    featureLayers.add(layer);
                    layerNames.add(config.displayName);
                    layerListAdapter.notifyDataSetChanged();
                    
                    // 默认选中图层
                    ListView layerListView = findViewById(R.id.layerListView);
                    layerListView.setItemChecked(layerNames.size() - 1, true);
                    
                    // 如果是第一个图层，缩放到其范围
                    if (featureLayers.size() == 1) {
                        layerManager.zoomToLayer(layer);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, error);
                    Toast.makeText(Main.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // 添加检查必要文件的方法
    private boolean checkRequiredFiles(String basePath) {
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

    // 加载 Shapefile 图层的方法
    private void loadShapefileLayer(String shapefilePath, LayerConfig config) {
        try {
            Log.d(TAG, "开始加载图层: " + config.displayName + ", 路径: " + shapefilePath);
            
            ShapefileFeatureTable shapefileFeatureTable = new ShapefileFeatureTable(shapefilePath);
            
            shapefileFeatureTable.addDoneLoadingListener(() -> {
                if (shapefileFeatureTable.getLoadStatus() == LoadStatus.LOADED) {
                    Log.d(TAG, "表加载成功: " + config.displayName);
                    FeatureLayer featureLayer = new FeatureLayer(shapefileFeatureTable);
                    
                    // 应用图层样式
                    SimpleLineSymbol lineSymbol = new SimpleLineSymbol(
                        SimpleLineSymbol.Style.SOLID, 
                        config.lineColor, 
                        config.lineWidth
                    );
                    SimpleFillSymbol fillSymbol = new SimpleFillSymbol(
                        SimpleFillSymbol.Style.SOLID, 
                        config.fillColor, 
                        lineSymbol
                    );
                    SimpleRenderer renderer = new SimpleRenderer(fillSymbol);
                    featureLayer.setRenderer(renderer);

                    featureLayer.addDoneLoadingListener(() -> {
                        if (featureLayer.getLoadStatus() == LoadStatus.LOADED) {
                            Log.d(TAG, "图层加载成功: " + config.displayName);
                            runOnUiThread(() -> {
                                map.getOperationalLayers().add(featureLayer);
                                featureLayers.add(featureLayer);
                                layerNames.add(config.displayName);
                                layerListAdapter.notifyDataSetChanged();

                                // 默认选中图
                                ListView layerListView = findViewById(R.id.layerListView);
                                layerListView.setItemChecked(layerNames.size() - 1, true);

                                // 如果是第一个加载的图层，缩放到其范围
                                if (featureLayers.size() == 1) {
                                    zoomToLayerExtent(featureLayer);
                                }
                            });
                        } else {
                            handleLoadError(config.displayName, "图层加载失败");
                        }
                    });

                    featureLayer.loadAsync();
                } else {
                    handleLoadError(config.displayName, "表加载失败");
                }
            });

            shapefileFeatureTable.loadAsync();
        } catch (Exception e) {
            handleLoadError(config.displayName, "加载异常: " + e.getMessage());
        }
    }

    // 修改缩放到图层范围的方法
    private void zoomToLayerExtent(FeatureLayer layer) {
        if (layer != null && layer.getFullExtent() != null) {
            try {
                // 获取图层范围并添加缓冲区
                Envelope extent = layer.getFullExtent();
                Envelope bufferedExtent = new Envelope(
                    extent.getXMin() - extent.getWidth() * 0.1,
                    extent.getYMin() - extent.getHeight() * 0.1,
                    extent.getXMax() + extent.getWidth() * 0.1,
                    extent.getYMax() + extent.getHeight() * 0.1,
                    extent.getSpatialReference()
                );
                
                // 使用动画效果缩放到图层范围
                mMapView.setViewpointGeometryAsync(bufferedExtent, 1.0)
                    .addDoneListener(() -> {
                        String layerName = layerNames.get(featureLayers.indexOf(layer));
                        Log.d(TAG, "已缩放至图层: " + layerName);
                    });
            } catch (Exception e) {
                Log.e(TAG, "缩放到图层失败: " + e.getMessage());
            }
        }
    }

    // 更新图层列表
    private void updateLayerList() {
        ListView layerListView = findViewById(R.id.layerListView);
        // 更新选中状态
        for (int i = 0; i < featureLayers.size(); i++) {
            FeatureLayer layer = featureLayers.get(i);
            layerListView.setItemChecked(i, layer.isVisible());
        }
    }

    // 获取可见图层数量
    private int getVisibleLayerCount() {
        int count = 0;
        for (FeatureLayer layer : featureLayers) {
            if (layer.isVisible()) count++;
        }
        return count;
    }

    // 缩放到所有可见图层的范围
    private void zoomToVisibleLayersExtent() {
        List<Geometry> geometries = new ArrayList<>();
        for (FeatureLayer layer : featureLayers) {
            if (layer.isVisible() && layer.getFullExtent() != null) {
                geometries.add(layer.getFullExtent());
            }
        }

        if (!geometries.isEmpty()) {
            try {
                Envelope combinedExtent = GeometryEngine.combineExtents(geometries);
                if (combinedExtent != null) {
                    // 添加缓冲区
                    Envelope bufferedExtent = new Envelope(
                        combinedExtent.getXMin() - combinedExtent.getWidth() * 0.1,
                        combinedExtent.getYMin() - combinedExtent.getHeight() * 0.1,
                        combinedExtent.getXMax() + combinedExtent.getWidth() * 0.1,
                        combinedExtent.getYMax() + combinedExtent.getHeight() * 0.1,
                        combinedExtent.getSpatialReference()
                    );
                    mMapView.setViewpointGeometryAsync(bufferedExtent, 100);
                }
            } catch (Exception e) {
                Log.e(TAG, "计算图层范围失败: " + e.getMessage());
            }
        }
    }

    // 添加错误处理方法
    private void handleLoadError(String layerName, String errorMessage) {
        Log.e(TAG, layerName + ": " + errorMessage);
        runOnUiThread(() -> {
            Toast.makeText(this, layerName + ": " + errorMessage, Toast.LENGTH_SHORT).show();
        });
    }

    private void startDrawing() {
        if (featureEditor.getCurrentGeometryType() != null) {
            isDrawing = true;
            featureEditor.startEditing(featureEditor.getCurrentGeometryType());
            Toast.makeText(this, "开始绘制", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "请先选择要素类型", Toast.LENGTH_SHORT).show();
        }
    }

    private void completeDrawing() {
        if (isDrawing) {
            featureEditor.completeDrawing();
            isDrawing = false;
            Toast.makeText(this, "绘制完成", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelDrawing() {
        if (isDrawing) {
            featureEditor.stopEditing();
            isDrawing = false;
            Toast.makeText(this, "取消绘制", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveFeatures() {
        String fileName = "编辑要素_" + System.currentTimeMillis();
        featureEditor.saveAsShapefile(fileName);
        Toast.makeText(this, "保存为: " + fileName + ".shp", Toast.LENGTH_SHORT).show();
    }

    // 添加显示加载对话框的方法
    private void showLoadLayerDialog() {
        String[] options = {"从资产加载", "从文件导入"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择加载方式")
               .setItems(options, (dialog, which) -> {
                   if (which == 0) {
                       // 从资产加载
                       String searchText = searchEditText.getText().toString();
                       if (!searchText.isEmpty()) {
                           loadLayerFromAssets(searchText);
                       } else {
                           Toast.makeText(this, "请输入图层名称", Toast.LENGTH_SHORT).show();
                       }
                   } else {
                       // 从文件导入
                       openFilePicker();
                   }
               });
        builder.show();
    }

    // 添加打开文件选择器的方法
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
            startActivityForResult(
                Intent.createChooser(intent, "选择Shapefile文件"),
                PICK_SHAPEFILE_REQUEST
            );
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "请安装文件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    // 修改导入Shapefile的方法
    private void importShapefile(Uri uri) {
        try {
            String fileName = getFileName(uri);
            Log.d(TAG, "选择的文件: " + fileName);

            if (!fileName.toLowerCase().endsWith(".shp")) {
                Toast.makeText(this, "请选择.shp文件", Toast.LENGTH_SHORT).show();
                return;
            }

            // 获取实际文件路径
            String actualPath = getActualPath(uri);
            if (actualPath == null) {
                Log.e(TAG, "无法获取文件路径");
                return;
            }
            Log.d(TAG, "文件实际路径: " + actualPath);

            File sourceFile = new File(actualPath);
            File sourceDir = sourceFile.getParentFile();
            if (!sourceDir.exists()) {
                Log.e(TAG, "源文件目录不存在");
                return;
            }
            Log.d(TAG, "源文件目录: " + sourceDir.getAbsolutePath());

            // 创建导入目录
            File importDir = new File(getFilesDir(), "imported_layers");
            if (!importDir.exists()) {
                importDir.mkdirs();
            }

            // 复制所有相关文件
            String baseName = fileName.substring(0, fileName.length() - 4);
            String[] extensions = {".shp", ".shx", ".dbf", ".prj"};
            boolean allFilesCopied = true;
            List<String> missingFiles = new ArrayList<>();

            for (String ext : extensions) {
                String sourceFileName = baseName + ext;
                File sourceFileWithExt = new File(sourceDir, sourceFileName);
                Log.d(TAG, "检查文件: " + sourceFileWithExt.getAbsolutePath());
                
                if (sourceFileWithExt.exists() && sourceFileWithExt.isFile()) {
                    File destFile = new File(importDir, sourceFileName);
                    try {
                        copyFile(Uri.fromFile(sourceFileWithExt), destFile);
                        Log.d(TAG, "已复制: " + sourceFileName);
                    } catch (IOException e) {
                        Log.e(TAG, "复制文件失败: " + sourceFileName + " - " + e.getMessage());
                        allFilesCopied = false;
                        missingFiles.add(sourceFileName);
                    }
                } else {
                    Log.e(TAG, "文件不存在: " + sourceFileWithExt.getAbsolutePath());
                    allFilesCopied = false;
                    missingFiles.add(sourceFileName);
                }
            }

            if (!allFilesCopied) {
                Toast.makeText(this, "文件缺失: " + String.join(", ", missingFiles), Toast.LENGTH_SHORT).show();
                return;
            }

            // 加图层
            String shapefilePath = new File(importDir, fileName).getAbsolutePath();
            Log.d(TAG, "开始加载Shapefile: " + shapefilePath);

            layerManager.loadShapefileLayer(shapefilePath, new LayerManager.LayerLoadCallback() {
                @Override
                public void onSuccess(FeatureLayer layer) {
                    featureLayers.add(layer);
                    layerNames.add(baseName);
                    layerListAdapter.notifyDataSetChanged();

                    // 默认选中并缩放到新图层
                    ListView layerListView = findViewById(R.id.layerListView);
                    layerListView.setItemChecked(layerNames.size() - 1, true);
                    layerManager.zoomToLayer(layer);
                    
                    Toast.makeText(Main.this, "图层导入成功", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "图层加载失败: " + error);
                    Toast.makeText(Main.this, "图层加载失败: " + error, Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "导入失败: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 添加文件路径获取方法
    private String getActualPath(Uri uri) {
        try {
            Log.d(TAG, "尝试获取文件路径，URI: " + uri.toString());
            
            if ("file".equals(uri.getScheme())) {
                return uri.getPath();
            }
            
            if ("content".equals(uri.getScheme())) {
                // 对于 Downloads 目录的特殊处理
                if (uri.toString().contains("com.android.providers.downloads.documents")) {
                    String path = uri.getPath();
                    if (path != null && path.contains("raw:")) {
                        path = path.substring(path.indexOf("raw:") + 4);
                        Log.d(TAG, "从Downloads解析路径: " + path);
                        return path;
                    }
                }

                // 使用 ContentResolver
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                        if (dataIndex != -1) {
                            String path = cursor.getString(dataIndex);
                            Log.d(TAG, "通过ContentResolver获取路径: " + path);
                            return path;
                        }
                    }
                }

                // 使用 DocumentFile
                DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
                if (documentFile != null) {
                    String path = documentFile.getUri().getPath();
                    if (path != null) {
                        if (path.contains("primary:")) {
                            path = "/storage/emulated/0/" + path.substring(path.indexOf("primary:") + 8);
                        }
                        Log.d(TAG, "通过DocumentFile获取路径: " + path);
                        return path;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取文件路径失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // 添加文件复制方法
    private void copyFile(Uri sourceUri, File destFile) throws IOException {
        Log.d(TAG, "开始复制文件: " + sourceUri + " -> " + destFile.getAbsolutePath());
        
        try {
            InputStream is;
            if ("file".equals(sourceUri.getScheme())) {
                is = new java.io.FileInputStream(new File(sourceUri.getPath()));
            } else {
                is = getContentResolver().openInputStream(sourceUri);
            }
            
            if (is == null) {
                throw new IOException("无法打开输入流: " + sourceUri);
            }

            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[8192];
                int length;
                long totalBytes = 0;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                    totalBytes += length;
                }
                fos.flush();
                Log.d(TAG, "文件复制完成，大小: " + totalBytes + " 字节");
            } finally {
                is.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "复制文件失败: " + e.getMessage());
            throw e;
        }
    }

    // 从资产加载图层的方法
    private void loadLayerFromAssets(String layerName) {
        LayerConfig config = new LayerConfig(
            layerName,
            layerName,
            Color.GRAY,
            1.0f,
            Color.argb(50, 128, 128, 128)
        );
        String shapefilePath = getInternalStorageFilePath(this, layerName + ".shp");
        loadShapefileLayer(shapefilePath, config);
    }

    // 添加获取文件名的方法
    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "获取文件名失败: " + e.getMessage());
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "unknown";
    }

    // 修改缩放至全图的方法
    private void zoomToAllLayers() {
        // 获取所有选中的图层
        List<FeatureLayer> selectedLayers = new ArrayList<>();
        ListView layerListView = findViewById(R.id.layerListView);
        
        for (int i = 0; i < featureLayers.size(); i++) {
            if (layerListView.isItemChecked(i)) {
                selectedLayers.add(featureLayers.get(i));
            }
        }

        if (selectedLayers.isEmpty()) {
            Toast.makeText(this, "请先选择要缩放的图层", Toast.LENGTH_SHORT).show();
            return;
        }

        // 找到选中图层中范围最大的图层
        FeatureLayer largestLayer = null;
        double maxArea = 0;

        for (FeatureLayer layer : selectedLayers) {
            if (layer.getFullExtent() != null) {
                double width = layer.getFullExtent().getWidth();
                double height = layer.getFullExtent().getHeight();
                double area = width * height;
                if (area > maxArea) {
                    maxArea = area;
                    largestLayer = layer;
                }
            }
        }

        if (largestLayer != null) {
            // 使用 final 变量来在 lambda 表达式中引用
            final FeatureLayer finalLargestLayer = largestLayer;
            
            // 缩放到最大图层的范围
            Envelope extent = finalLargestLayer.getFullExtent();
            // 添加缓冲区
            Envelope bufferedExtent = new Envelope(
                extent.getXMin() - extent.getWidth() * 0.1,
                extent.getYMin() - extent.getHeight() * 0.1,
                extent.getXMax() + extent.getWidth() * 0.1,
                extent.getYMax() + extent.getHeight() * 0.1,
                extent.getSpatialReference()
            );

            // 使用动画效果缩放
            mMapView.setViewpointGeometryAsync(bufferedExtent, 1.0)
                .addDoneListener(() -> {
                    String layerName = layerNames.get(featureLayers.indexOf(finalLargestLayer));
                    Toast.makeText(this, "已缩放至: " + layerName, Toast.LENGTH_SHORT).show();
                });
        }
    }
}

