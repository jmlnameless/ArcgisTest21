package com.example.arcgistest2.utils;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileManager {
    private final Context context;
    private final String[] requiredFiles;

    public FileManager(Context context, String[] requiredFiles) {
        this.context = context;
        this.requiredFiles = requiredFiles;
    }

    public void copyAssetsToInternalStorage() {
        for (String fileName : requiredFiles) {
            copyAssetFile(fileName);
        }
    }

    private void copyAssetFile(String fileName) {
        try {
            File destFile = new File(context.getFilesDir(), fileName);
            if (!destFile.exists()) {
                try (InputStream in = context.getAssets().open(fileName);
                     FileOutputStream out = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 