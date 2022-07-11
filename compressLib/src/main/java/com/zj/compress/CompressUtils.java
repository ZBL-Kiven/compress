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

    private DataSource mDataSource;
    private final Context context;

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
        mDataSource = new DataSource(context, uri);
        return this;
    }

    public ImageCompressBuilder asImage() {
        if (mDataSource == null) throw new NullPointerException("please call load() before!");
        return new ImageCompressBuilder(context, mDataSource);
    }

    public VideoCompressBuilder asVideo() {
        if (mDataSource == null) throw new NullPointerException("please call load() before!");
        return new VideoCompressBuilder(context, mDataSource);
    }

    public void transForAndroidQ(Consumer<FileInfo> consumer) {
        if (mDataSource == null) throw new NullPointerException("please call load() before!");
        mDataSource.start(consumer);
    }
}
