package com.example.arcgistest2;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupShapefileFeatureTable();
    }

    private void setupShapefileFeatureTable() {
        // 首先确保文件被复制到内部存储
        copyAssetsToInternalStorage();
        
        File internalDir = getFilesDir();
        String shapefilePath = new File(internalDir, "测试数据.shp").getAbsolutePath();
        
        try {
            ShapefileFeatureTable shapefileFeatureTable = new ShapefileFeatureTable(shapefilePath);
            // 后续代码...
        } catch (Exception e) {
            Log.e("MainActivity", "Error loading shapefile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void copyAssetsToInternalStorage() {
        String[] shapefileExtensions = {".shp", ".shx", ".dbf", ".prj", ".sbn"};
        String baseFileName = "测试数据";
        
        for (String ext : shapefileExtensions) {
            try {
                InputStream in = getAssets().open(baseFileName + ext);
                File outFile = new File(getFilesDir(), baseFileName + ext);
                
                if (!outFile.exists()) {
                    FileOutputStream out = new FileOutputStream(outFile);
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                    out.close();
                    in.close();
                }
            } catch (IOException e) {
                Log.e("MainActivity", "Error copying shapefile components: " + e.getMessage());
            }
        }
    }
} 