package com.zj.compress;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class DataSource implements OnExchangeResult {

    public boolean fromTransFile;
    public final Context context;
    private Consumer<FileInfo> onExecutor;
    public FileMimeInfo mimeType;
    public boolean compressEnable = true;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(3);
    private int curTaskNum = 0;
    public final FileInfo fileInfo;

    public DataSource(Context context, Uri uri) {
        this.fileInfo = new FileInfo();
        this.fileInfo.originalPath = uri;
        this.context = context;
    }

    public void start(Consumer<FileInfo> onExecutor) {
        this.onExecutor = onExecutor;
        boolean permissions = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasR = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean hasW = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            permissions = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ? hasR : hasR && hasW;
        }
        if (!permissions) {
            CompressLog.e("permission denied!!");
            onExecutor.accept(fileInfo);
            return;
        }
        changeExternalFileData(context, this);
    }

    private void changeExternalFileData(Context context, OnExchangeResult onExchangeResult) {
        File internalCachedFile = context.getCacheDir();
        File file = new File(internalCachedFile, "temp/uploads/");
        Uri uri = fileInfo.originalPath;
        if (uri == null) onExchangeResult.onResult(fileInfo);
        else {
            ContentResolver resolver = context.getContentResolver();
            mimeType = matchesMimeType(resolver);
            Cursor cursor = null;
            try {
                if (mimeType == null) throw new NullPointerException("can not parse mime-type with path : " + uri);
                if (uri.getScheme() == null || uri.getScheme().equals("file")) {
                    if (mimeType.contentType == FileMimeInfo.CONTENT_IMAGE) {
                        Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        String[] projection = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.HEIGHT};
                        String selection = MediaStore.Video.Media.DATA + "=?";
                        String[] args = new String[]{uri.getPath()};
                        cursor = resolver.query(external, projection, selection, args, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            int columnW = cursor.getColumnIndex(MediaStore.Images.ImageColumns.WIDTH);
                            int columnH = cursor.getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT);
                            int columnId = cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID);
                            fileInfo.width = cursor.getInt(columnW);
                            fileInfo.height = cursor.getInt(columnH);
                            uri = ContentUris.withAppendedId(external, cursor.getLong(columnId));
                        }

                    } else if (mimeType.contentType == FileMimeInfo.CONTENT_VIDEO) {
                        Uri external = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        String selection = MediaStore.Video.Media.DATA + "=?";
                        String[] args = new String[]{uri.getPath()};
                        String[] projection = new String[]{MediaStore.Video.Media._ID};
                        cursor = resolver.query(external, projection, selection, args, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            try {
                                Bitmap bmp = MediaStore.Video.Thumbnails.getThumbnail(resolver, cursor.getLong(0), MediaStore.Video.Thumbnails.MINI_KIND, options);
                                fileInfo.width = bmp.getWidth();
                                fileInfo.height = bmp.getHeight();
                                bmp.recycle();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            int columnId = cursor.getColumnIndex(MediaStore.Video.Media._ID);
                            uri = ContentUris.withAppendedId(external, cursor.getLong(columnId));
                        }
                    } else {
                        throw new NoSuchFieldError("there is no valid type matches for : " + mimeType.mimeType);
                    }
                }
            } catch (Exception e) {
                CompressLog.e(e);
                onExchangeResult.onResult(fileInfo);
                return;
            } finally {
                if (cursor != null) cursor.close();
            }
            curTaskNum++;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                fileInfo.originalPath = uri;
                fromTransFile = true;
                AsyncTask.SERIAL_EXECUTOR.execute(new FileExchangeTask(context, mimeType.suffix, fileInfo, file.getPath(), onExchangeResult));
            } else {
                fromTransFile = false;
                fileInfo.path = fileInfo.originalPath.getPath();
                onExecutor.accept(fileInfo);
            }
        }
    }

    @Nullable
    private FileMimeInfo matchesMimeType(ContentResolver contentResolver) {
        try {
            String mimeType;
            Uri path = fileInfo.originalPath;
            String scheme = path.getScheme();
            if (!TextUtils.isEmpty(scheme) && scheme.equals(ContentResolver.SCHEME_CONTENT)) {
                mimeType = contentResolver.getType(path);
            } else {
                int dotsIndex = path.toString().lastIndexOf(".");
                String fileExtension = path.toString().substring(dotsIndex + 1);
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
            }
            if (TextUtils.isEmpty(mimeType)) return null;
            String suffix = "." + mimeType.split("/")[1];
            Pattern imagePattern = Pattern.compile("image/.*");
            Pattern videoPattern = Pattern.compile("video/.*");
            if (imagePattern.matcher(mimeType).matches()) {
                return new FileMimeInfo(mimeType, suffix, FileMimeInfo.CONTENT_IMAGE);
            } else if (videoPattern.matcher(mimeType).matches()) {
                return new FileMimeInfo(mimeType, suffix, FileMimeInfo.CONTENT_VIDEO);
            }
        } catch (Exception e) {
            CompressLog.e(e);
        }
        return null;
    }

    private static final class FileMimeInfo {

        static final int CONTENT_IMAGE = 0;
        static final int CONTENT_VIDEO = 1;

        String mimeType;
        String suffix;
        int contentType;

        private FileMimeInfo(String mimeType, String suffix, int contentType) {
            this.mimeType = mimeType;
            this.suffix = suffix;
            this.contentType = contentType;
        }
    }

    @Override
    public void onResult(FileInfo info) {
        if (info.path == null || info.path.isEmpty()) CompressLog.e("failed to exchange file with path : " + info.originalPath.getPath());
        if (curTaskNum > 0) curTaskNum--;
        if (curTaskNum <= 0) try {
            threadPool.shutdown();
        } catch (Exception e) {
            CompressLog.e(e);
        }
        if (onExecutor != null) {
            onExecutor.accept(fileInfo);
        }
    }
}
