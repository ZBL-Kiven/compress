package com.zj.compress;

import android.net.Uri;

@SuppressWarnings("unused")
public class FileInfo {

    FileInfo(Uri uri) {
        this.originalPath = uri;
    }

    public boolean fromTransFile = false;

    int w, h;

    long size, bitrate;

    public String path;

    public Uri originalPath;

    public FileMimeInfo mimeType;

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
