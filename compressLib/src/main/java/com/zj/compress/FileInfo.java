package com.zj.compress;

import android.net.Uri;


@SuppressWarnings("unused")
public class FileInfo {

    public static final int CONTENT_IMAGE = 0;
    public static final int CONTENT_VIDEO = 1;
    public static final int CONTENT_FILE = 2;

    FileInfo(Uri uri) {
        this.originalPath = uri;
    }

    public FileInfo(Uri uri, String mimeType, String suffix, int contentType) {
        this.originalPath = uri;
        this.mimeType = new FileMimeInfo(mimeType, suffix, contentType);
    }

    public boolean fromTransFile = false;

    int w, h;

    long size, bitrate;

    public String fileName;

    public String path;

    public Uri originalPath;

    FileMimeInfo mimeType;

    public String getSuffix() {
        return mimeType.suffix;
    }

    public int getContentType() {
        return mimeType.contentType;
    }


    public static class ImageFileInfo extends FileInfo {

        ImageFileInfo(Uri uri) {
            super(uri);
        }

        public int getWidth() {
            return w;
        }

        public int getHeight() {
            return h;
        }

        public long getSize() {
            return size;
        }
    }

    public static class VideoFileInfo extends ImageFileInfo {

        VideoFileInfo(Uri uri) {
            super(uri);
        }

        public long getBitrate() {
            return bitrate;
        }

    }
}
