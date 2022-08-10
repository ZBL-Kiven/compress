package com.zj.compress;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.zj.compress.images.ImageCompressBuilder;
import com.zj.compress.videos.VideoCompressBuilder;

import java.io.File;

@SuppressWarnings("unused")
public class CompressUtils {

    private final Context context;
    private Uri originalUri;
    private long limited = -1;

    public static void setOutputDir(String dir) {
        if (!dir.endsWith("/")) dir += "/";
        BaseCompressBuilder.mOutPath = dir;
    }

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
            File f = new File(uri.getPath());
            if (f.exists()) {
                uri = Uri.fromFile(f);
            } else {
                uri = Uri.parse(ContentResolver.SCHEME_FILE + "://" + uri.getPath());
            }
        }
        this.originalUri = uri;
        return this;
    }

    public CompressUtils limit(long size) {
        this.limited = size;
        return this;
    }

    public ImageCompressBuilder asImage() {
        if (originalUri == null) throw new NullPointerException("please call load() before!");
        DataSource<FileInfo.ImageFileInfo> mDataSource = new DataSource<>(context, new FileInfo.ImageFileInfo(originalUri, limited));
        return new ImageCompressBuilder(context, mDataSource);
    }

    public VideoCompressBuilder asVideo() {
        if (originalUri == null) throw new NullPointerException("please call load() before!");
        DataSource<FileInfo.VideoFileInfo> mDataSource = new DataSource<>(context, new FileInfo.VideoFileInfo(originalUri, limited));
        return new VideoCompressBuilder(context, mDataSource);
    }

    public void transForAndroidQ(final OnFileTransferListener<FileInfo> consumer) {
        if (originalUri == null) throw new NullPointerException("please call load() before!");
        DataSource<FileInfo> mDataSource = new DataSource<>(context, new FileInfo(originalUri, limited));
        mDataSource.start((info, e) -> {
            le(info, e);
            new Handler(Looper.getMainLooper()).post(() -> consumer.onChanged(info, e));
        });
    }

    public static void le(FileInfo info, Throwable e) {
        if (info == null || info.path == null || info.path.isEmpty()) {
            CompressLog.e("failed to transform file with path : " + ((info == null) ? " null" : info.originalPath.getPath()) + "  case : " + (e == null ? "" : e.getMessage()));
        }
    }
}
