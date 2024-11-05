package com.example.arcgistest2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;

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
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.BackgroundGrid;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.google.android.material.navigation.NavigationView;

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

public class Main extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private MapView mapView; // 地图视图组件
    private FeatureLayer mFeatureLayer; // 要素图层
    // 需要检查的文件列表(实际只需要.shx .shp .dbf .prj)
    private final List<String> filesToCheck = Arrays.asList(
            "测试数据.shp",
            "测试数据.shp.xml",
            "测试数据.shx",
            "测试数据.dbf",
            "测试数据.prj",
            "测试数据.shp",
            "测试数据.cpg",
            "测试数据.sbn",
            "测试数据.sbx"
    );

    // onCreate 方法，当活动创建时调用
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.index); // 设置布局文件

        // 设置按钮点击事件
        setupButtonClickListeners();

        // 检查写入外部存储的权限
        handlePermission();

        // 初始化地图视图
        mapView = findViewById(R.id.mapView);
        mapView.setAttributionTextVisible(false);

        // 复制 Shapefile 文件到内部存储
        copyShapefilesToInternalStorage(this);

        // 获取 Shapefile 文件的路径
        String shapefilePath = getInternalStorageFilePath(this, "测试数据.shp");

        // 设置 Shapefile 特征表
        setupShapefileFeatureTable(shapefilePath);

        // 检查文件是否为空
        for (String fileName : filesToCheck) {
            checkNullFile(fileName);
        }

        // 设置地图视图的视点
        mapView.setViewpointGeometryAsync(mFeatureLayer.getFullExtent());

        // 设置地图背景
        setBackgroundGrid();
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);


        // 处理菜单项点击事件
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {  // 点击“首页”菜单项
                    // 跳转回当前Activity并清空历史栈
                    Intent homeIntent = new Intent(Main.this, Main.class);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  // 清空历史栈
                    startActivity(homeIntent);  // 跳转到首页
                    finish();  // 结束当前Activity（如果需要关闭当前Activity）
                } else if (id == R.id.nav_action1) {  // 点击“其他页面”菜单项
                    // 跳转到其他页面
                    Intent otherIntent = new Intent(Main.this, Action1.class);
                    startActivity(otherIntent);  // 跳转到其他页面
                }else if(id==R.id.nav_action2){
                    Intent SecondIntent = new Intent(Main.this,Action2.class);
                    startActivity(SecondIntent);
                }

                // 关闭导航菜单
                drawerLayout.closeDrawers();
                return true;
            }
        });
    }

    // 设置按钮点击事件
    private void setupButtonClickListeners() {
        // 放大按钮
        Button magnifyButton = findViewById(R.id.magnify);
        magnifyButton.setOnClickListener(v -> handleMagnifyButtonClick());

        // 缩小按钮
        Button shrinkButton = findViewById(R.id.shrink);
        shrinkButton.setOnClickListener(v -> handleShrinkButtonClick());

//        // 提交按钮，用于启动另一个活动
//        Button submitButton = findViewById(R.id.Attribute_queries);
//        submitButton.setOnClickListener(v -> {
//            Intent intent = new Intent(Main.this, Attribute_select.class);
//            startActivityForResult(intent, 1);
//        });

        // 加载图层按钮，可以添加加载图层的代码
        Button loadLayerButton = findViewById(R.id.Load_layer);
        loadLayerButton.setOnClickListener(v -> {
            // 可以添加加载图层的代码
        });

    }

    // onActivityResult 方法，处理从另一个活动返回的结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 1) {
            String fieldName = data.getStringExtra("fieldName");
            String fieldValue = data.getStringExtra("fieldValue");

            // 高亮显示符合条件的要素
            highlightFeatures(fieldName, fieldValue);
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
                        // 处理查询结果并高亮显示要素
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
            mapView.setViewpointGeometryAsync(extent, 100);
        }
    }

    // 处理放大按钮点击事件
    private void handleMagnifyButtonClick() {
        double currentScale = mapView.getMapScale();
        final double MAX_SCALE = currentScale * 5.0;
        final double MIN_SCALE = 1.0;

        double newScale = currentScale * 0.8; // 每次放大到原来的80%

        if (newScale > MAX_SCALE) {
            newScale = MAX_SCALE;
        } else if (newScale < MIN_SCALE) {
            newScale = MIN_SCALE;
        }

        mapView.setViewpointScaleAsync(newScale);
    }

    // 处理缩小按钮点击事件
    private void handleShrinkButtonClick() {
        double currentScale = mapView.getMapScale();
        final double MIN_SCALE = 1.0;
        final double MAX_SCALE = currentScale * 1.6;

        double newScale = currentScale * 1.2;

        if (newScale > MAX_SCALE) {
            newScale = MAX_SCALE;
        } else if (newScale < MIN_SCALE) {
            newScale = MIN_SCALE;
        }

        mapView.setViewpointScaleAsync(newScale);
    }

    // 处理权限请求
    private void handlePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    // 将资源文件复制到内部存储
    public boolean copyAssetToInternalStorage(Context context, String assetName, String destFileName) {
        if (destFileName == null) {
            destFileName = assetName;
        }

        File fileDir = context.getFilesDir();
        File destFile = new File(fileDir, destFileName);

        try (InputStream in = context.getAssets().open(assetName);
             FileOutputStream fos = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            return true;
        } catch (IOException e) {
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

    // 设置 Shapefile 特征表
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
        mapView.setMap(map);

    }

    // 设置地图背景
    private void setBackgroundGrid() {
        BackgroundGrid mainBackgroundGrid = new BackgroundGrid();
        mainBackgroundGrid.setColor(Color.WHITE);
        mainBackgroundGrid.setGridLineColor(Color.WHITE);
        mainBackgroundGrid.setGridLineWidth(0);
        mapView.setBackgroundGrid(mainBackgroundGrid);
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
}

