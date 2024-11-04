package com.example.arcgistest2;


import android.app.Activity;
import android.os.Bundle;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.WebTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.MapView;
//import com.jackiesky.qingzhou.linyefanghuo.TianDiTu.TianDiTuMethodsClass;

public class MainActivity extends Activity {

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化控件
        initView();

        //显示地图
        showMap();
    }

    private void initView(){
        mapView = findViewById(R.id.main_mapview);
    }

    private void showMap(){

        ArcGISRuntimeEnvironment.setLicense("runtimelite,1000,rud4449636536,none,NKMFA0PL4S0DRJE15166");//去水印，lite级别
        mapView.setAttributionTextVisible(false);//去除arcgis水印logo

        //显示平面地图
        WebTiledLayer webTiledLayer = TianDiTuMethodsClass.CreateTianDiTuTiledLayer(TianDiTuMethodsClass.LayerType.TIANDITU_VECTOR_2000);
        Basemap tdtBasemap = new Basemap(webTiledLayer);
        WebTiledLayer webTiledLayer1 = TianDiTuMethodsClass.CreateTianDiTuTiledLayer(TianDiTuMethodsClass.LayerType.TIANDITU_VECTOR_ANNOTATION_CHINESE_2000);
        tdtBasemap.getBaseLayers().add(webTiledLayer1);

        ArcGISMap map = new ArcGISMap(tdtBasemap);
        mapView.setMap(map);
        mapView.setViewpoint(new Viewpoint(36.77669, 118.67922, 10000));
    }

    @Override
    protected void onPause() {
        mapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.resume();
    }

    @Override
    protected void onDestroy() {
        mapView.dispose();
        super.onDestroy();
    }
}