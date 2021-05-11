package com.zj.compress;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.IntRange;

import java.io.File;

@SuppressWarnings("unused,WeakerAccess")
public class CompressConfig {
    private final String mLocalDirPath;
    private String mOutPath;
    Uri mInPath;
    String mDataPath;
    int mCompressLevel;
    Application app;
    VideoCompressUtils vcu;

    public CompressConfig(Application c, VideoCompressUtils vcu) throws NullPointerException {
        File f = c.getCacheDir();
        if (f == null) {
            throw new NullPointerException("");
        } else {
            this.app = c;
            CompressConfig.this.vcu = vcu;
            this.mLocalDirPath = f.getPath();
        }
    }

    public CompressConfig setLevel(@IntRange(from = 500, to = 8000) int level) {
        this.mCompressLevel = (int) ((float) level * 1000.0F);
        return this;
    }

    public CompressConfig setInputFilePath(Uri uri) {
        assert uri != null;
        if (uri.getScheme() == null || uri.getScheme().equals("file")) {
            String path = uri.getPath();
            if (path == null || !(new File(path)).exists()) {
                throw new IllegalArgumentException("the input file [" + path + "] was not exits!! ");
            }
            mDataPath = path;
        } else {
            mDataPath = getFilePathFromContentUri(app, uri);
        }
        this.mInPath = uri;
        return this;
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
        return vcu;
    }

    String getOutPath() {
        return this.mLocalDirPath + this.mOutPath;
    }

    private static String getFilePathFromContentUri(Context context, Uri contentUri) {
        String scheme = contentUri.getScheme();
        if (scheme == null || !scheme.equals("content")) return contentUri.getPath();
        Cursor cursor = null;
        try {
            String[] pj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, pj, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
