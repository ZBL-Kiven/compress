package com.zj.compress;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

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
}