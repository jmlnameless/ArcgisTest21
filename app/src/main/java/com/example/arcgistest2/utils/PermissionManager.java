package com.example.arcgistest2.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class PermissionManager {
    private final Activity activity;
    private final int requestCode;
    private final PermissionCallback callback;

    public interface PermissionCallback {
        void onPermissionsGranted();
        void onPermissionsDenied();
    }

    public PermissionManager(Activity activity, int requestCode, PermissionCallback callback) {
        this.activity = activity;
        this.requestCode = requestCode;
        this.callback = callback;
    }

    public void checkAndRequestPermissions(String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            } else {
                callback.onPermissionsGranted();
            }
        } else {
            List<String> permissionsNeeded = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(activity, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(permission);
                }
            }

            if (!permissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(activity, 
                    permissionsNeeded.toArray(new String[0]), 
                    requestCode);
            } else {
                callback.onPermissionsGranted();
            }
        }
    }
} 