package com.example.arcgistest2.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

public class FileUtils {
    public static String getPath(Context context, Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // 处理 Document Uri
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                String[] split = docId.split(":");
                String type = split[0];
                String[] columns = {MediaStore.Files.FileColumns.DATA};
                String selection = MediaStore.Files.FileColumns.DATA + " LIKE '%" + split[1] + "'";
                return getDataColumn(context, MediaStore.Files.getContentUri("external"), 
                    selection, null);
            }
        }
        // 处理 Content Uri
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // 处理 File Uri
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, 
        String[] selectionArgs) {
        Cursor cursor = null;
        final String column = MediaStore.Files.FileColumns.DATA;
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, 
                selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }
} 