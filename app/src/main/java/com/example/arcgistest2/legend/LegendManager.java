package com.example.arcgistest2.legend;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class LegendManager {
    private static final String TAG = "LegendManager";
    private final Context context;
    private final List<LegendItem> legendItems = new ArrayList<>();
    private final File legendDir;

    public static class LegendItem {
        public final String name;
        public final File imageFile;
        public final long timestamp;

        public LegendItem(String name, File imageFile, long timestamp) {
            this.name = name;
            this.imageFile = imageFile;
            this.timestamp = timestamp;
        }
    }

    public LegendManager(Context context) {
        this.context = context;
        this.legendDir = new File(context.getFilesDir(), "legends");
        if (!legendDir.exists()) {
            legendDir.mkdirs();
        }
        loadExistingLegends();
    }

    private void loadExistingLegends() {
        File[] files = legendDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".png"));
        if (files != null) {
            for (File file : files) {
                legendItems.add(new LegendItem(
                    file.getName(),
                    file,
                    file.lastModified()
                ));
            }
        }
    }

    public File saveLegendImage(Uri imageUri) throws Exception {
        String fileName = "legend_" + System.currentTimeMillis() + ".png";
        File outputFile = new File(legendDir, fileName);

        try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            
            if (inputStream == null) {
                throw new Exception("无法读取图片");
            }

            // 压缩图片
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            bitmap = Bitmap.createScaledBitmap(bitmap, 
                bitmap.getWidth() / 2, 
                bitmap.getHeight() / 2, 
                true);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
            bitmap.recycle();

            // 添加到列表
            legendItems.add(new LegendItem(
                fileName,
                outputFile,
                System.currentTimeMillis()
            ));

            return outputFile;
        } catch (Exception e) {
            Log.e(TAG, "保存图例失败: " + e.getMessage());
            throw e;
        }
    }

    public List<LegendItem> getLegendItems() {
        return new ArrayList<>(legendItems);
    }

    public void deleteLegend(LegendItem item) {
        if (item.imageFile.delete()) {
            legendItems.remove(item);
        }
    }

    public void clearLegends() {
        for (LegendItem item : legendItems) {
            item.imageFile.delete();
        }
        legendItems.clear();
    }
} 