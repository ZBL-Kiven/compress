package com.zj.compress;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import java.io.File;

public class DataSource<T extends FileInfo> implements Runnable, OnExchangeResult<T> {

    public boolean fromTransFile;
    public final Context context;
    private OnFileTransferListener<T> onExecutor;
    public boolean compressEnable = true;
    public T fileInfo;

    DataSource(Context context, T fileInfo) {
        this.context = context;
        this.fileInfo = fileInfo;
    }

    public void start(OnFileTransferListener<T> onExecutor) {
        this.onExecutor = onExecutor;
        AsyncTask.SERIAL_EXECUTOR.execute(this);
    }

    @Override
    public void onResult(T info, @Nullable Throwable e) {
        if (info == null || info.path == null || info.path.isEmpty()) {
            CompressLog.e("failed to exchange file with path : " + ((info == null) ? " null" : info.originalPath.getPath()));
        }
        if (onExecutor != null) {
            if (info != null) {
                patchFileInfoFromUri(context, Uri.parse(info.path), info);
            }
            this.fileInfo = info;
            long l = fileInfo.limited;
            if (l > 0 && info.size > fileInfo.limited) {
                onExecutor.onChanged(fileInfo, new FileOutOfSizeException(fileInfo.limited, "from file transformer"));
            } else {
                onExecutor.onChanged(fileInfo, e);
            }
        }
    }

    private void patchFileInfoFromUri(Context context, Uri uri, FileInfo fileInfo) {
        if (fileInfo.mimeType.contentType == FileInfo.CONTENT_IMAGE) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try {
                BitmapFactory.decodeFile(uri.getPath(), options);
                fileInfo.w = options.outWidth;
                fileInfo.h = options.outHeight;
                fileInfo.size = new File(uri.getPath()).length();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (fileInfo.mimeType.contentType == FileInfo.CONTENT_VIDEO) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, uri);
            String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            fileInfo.w = Integer.parseInt(width);
            fileInfo.h = Integer.parseInt(height);
            fileInfo.bitrate = Long.parseLong(bitrate);
            fileInfo.size = Long.parseLong(duration) * 1000L;
        } else {
            Cursor cursor = null;
            try {
                String[] projection = new String[]{MediaStore.Files.FileColumns.SIZE};
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnS = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE);
                    fileInfo.size = cursor.getLong(columnS);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
    }

    @Override
    public void run() {
        boolean permissions = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissions) {
            CompressLog.e("permission denied for context : " + context + " !!");
            onExecutor.onChanged(fileInfo, new IllegalArgumentException("permission denied for context : " + context + " !!"));
            return;
        }
        new ExchangeFile<>(this).exchange(context, fileInfo);
    }
}
