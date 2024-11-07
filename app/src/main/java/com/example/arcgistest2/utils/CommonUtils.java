package com.example.arcgistest2.utils;

import android.content.Context;
import android.widget.Toast;
import android.util.Log;

public class CommonUtils {
    private static final String TAG = "CommonUtils";

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showLongToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static void logError(String tag, String message, Exception e) {
        Log.e(tag, message + ": " + e.getMessage());
        e.printStackTrace();
    }

    public static void logDebug(String tag, String message) {
        Log.d(tag, message);
    }

    public static String formatFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
} 