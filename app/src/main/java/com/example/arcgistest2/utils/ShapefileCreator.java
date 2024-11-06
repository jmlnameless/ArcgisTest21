package com.example.arcgistest2.utils;

import android.content.Context;
import android.util.Log;

import com.esri.arcgisruntime.geometry.GeometryType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ShapefileCreator {
    private static final String TAG = "ShapefileCreator";

    public static boolean createShapefile(Context context, String fileName, GeometryType geometryType) {
        try {
            // 创建输出目录
            File outputDir = new File(context.getFilesDir(), "shapefiles");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // 创建基本文件
            createShpFile(new File(outputDir, fileName + ".shp"), geometryType);
            createShxFile(new File(outputDir, fileName + ".shx"), geometryType);
            createDbfFile(new File(outputDir, fileName + ".dbf"));
            createPrjFile(new File(outputDir, fileName + ".prj"));

            return true;
        } catch (Exception e) {
            Log.e(TAG, "创建Shapefile失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void createShpFile(File file, GeometryType geometryType) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // 写入文件头（100字节）
            ByteBuffer header = ByteBuffer.allocate(100);
            header.order(ByteOrder.BIG_ENDIAN);

            // 文件代码（9994）
            header.putInt(9994);
            
            // 5个未使用的4字节整数
            for (int i = 0; i < 5; i++) {
                header.putInt(0);
            }
            
            // 文件长度（以16位字计）
            header.putInt(50); // 初始长度为100字节（50个16位字）
            
            // 版本号
            header.order(ByteOrder.LITTLE_ENDIAN);
            header.putInt(1000);
            
            // 几何类型
            int shapeType;
            switch (geometryType) {
                case POINT:
                    shapeType = 1;
                    break;
                case POLYLINE:
                    shapeType = 3;
                    break;
                case POLYGON:
                    shapeType = 5;
                    break;
                default:
                    shapeType = 0;
            }
            header.putInt(shapeType);
            
            // 边界框（8个双精度浮点数）
            for (int i = 0; i < 8; i++) {
                header.putDouble(0.0);
            }

            fos.write(header.array());
        }
    }

    private static void createShxFile(File file, GeometryType geometryType) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // 写入与.shp文件相同的头部
            ByteBuffer header = ByteBuffer.allocate(100);
            header.order(ByteOrder.BIG_ENDIAN);
            header.putInt(9994);
            for (int i = 0; i < 5; i++) {
                header.putInt(0);
            }
            header.putInt(50);
            
            header.order(ByteOrder.LITTLE_ENDIAN);
            header.putInt(1000);
            
            int shapeType;
            switch (geometryType) {
                case POINT:
                    shapeType = 1;
                    break;
                case POLYLINE:
                    shapeType = 3;
                    break;
                case POLYGON:
                    shapeType = 5;
                    break;
                default:
                    shapeType = 0;
            }
            header.putInt(shapeType);
            
            for (int i = 0; i < 8; i++) {
                header.putDouble(0.0);
            }

            fos.write(header.array());
        }
    }

    private static void createDbfFile(File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // DBF文件头（32字节）
            byte[] header = new byte[32];
            
            // 版本号（dBASE III）
            header[0] = 0x03;
            
            // 最后更新日期（年月日）
            java.util.Calendar cal = java.util.Calendar.getInstance();
            header[1] = (byte) (cal.get(java.util.Calendar.YEAR) - 1900);
            header[2] = (byte) (cal.get(java.util.Calendar.MONTH) + 1);
            header[3] = (byte) cal.get(java.util.Calendar.DAY_OF_MONTH);
            
            // 记录数（0）
            header[4] = header[5] = header[6] = header[7] = 0;
            
            // 头部长度（包括字段描述符）
            short headerLength = (short) (32 + 32 + 1);
            header[8] = (byte) (headerLength & 0xFF);
            header[9] = (byte) ((headerLength >> 8) & 0xFF);
            
            // 记录长度
            header[10] = 11; // ID字段长度为10 + 删除标记1字节
            header[11] = 0;

            fos.write(header);
            
            // 写入字段描述符
            byte[] fieldDescriptor = new byte[32];
            System.arraycopy("ID".getBytes(), 0, fieldDescriptor, 0, 2);
            fieldDescriptor[11] = 'N'; // 数值类型
            fieldDescriptor[16] = 10;  // 字段长度
            fieldDescriptor[17] = 0;   // 小数位数
            
            fos.write(fieldDescriptor);
            
            // 写入终止符和文件结束标记
            fos.write(0x0D);
            fos.write(0x1A);
        }
    }

    private static void createPrjFile(File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // WGS84坐标系的WKT字符串
            String wkt = "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]]," +
                        "PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]]";
            fos.write(wkt.getBytes());
        }
    }
} 