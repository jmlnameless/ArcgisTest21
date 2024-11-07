package com.example.arcgistest2.base;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.BackgroundGrid;
import android.util.Log;
import android.widget.Toast;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.geometry.SpatialReferences;

public abstract class BaseMapActivity extends AppCompatActivity {
    protected MapView mapView;
    protected ArcGISMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        initializeMap();
        waitForMapLoad();
    }

    protected abstract int getLayoutId();
    protected abstract void setupUI();
    protected abstract void initializeManagers();

    protected void initializeMap() {
        mapView = findViewById(getMapViewId());
        map = new ArcGISMap(SpatialReferences.getWgs84());
        mapView.setMap(map);
        
        // 设置背景网格
        BackgroundGrid mainBackgroundGrid = new BackgroundGrid();
        mainBackgroundGrid.setColor(Color.WHITE);
        mainBackgroundGrid.setGridLineColor(Color.WHITE);
        mainBackgroundGrid.setGridLineWidth(0);
        mapView.setBackgroundGrid(mainBackgroundGrid);
    }

    private void waitForMapLoad() {
        if (map != null) {
            map.addDoneLoadingListener(() -> {
                if (map.getLoadStatus() == LoadStatus.LOADED) {
                    // 地图加载完成后按顺序初始化
                    runOnUiThread(() -> {
                        try {
                            // 先初始化管理器
                            initializeManagers();
                            // 然后设置UI
                            setupUI();
                            // 最后执行地图加载完成后的操作
                            onMapLoaded();
                        } catch (Exception e) {
                            Log.e("BaseMapActivity", "初始化失败: " + e.getMessage());
                            Toast.makeText(this, "初始化失败: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // 地图加载失败
                    String error = map.getLoadError() != null ? 
                        map.getLoadError().getMessage() : "未知错误";
                    Log.e("BaseMapActivity", "地图加载失败: " + error);
                    runOnUiThread(() -> 
                        Toast.makeText(this, "地图加载失败: " + error, 
                            Toast.LENGTH_SHORT).show()
                    );
                }
            });
            map.loadAsync();
        }
    }

    // 添加地图加载完成后的回调方法
    protected void onMapLoaded() {
        // 子类可以重写此方法以在地图加载完成后执行操作
    }

    protected abstract int getMapViewId();

    @Override
    protected void onPause() {
        if (mapView != null) {
            mapView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) {
            mapView.dispose();
        }
        super.onDestroy();
    }
} 