package com.zj.compress;

import android.content.ContentResolver;
import android.content.Context;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class FileExchangeTask implements Runnable {
    final OnExchangeResult result;
    final String outputPath;
    final FileInfo fi;
    final String suffix;
    final Context context;

    public FileExchangeTask(Context context, String suffix, FileInfo input, String outputPath, OnExchangeResult result) {
        this.result = result;
        this.context = context;
        this.fi = input;
        this.suffix = suffix;
        this.outputPath = outputPath;
    }

    @Override
    public void run() {
        ContentResolver contentResolver = context.getContentResolver();
        FileInputStream fis = null;
        OutputStream to = null;
        try {
            ParcelFileDescriptor descriptor = contentResolver.openFileDescriptor(fi.originalPath, "r");
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
            String fileName = System.currentTimeMillis() + suffix;
            File f1 = new File(file, fileName);
            if (!f1.exists() && !f1.createNewFile()) {
                return;
            }
            to = new FileOutputStream(f1);
            byte[] b = new byte[1024];
            int c;
            while ((c = fis.read(b)) > 0) {
                to.write(b, 0, c);
            }
            fi.path = filePath + fileName;
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
}