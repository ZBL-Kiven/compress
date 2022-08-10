package com.zj.compress;

import android.content.Context;

import java.io.File;

public abstract class BaseCompressBuilder {

    static String mOutPath = "/ZCompress/";
    public final Context context;

    public BaseCompressBuilder(Context context) {
        this.context = context;
    }

    public File getOutDir(String fileName) {
        File f = new File(context.getCacheDir(), mOutPath);
        if (!f.exists() && !f.mkdirs()) {
            throw new IllegalArgumentException("Cannot create the cache dir with path : " + f.getPath());
        }
        File file = new File(f, fileName);
        if (file.exists() && file.isDirectory()) {
            if (file.delete()) {
                return getOutDir(fileName);
            }
        }
        return file;
    }
}
