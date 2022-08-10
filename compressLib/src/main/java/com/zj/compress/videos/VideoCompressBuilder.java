package com.zj.compress.videos;

import android.content.Context;

import androidx.annotation.IntRange;

import java.io.File;

import com.zj.compress.BaseCompressBuilder;
import com.zj.compress.CompressUtils;
import com.zj.compress.DataSource;
import com.zj.compress.FileInfo;

public class VideoCompressBuilder extends BaseCompressBuilder {

    final DataSource<FileInfo.VideoFileInfo> dataSource;
    int mCompressLevel;

    public VideoCompressBuilder(Context context, DataSource<FileInfo.VideoFileInfo> dataSource) throws NullPointerException {
        super(context);
        this.dataSource = dataSource;
        File f = context.getCacheDir();
    }

    public VideoCompressBuilder setLevel(@IntRange(from = 500, to = 8000) int level) {
        this.mCompressLevel = (int) ((float) level * 1000.0F);
        return this;
    }

    public void start(final CompressListener listener) {
        dataSource.start((info, e) -> {
            CompressUtils.le(info, e);
            new VideoCompressUtils(this, listener);
        });
    }
}
