package com.zj.compress;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.regex.Pattern;

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
            this.exchangeResult.onResult(fileInfo);
            return;
        }
        ContentResolver resolver = context.getContentResolver();
        fileInfo.mimeType = matchesMimeType(resolver, fileInfo.originalPath);
        boolean isQForest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
        @Nullable String scheme = origin.getScheme();
        try {
            if (scheme == null || scheme.equals(ContentResolver.SCHEME_FILE)) {
                if (isQForest) { // from Q and scheme as file
                    Uri result;
                    if (fileInfo.mimeType.contentType == FileMimeInfo.CONTENT_IMAGE) {
                        Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        result = queryContentUri(context, external, origin);
                    } else if (fileInfo.mimeType.contentType == FileMimeInfo.CONTENT_VIDEO) {
                        Uri external = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        result = queryContentUri(context, external, origin);
                    } else {
                        @SuppressLint("InlinedApi") // [isQForest] checked.
                        Uri external = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
                        result = queryContentUri(context, external, origin);
                    }
                    if (result == null) {
                        throw new IllegalArgumentException("Could not parse the uri : " + origin + " to content provider!");
                    }
                    CompressLog.d("A " + fileInfo.mimeType.suffix + " type file from file-scheme to uri parsed in Q , the uri is :" + result);
                    fileInfo.originalPath = result;
                    fileInfo.fromTransFile = true;
                    new FileExchangeTask<>(context, fileInfo.mimeType.suffix, fileInfo, file.getPath(), exchangeResult);
                } else {
                    fileInfo.path = origin.getPath();
                    fileInfo.fromTransFile = false;
                    exchangeResult.onResult(fileInfo);
                }
            } else if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
                if (isQForest) {
                    CompressLog.d("A " + fileInfo.mimeType.suffix + " type file from file-scheme to uri parsed in Q , the uri is :" + origin);
                    fileInfo.fromTransFile = true;
                    new FileExchangeTask<>(context, fileInfo.mimeType.suffix, fileInfo, file.getPath(), exchangeResult);
                } else {
                    Uri result = queryFileUri(context, origin);
                    if (result != null) {
                        fileInfo.path = result.getPath();
                        fileInfo.fromTransFile = false;
                        exchangeResult.onResult(fileInfo);
                    } else {
                        fileInfo.originalPath = origin;
                        fileInfo.fromTransFile = true;
                        new FileExchangeTask<>(context, fileInfo.mimeType.suffix, fileInfo, file.getPath(), exchangeResult);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            exchangeResult.onResult(null);
        }
    }

    private Uri queryContentUri(Context context, Uri external, Uri uri) {
        String[] projection = new String[]{MediaStore.MediaColumns._ID};
        String selection = MediaStore.Files.FileColumns.DATA + "=?";
        String[] args = new String[]{uri.getPath()};
        try (Cursor cursor = context.getContentResolver().query(external, projection, selection, args, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnId = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
                return ContentUris.withAppendedId(external, cursor.getLong(columnId));
            }
        }
        return null;
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

    @Nullable
    private static FileMimeInfo matchesMimeType(ContentResolver contentResolver, Uri path) {
        try {
            String mimeType;
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
            } else {
                return new FileMimeInfo(mimeType, suffix, FileMimeInfo.CONTENT_FILE);
            }
        } catch (Exception e) {
            CompressLog.e(e);
        }
        return null;
    }
}