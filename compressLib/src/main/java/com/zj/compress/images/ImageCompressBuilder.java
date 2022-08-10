package com.zj.compress.images;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.zj.compress.BaseCompressBuilder;
import com.zj.compress.CompressUtils;
import com.zj.compress.DataSource;
import com.zj.compress.FileInfo;

import java.io.File;

@SuppressWarnings("unused")
public class ImageCompressBuilder extends BaseCompressBuilder {

    final DataSource<FileInfo.ImageFileInfo> dataSource;

    int mLeastCompressSize = 100;
    int quality = 100;
    int sampleSize = 1;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public ImageCompressBuilder(Context context, DataSource<FileInfo.ImageFileInfo> dataSource) {
        super(context);
        this.dataSource = dataSource;
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
        dataSource.start((info, e) -> {
            CompressUtils.le(info, e);
            new ImgCompressUtils(this, compressListener).launch();
        });
    }

    public void get(final CompressListener compressListener) {
        dataSource.start((info, e) -> {
            CompressUtils.le(info, e);
            File f = new ImgCompressUtils(this, compressListener).get();
            if (f != null && f.exists() && !f.isDirectory()) {
                handler.post(() -> compressListener.onSuccess(f.getPath()));
            } else {
                handler.post(() -> compressListener.onError(404, new NullPointerException("no file found!")));
            }
        });
    }
}