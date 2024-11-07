package com.example.arcgistest2.utils;

public class Constants {
    // 请求码
    public static final int PERMISSION_REQUEST_CODE = 1000;
    public static final int PICK_SHAPEFILE_REQUEST = 1001;
    public static final int PICK_GPS_FILE = 1002;
    public static final int PICK_LEGEND_IMAGE = 1003;

    // 文件相关
    public static final String SHAPEFILE_EXTENSION = ".shp";
    public static final String[] SHAPEFILE_REQUIRED_EXTENSIONS = {".shp", ".shx", ".dbf", ".prj"};
    
    // 图层相关
    public static final int MAX_LAYERS = 10;
    public static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    
    // GPS相关
    public static final int GPS_UPDATE_INTERVAL = 1000; // 1秒
    public static final float GPS_MIN_DISTANCE = 1; // 1米
} 