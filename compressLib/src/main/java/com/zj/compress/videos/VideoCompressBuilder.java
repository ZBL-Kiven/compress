package com.zj.compress.videos;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.IntRange;
import java.io.File;
import com.zj.compress.DataSource;
import com.zj.compress.FileInfo;

public class VideoCompressBuilder {

    final Context context;
    final DataSource<FileInfo.VideoFileInfo> dataSource;
    private final String mLocalDirPath;
    private String mOutPath;
    int mCompressLevel;

    public VideoCompressBuilder(Context context, DataSource<FileInfo.VideoFileInfo> dataSource) throws NullPointerException {
        this.context = context;
        this.dataSource = dataSource;
        File f = context.getCacheDir();
        this.mLocalDirPath = f.getPath();
    }

    public VideoCompressBuilder setLevel(@IntRange(from = 500, to = 8000) int level) {
        this.mCompressLevel = (int) ((float) level * 1000.0F);
        return this;
    }

    public VideoCompressBuilder setOutPutFileName(String name) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(mLocalDirPath)) {
            throw new IllegalArgumentException("the output file [" + mLocalDirPath + "/" + name + "] cannot be empty!! ");
        } else {
            this.mOutPath = name;
            return this;
        }
    }

    public void start(final CompressListener listener) {
        dataSource.start((info, e) -> new VideoCompressUtils(this, listener));
    }

    String getOutPath() {
        return this.mLocalDirPath + this.mOutPath;
    }
}
