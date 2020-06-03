package com.zj.compress;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.IntRange;

import java.io.File;

@SuppressWarnings("unused,WeakerAccess")
public class CompressConfig {
    private String mLocalDirPath;
    private String mOutPath;
    String mInPath;
    int mCompressLevel;
    Application app;
    VideoCompressUtils vcu;

    public CompressConfig(Application c, VideoCompressUtils vcu) throws NullPointerException {
        File f = c.getExternalFilesDir("");
        if (f == null) {
            throw new NullPointerException("");
        } else {
            this.app = c;
            this.vcu = vcu;
            this.mLocalDirPath = f.getPath();
        }
    }

    public CompressConfig setLevel(@IntRange(from = 500, to = 8000) int level) {
        this.mCompressLevel = (int) ((float) level * 1000.0F);
        return this;
    }

    public CompressConfig setInputFilePath(String path) {
        if (!(new File(path)).exists()) {
            throw new IllegalArgumentException("the input file [" + path + "] was not exits!! ");
        } else {
            this.mInPath = path;
            return this;
        }
    }

    public CompressConfig setOutPutFileName(String name) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("the input file [" + name + "] was not exits!! ");
        } else {
            this.mOutPath = name;
            return this;
        }
    }

    public VideoCompressUtils build() {
        return this.vcu;
    }

    String getOutPath() {
        return this.mLocalDirPath + this.mOutPath;
    }
}
