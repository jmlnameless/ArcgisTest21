package com.example.arcgistest2;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ShapefileTestActivity extends AppCompatActivity {
    private static final String TAG = "ShapefileTest";
    private static final int PICK_SHAPEFILE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 2000;

    private MapView mapView;
    private TextView statusTextView;
    private List<FeatureLayer> loadedLayers = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shapefile_test);

        // 请求权限
        requestPermissions();

        // 初始化视图
        mapView = findViewById(R.id.testMapView);
        statusTextView = findViewById(R.id.tvStatus);
        MaterialButton loadButton = findViewById(R.id.btnLoadFile);

        // 设置状态文本可滚动
        statusTextView.setMovementMethod(new ScrollingMovementMethod());

        // 初始化地图
        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC);
        mapView.setMap(map);

        // 设置加载按钮点击事件
        loadButton.setOnClickListener(v -> openFilePicker());
    }

    private void requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11及以上
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "请求权限失败: " + e.getMessage());
                }
            }
        } else {
            // Android 10及以下
            String[] permissions = {
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择Shapefile文件"), PICK_SHAPEFILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_SHAPEFILE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                handleSelectedFile(data.getData());
            }
        }
    }

    private void handleSelectedFile(Uri uri) {
        try {
            String fileName = getFileName(uri);
            logStatus("选择的文件: " + fileName);

            if (!fileName.toLowerCase().endsWith(".shp")) {
                logStatus("错误: 请选择.shp文件");
                return;
            }

            // 获取实际文件路径
            String actualPath = getActualPath(uri);
            if (actualPath == null) {
                logStatus("错误: 无法获取文件路径");
                return;
            }
            logStatus("文件实际路径: " + actualPath);

            File sourceFile = new File(actualPath);
            File sourceDir = sourceFile.getParentFile();
            if (!sourceDir.exists()) {
                logStatus("错误: 源文件目录不存在");
                return;
            }
            logStatus("源文件目录: " + sourceDir.getAbsolutePath());

            // 创建临时目录
            File tempDir = new File(getFilesDir(), "test_layers");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            // 复制所有相关文件
            String baseName = fileName.substring(0, fileName.length() - 4);
            String[] extensions = {".shp", ".shx", ".dbf", ".prj"};
            boolean allFilesCopied = true;
            List<String> missingFiles = new ArrayList<>();

            for (String ext : extensions) {
                String sourceFileName = baseName + ext;
                File sourceFileWithExt = new File(sourceDir, sourceFileName);
                logStatus("检查文件: " + sourceFileWithExt.getAbsolutePath());
                
                if (sourceFileWithExt.exists() && sourceFileWithExt.isFile()) {
                    File destFile = new File(tempDir, sourceFileName);
                    try {
                        copyFile(Uri.fromFile(sourceFileWithExt), destFile);
                        logStatus("已复制: " + sourceFileName);
                    } catch (IOException e) {
                        logStatus("复制文件失败: " + sourceFileName + " - " + e.getMessage());
                        allFilesCopied = false;
                        missingFiles.add(sourceFileName);
                    }
                } else {
                    logStatus("文件不存在: " + sourceFileWithExt.getAbsolutePath());
                    allFilesCopied = false;
                    missingFiles.add(sourceFileName);
                }
            }

            if (!allFilesCopied) {
                logStatus("错误: 以下文件缺失或无法复制: " + String.join(", ", missingFiles));
                return;
            }

            // 加载Shapefile
            File shpFile = new File(tempDir, fileName);
            loadShapefile(shpFile);

        } catch (Exception e) {
            logStatus("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadShapefile(File shpFile) {
        try {
            logStatus("开始加载Shapefile: " + shpFile.getName());

            ShapefileFeatureTable featureTable = new ShapefileFeatureTable(shpFile.getAbsolutePath());
            featureTable.loadAsync();

            featureTable.addDoneLoadingListener(() -> {
                if (featureTable.getLoadStatus() == LoadStatus.LOADED) {
                    logStatus("要素表加载成功");
                    
                    FeatureLayer layer = new FeatureLayer(featureTable);
                    layer.loadAsync();
                    
                    layer.addDoneLoadingListener(() -> {
                        if (layer.getLoadStatus() == LoadStatus.LOADED) {
                            runOnUiThread(() -> {
                                mapView.getMap().getOperationalLayers().add(layer);
                                loadedLayers.add(layer);
                                logStatus("图层加载成功");
                                
                                // 缩放到图层范围
                                if (layer.getFullExtent() != null) {
                                    mapView.setViewpointGeometryAsync(layer.getFullExtent(), 50);
                                }
                            });
                        } else {
                            logStatus("图层加载失败: " + layer.getLoadError().getMessage());
                        }
                    });
                } else {
                    logStatus("要素表加载失败: " + featureTable.getLoadError().getMessage());
                }
            });
        } catch (Exception e) {
            logStatus("加载Shapefile失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    private Uri findRelatedFile(Uri originalUri, String targetFileName) {
        try {
            // 获取原始文件的实际路径
            String originalPath = getActualPath(originalUri);
            if (originalPath != null) {
                File originalFile = new File(originalPath);
                File parentDir = originalFile.getParentFile();
                logStatus("正在查找文件: " + targetFileName + " 在目录: " + parentDir.getAbsolutePath());
                
                if (parentDir != null && parentDir.exists()) {
                    File[] files = parentDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            logStatus("发现文件: " + file.getName());
                            if (file.getName().equalsIgnoreCase(targetFileName)) {
                                logStatus("找到匹配文件: " + file.getAbsolutePath());
                                return Uri.fromFile(file);
                            }
                        }
                    }
                }
            }

            // 如果通过路径没找到，尝试通过 content resolver
            if ("content".equals(originalUri.getScheme())) {
                String originalFileName = getFileName(originalUri);
                String originalBaseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
                String parentPath = originalPath.substring(0, originalPath.lastIndexOf('/'));
                
                // 使用 MediaStore 查询
                String selection = MediaStore.MediaColumns.DATA + " LIKE ? AND " +
                                   MediaStore.MediaColumns.DISPLAY_NAME + " = ?";
                String[] selectionArgs = {parentPath + "/%", targetFileName};
                
                try (Cursor cursor = getContentResolver().query(
                        MediaStore.Files.getContentUri("external"),
                        null,
                        selection,
                        selectionArgs,
                        null)) {
                    
                    if (cursor != null && cursor.moveToFirst()) {
                        String filePath = cursor.getString(
                            cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
                        return Uri.fromFile(new File(filePath));
                    }
                }
            }

            logStatus("未找到文件: " + targetFileName);
            return null;
        } catch (Exception e) {
            logStatus("查找关联文件失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String getActualPath(Uri uri) {
        try {
            logStatus("尝试获取文件路径，URI: " + uri.toString());
            
            if ("file".equals(uri.getScheme())) {
                return uri.getPath();
            }
            
            if ("content".equals(uri.getScheme())) {
                // 对于 Downloads 目录的特殊处理
                if (uri.toString().contains("com.android.providers.downloads.documents")) {
                    String path = uri.getPath();
                    if (path != null && path.contains("raw:")) {
                        path = path.substring(path.indexOf("raw:") + 4);
                        logStatus("从Downloads解析路径: " + path);
                        return path;
                    }
                }

                // 使用 ContentResolver
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                        if (dataIndex != -1) {
                            String path = cursor.getString(dataIndex);
                            logStatus("通过ContentResolver获取路径: " + path);
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
                        logStatus("通过DocumentFile获取路径: " + path);
                        return path;
                    }
                }
            }
        } catch (Exception e) {
            logStatus("获取文件路径失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private void copyFile(Uri sourceUri, File destFile) throws IOException {
        logStatus("开始复制文件: " + sourceUri + " -> " + destFile.getAbsolutePath());
        
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
                logStatus("文件复制完成，大小: " + totalBytes + " 字节");
            } finally {
                is.close();
            }
        } catch (Exception e) {
            logStatus("复制文件失败: " + e.getMessage());
            throw e;
        }
    }

    private void logStatus(String message) {
        Log.d(TAG, message);
        runOnUiThread(() -> {
            statusTextView.append(message + "\n");
            // 自动滚动到底部
            int scrollAmount = statusTextView.getLayout().getLineTop(statusTextView.getLineCount()) 
                - statusTextView.getHeight();
            if (scrollAmount > 0) {
                statusTextView.scrollTo(0, scrollAmount);
            }
        });
    }

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

    // 添加在同一目录下查找相关文件的方法
    private Uri findRelatedFileInSameDirectory(Uri originalUri, String targetFileName) {
        try {
            DocumentFile originalFile = DocumentFile.fromSingleUri(this, originalUri);
            if (originalFile != null && originalFile.getParentFile() != null) {
                DocumentFile parent = originalFile.getParentFile();
                DocumentFile[] files = parent.listFiles();
                for (DocumentFile file : files) {
                    if (file.getName() != null && file.getName().equalsIgnoreCase(targetFileName)) {
                        return file.getUri();
                    }
                }
            }
        } catch (Exception e) {
            logStatus("查找关联文件失败: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, "需要存储权限才能访问文件", Toast.LENGTH_LONG).show();
            }
        }
    }
} 