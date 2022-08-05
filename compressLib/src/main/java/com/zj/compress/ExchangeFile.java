package com.zj.compress;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import java.io.File;

class ExchangeFile<T extends FileInfo> {

    private final OnExchangeResult<T> exchangeResult;

    ExchangeFile(OnExchangeResult<T> exchangeResult) {
        this.exchangeResult = exchangeResult;
    }

    void exchange(Context context, T fileInfo) {
        File internalCachedFile = context.getCacheDir();
        File file = new File(internalCachedFile, "temp/files/");
        if (!file.exists() && !file.mkdirs()) {
            CompressLog.e("Could not create file for :" + file.getPath());
        }
        Uri origin = fileInfo.originalPath;
        if (origin == null) {
            this.exchangeResult.onResult(fileInfo, new NullPointerException("the file originalPath is null!"));
            return;
        }
        boolean isQForest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
        @Nullable String scheme = origin.getScheme();
        try {
            if (scheme == null || scheme.equals(ContentResolver.SCHEME_FILE)) {
                if (!new File(origin.getPath()).exists() && isQForest) { // from Q and scheme as file
                    CompressLog.d("A file from file-scheme to uri parsed in Q , the uri is :" + origin);
                    fileInfo.originalPath = origin;
                    fileInfo.fromTransFile = true;
                    new FileExchangeTask<>(context, fileInfo, file.getPath(), exchangeResult);
                } else {
                    onPatchPathFile(fileInfo, origin.getPath());
                }
            } else if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
                if (isQForest) {
                    CompressLog.d("A file from file-scheme to uri parsed in Q , the uri is :" + origin);
                    fileInfo.fromTransFile = true;
                    new FileExchangeTask<>(context, fileInfo, file.getPath(), exchangeResult);
                } else {
                    Uri result = queryFileUri(context, origin);
                    if (result != null) {
                        onPatchPathFile(fileInfo, result.getPath());
                    } else {
                        fileInfo.originalPath = origin;
                        fileInfo.fromTransFile = true;
                        new FileExchangeTask<>(context, fileInfo, file.getPath(), exchangeResult);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            exchangeResult.onResult(null, e);
        }
    }

    private void onPatchPathFile(T fileInfo, String result) {
        fileInfo.path = result;
        fileInfo.fromTransFile = false;
        exchangeResult.onResult(fileInfo, null);
    }

    private Uri queryFileUri(Context context, Uri uri) {
        String[] projection = new String[]{MediaStore.MediaColumns.DATA};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnF = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                String path = cursor.getString(columnF);
                return (path == null) ? null : Uri.parse(path);
            }
        }
        return null;
    }
}
