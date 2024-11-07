package com.example.arcgistest2.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.view.MapView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class LocationManager {
    private static final String TAG = "LocationManager";
    private final Context context;
    private final MapView mapView;
    private android.location.LocationManager systemLocationManager;
    private LocationListener locationListener;
    private List<Point> locationPoints = new ArrayList<>();

    public interface LocationCallback {
        void onLocationReceived(Point point);
        void onError(String error);
    }

    public LocationManager(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        this.systemLocationManager = (android.location.LocationManager) 
            context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void startLocationUpdates(LocationCallback callback) {
        try {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Point point = new Point(location.getLongitude(), 
                        location.getLatitude(), 
                        SpatialReferences.getWgs84());
                    locationPoints.add(point);
                    callback.onLocationReceived(point);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {
                    callback.onError("GPS已禁用");
                }
            };

            systemLocationManager.requestLocationUpdates(
                android.location.LocationManager.GPS_PROVIDER,
                1000, // 最小时间间隔（毫秒）
                1,    // 最小距离间隔（米）
                locationListener
            );
        } catch (SecurityException e) {
            callback.onError("没有位置权限");
        } catch (Exception e) {
            callback.onError("启动位置更新失败: " + e.getMessage());
        }
    }

    public void stopLocationUpdates() {
        try {
            if (locationListener != null && systemLocationManager != null) {
                systemLocationManager.removeUpdates(locationListener);
                locationListener = null;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "停止位置更新失败: " + e.getMessage());
        }
    }

    public void importGPSPoints(File file, LocationCallback callback) {
        try {
            List<Point> points = new ArrayList<>();
            
            // 读取文件内容
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 假设文件格式为: 经度,纬度
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        double longitude = Double.parseDouble(parts[0].trim());
                        double latitude = Double.parseDouble(parts[1].trim());
                        Point point = new Point(longitude, latitude, 
                            SpatialReferences.getWgs84());
                        points.add(point);
                    }
                }
            }

            // 添加所有点
            locationPoints.addAll(points);
            
            // 通知回调
            for (Point point : points) {
                callback.onLocationReceived(point);
            }
        } catch (Exception e) {
            callback.onError("导入GPS点失败: " + e.getMessage());
        }
    }

    public List<Point> getLocationPoints() {
        return new ArrayList<>(locationPoints);
    }

    public void clearLocationPoints() {
        locationPoints.clear();
    }
} 