package com.zj.compress;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.util.Consumer;

import com.zj.compress.images.ImageCompressBuilder;
import com.zj.compress.videos.VideoCompressBuilder;

import java.io.File;

@SuppressWarnings("unused")
public class CompressUtils {

    private final Context context;
    private Uri originalUri;

    public CompressUtils(Context context) {
        this.context = context.getApplicationContext();
    }

    public static CompressUtils with(Context context) {
        return new CompressUtils(context);
    }

    public CompressUtils load(File file) {
        return load(Uri.fromFile(file));
    }

    public CompressUtils load(String path) {
        return load(Uri.parse(path));
    }

    public CompressUtils load(Uri uri) {
        if (TextUtils.isEmpty(uri.getScheme())) {
            uri = Uri.parse(ContentResolver.SCHEME_FILE + "://" + uri.getPath());
        }
        this.originalUri = uri;
        return this;
    }

    public ImageCompressBuilder asImage() {
        if (originalUri == null) throw new NullPointerException("please call load() before!");
        DataSource<FileInfo.ImageFileInfo> mDataSource = new DataSource<>(context, new FileInfo.ImageFileInfo(originalUri));
        return new ImageCompressBuilder(context, mDataSource);
    }

    public VideoCompressBuilder asVideo() {
        if (originalUri == null) throw new NullPointerException("please call load() before!");
        DataSource<FileInfo.VideoFileInfo> mDataSource = new DataSource<>(context, new FileInfo.VideoFileInfo(originalUri));
        return new VideoCompressBuilder(context, mDataSource);
    }

    public void transForAndroidQ(Consumer<FileInfo> consumer) {
        if (originalUri == null) throw new NullPointerException("please call load() before!");
        DataSource<FileInfo> mDataSource = new DataSource<>(context, new FileInfo(originalUri));
        mDataSource.start(consumer);
    }
}
