package com.zj.compress;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class FileExchangeTask<T extends FileInfo> {
    final OnExchangeResult<T> result;
    final String outputPath;
    final T fi;
    final String suffix;
    final Context context;

    public FileExchangeTask(Context context, String suffix, T input, String outputPath, OnExchangeResult<T> result) {
        this.result = result;
        this.context = context;
        this.fi = input;
        this.suffix = suffix;
        this.outputPath = outputPath;
        CompressLog.d("In order to get enough files available, we make an accessible copy in the application cache directory, you can delete it with new File(path).delete() after use.");
        run();
    }

    private void run() {
        ContentResolver contentResolver = context.getContentResolver();
        FileInputStream fis = null;
        OutputStream to = null;
        try {
            String prefix = null;
            Uri uri = fi.originalPath;
            try (Cursor cursor = contentResolver.query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    prefix = cursor.getString(index);
                    if (prefix.contains(suffix)) {
                        prefix = prefix.replace(suffix, "");
                    } else if (prefix.contains(".") && !prefix.startsWith(".")) {
                        int sub = prefix.lastIndexOf('.');
                        prefix = prefix.substring(0, sub);
                    } else {
                        prefix = prefix.replaceAll("\\.", "_");
                    }
                }
            }
            if (prefix == null || prefix.isEmpty()) {
                prefix = "transform_" + System.currentTimeMillis();
            }
            fi.fileName = prefix;
            ParcelFileDescriptor descriptor = contentResolver.openFileDescriptor(uri, "r");
            if (descriptor == null) {
                return;
            }
            FileDescriptor fd = descriptor.getFileDescriptor();
            fis = new FileInputStream(fd);
            String filePath = outputPath + "/compressTemp/";
            File file = new File(filePath);
            if (!file.mkdirs() && !file.exists()) {
                return;
            }
            String fileName = prefix + suffix;
            File f1 = getGUFile(file, fileName, 0);
            if (f1 == null) throw new NullPointerException("Cannot create file for path : " + file.getPath() + fileName);
            to = new FileOutputStream(f1);
            byte[] b = new byte[1024];
            int c;
            while ((c = fis.read(b)) > 0) {
                to.write(b, 0, c);
            }
            fi.path = Uri.parse(filePath + fileName).getPath();
        } catch (Exception e) {
            CompressLog.e(e);
        } finally {
            try {
                if (fis != null) fis.close();
                if (to != null) to.close();
            } catch (IOException e) {
                CompressLog.e(e);
            }
            result.onResult(fi);
        }
    }

    private File getGUFile(File parent, String fileName, int index) {
        File f1 = new File(parent, fileName);
        if (f1.exists() && f1.length() > 0) {
            int next = index + 1;
            return getGUFile(parent, fileName + "(" + next + ")", next);
        }
        try {
            if (!f1.exists() && !f1.createNewFile()) {
                return null;
            }
        } catch (Exception ignored) {
        }
        return f1;
    }
}