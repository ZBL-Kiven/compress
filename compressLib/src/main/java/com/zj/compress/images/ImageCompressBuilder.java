package com.zj.compress.images;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.zj.compress.CompressLog;
import com.zj.compress.DataSource;
import com.zj.compress.FileInfo;

import java.io.File;

@SuppressWarnings("unused")
public class ImageCompressBuilder {

    final Context context;
    final DataSource<FileInfo.ImageFileInfo> dataSource;
    String mTargetPath = "temp_" + System.currentTimeMillis();
    int mLeastCompressSize = 100;
    int quality = 100;
    int sampleSize = 1;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public ImageCompressBuilder(Context context, DataSource<FileInfo.ImageFileInfo> dataSource) {
        this.context = context;
        this.dataSource = dataSource;
    }

    public ImageCompressBuilder setTargetPath(String targetPath) {
        if (!TextUtils.isEmpty(targetPath)) {
            String pathName = "";
            if (targetPath.contains(".")) {
                try {
                    pathName = targetPath.split("\\.")[0];
                } catch (Exception e) {
                    CompressLog.e(e);
                }
            } else {
                pathName = targetPath;
            }
            this.mTargetPath = pathName;
        }
        return this;
    }

    public ImageCompressBuilder setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
        return this;
    }

    public ImageCompressBuilder setQuality(int quality) {
        this.quality = quality;
        return this;
    }

    /**
     * do not compress when the origin image file size less than one value
     *
     * @param size the value of file size, unit KB, default 100K
     */
    public ImageCompressBuilder ignoreBy(int size) {
        this.mLeastCompressSize = size;
        return this;
    }

    /**
     * begin compress image with asynchronous
     */
    public void start(final CompressListener compressListener) {
        dataSource.start((info, e) -> new ImgCompressUtils(this, compressListener).launch(context));
    }

    public void get(final CompressListener compressListener) {
        dataSource.start((info, e) -> {
            File f = new ImgCompressUtils(this, compressListener).get(context);
            if (f != null && f.exists() && !f.isDirectory()) {
                handler.post(() -> compressListener.onSuccess(f.getPath()));
            } else {
                handler.post(() -> compressListener.onError(404, new NullPointerException("no file found!")));
            }
        });
    }
}