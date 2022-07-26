package com.zj.compress.images;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.zj.compress.CompressLog;
import com.zj.compress.DataSource;
import com.zj.compress.FileInfo;
import com.zj.compress.videos.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

@SuppressWarnings("unused")
class ImgCompressUtils implements Handler.Callback {

    private static final String DEFAULT_DISK_CACHE_DIR = "image_temp_cache";

    private static final int MSG_COMPRESS_FILE_PATCHED = 0;
    private static final int MSG_COMPRESS_START = 1;
    private static final int MSG_COMPRESS_ERROR = 2;
    private static final int MSG_COMPRESS_MULTIPLE_SUCCESS = 3;
    private final String mTargetPath;
    private final DataSource<FileInfo.ImageFileInfo> dataSource;
    private final int mLeastCompressSize;
    private final CompressListener mCompressListener;
    private final Handler mHandler;
    private final int sampleSize;
    private final int quality;
    public static final int ERROR_CODE_FILE_INVALID = 1;
    public static final int ERROR_CODE_CONTEXT_LOSE = 2;

    ImgCompressUtils(ImageCompressBuilder builder, CompressListener compressListener) {
        this.dataSource = builder.dataSource;
        this.mTargetPath = builder.mTargetPath;
        this.mCompressListener = compressListener;
        this.sampleSize = builder.sampleSize;
        this.quality = builder.quality;
        this.mLeastCompressSize = builder.mLeastCompressSize;
        mHandler = new Handler(Looper.getMainLooper(), this);
    }

    /**
     * Returns a mFile with a cache audio name in the private cache directory.
     *
     * @param context A context.
     */
    private File getImageCacheFile(Context context, String suffix) {
        File cache = getImageCacheDir(context);
        if (cache == null) {
            compressError(ERROR_CODE_FILE_INVALID, new FileNotFoundException("no target cached file found"));
            return null;
        }
        if (!cache.exists() && !cache.mkdirs()) {
            return null;
        }
        String targetPath = mTargetPath + (TextUtils.isEmpty(suffix) ? ".JPEG" : suffix);
        return checkOrBuildFile(cache, targetPath);
    }

    private File checkOrBuildFile(File cache, String targetPath) {
        try {
            File target = new File(cache, targetPath);
            if (target.exists()) {
                if (target.isFile()) return target;
                else if (target.delete()) {
                    checkOrBuildFile(cache, targetPath);
                } else return null;
            } else {
                File parent = target.getParentFile();
                if (parent == null) {
                    String s = target.getParent();
                    if (s == null || s.isEmpty()) {
                        s = cache.getPath();
                    }
                    parent = new File(s);
                }
                if ((parent.exists() || parent.mkdirs()) && ((target.exists() && target.isFile()) || target.createNewFile())) {
                    return target;
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }


    /**
     * Returns a directory with a default name in the private cache directory of the application to
     * use to store retrieved audio.
     *
     * @param context A context.
     * @see #getImageCacheDir(Context, String)
     */
    @Nullable
    private File getImageCacheDir(Context context) {
        return getImageCacheDir(context, DEFAULT_DISK_CACHE_DIR);
    }

    /**
     * Returns a directory with the given name in the private cache directory of the application to
     * use to store retrieved media and thumbnails.
     *
     * @param context   A context.
     * @param cacheName The name of the subdirectory in which to store the cache.
     * @see #getImageCacheDir(Context)
     */
    @Nullable
    private File getImageCacheDir(Context context, String cacheName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        File cacheDir = new File(cachePath);
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            return null;
        }
        File result = new File(cacheDir, cacheName);
        if (result.exists() && !result.isDirectory()) {
            if (!result.delete()) return null;
            result = new File(cacheDir, cacheName);
        }
        if (!result.exists() && !result.mkdirs()) {
            return null;
        }
        return result;
    }

    /**
     * start asynchronous compress thread
     */
    @UiThread
    void launch(final Context context) {
        mHandler.sendEmptyMessage(MSG_COMPRESS_FILE_PATCHED);
        if (!dataSource.compressEnable) return;
        final String path = getPath();
        if (context == null) {
            if (dataSource.fromTransFile) FileUtils.delete(path);
            compressError(ERROR_CODE_CONTEXT_LOSE, new NullPointerException("context cannot be null"));
            return;
        }
        if (path == null || path.isEmpty()) {
            if (dataSource.fromTransFile) FileUtils.delete(path);
            compressError(ERROR_CODE_FILE_INVALID, new NullPointerException("image file cannot be null"));
            return;
        }
        AsyncTask.SERIAL_EXECUTOR.execute(() -> {
            boolean compressed = Checker.isNeedCompress(mLeastCompressSize, path);
            try {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_START));
                File result;
                if (compressed) {
                    File f = getImageCacheFile(context, Checker.checkSuffix(path));
                    if (f == null || !f.exists()) {
                        compressError(ERROR_CODE_FILE_INVALID, new NullPointerException("the compress temp file is invalid!"));
                        return;
                    }
                    result = new Engine(path, f, sampleSize, quality).compress();
                } else {
                    result = new File(path);
                }
                mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_MULTIPLE_SUCCESS, result.getAbsolutePath()));
            } catch (Exception e) {
                compressError(ERROR_CODE_FILE_INVALID, e);
            } finally {
                if (compressed && dataSource.fromTransFile) FileUtils.delete(getPath());
            }
        });
    }

    /**
     * start compress and return the mFile
     */
    @WorkerThread
    @Nullable
    File get(Context context) {
        boolean compressed = true;
        try {
            File target = getImageCacheFile(context, Checker.checkSuffix(getPath()));
            if (target == null || !target.exists()) {
                throw new NullPointerException("the file with path " + getPath() + " is invalid!");
            }
            compressed = Checker.isNeedCompress(mLeastCompressSize, getPath());
            return compressed ? new Engine(getPath(), target, sampleSize, quality).compress() : new File(getPath());
        } catch (Exception e) {
            compressError(ERROR_CODE_FILE_INVALID, e);
        } finally {
            if (compressed && dataSource.fromTransFile) FileUtils.delete(getPath());
        }
        return null;
    }

    private void compressError(int code, Exception e) {
        Message msg = Message.obtain();
        msg.what = MSG_COMPRESS_ERROR;
        msg.arg1 = code;
        msg.obj = e;
        mHandler.sendMessage(msg);
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (mCompressListener == null) return false;
        switch (msg.what) {
            case MSG_COMPRESS_FILE_PATCHED:
                mCompressListener.onFileTransform(dataSource.fileInfo, dataSource.compressEnable);
                break;
            case MSG_COMPRESS_START:
                mCompressListener.onStart();
                CompressLog.d("on image compress star...");
                break;
            case MSG_COMPRESS_MULTIPLE_SUCCESS:
                mCompressListener.onSuccess(msg.obj.toString());
                CompressLog.d("on image compress success !");
                break;
            case MSG_COMPRESS_ERROR:
                Throwable e = (Throwable) msg.obj;
                mCompressListener.onError(msg.arg1, e);
                CompressLog.e(new Exception(e));
                break;
        }
        return false;
    }

    private String getPath() {
        return dataSource.fileInfo.path;
    }
}
