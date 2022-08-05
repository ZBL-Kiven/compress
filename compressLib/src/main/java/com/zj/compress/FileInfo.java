package com.zj.compress;

import android.net.Uri;


@SuppressWarnings("unused")
public class FileInfo {

    FileInfo(Uri uri, long limited) {
        this.originalPath = uri;
        this.limited = limited;
    }

    FileInfo(FileInfo info) {
        this.limited = info.limited;
        this.originalPath = info.originalPath;
        this.path = info.path;
        this.fileName = info.fileName;
        this.suffix = info.suffix;
        this.size = info.size;
        this.fromTransFile = info.fromTransFile;
    }

    public FileInfo(Uri uri, String suffix) {
        this.originalPath = uri;
        this.suffix = suffix;
    }

    long limited = -1;

    long size = 0;

    public boolean fromTransFile = false;

    public Uri originalPath;

    public String path;

    public String fileName;

    public String suffix;


    public static class ImageFileInfo extends FileInfo {

        int w = 0, h = 0;

        ImageFileInfo(FileInfo info) {super(info);}

        ImageFileInfo(Uri uri, long limited) {
            super(uri, limited);
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
        long bitrate = 0;

        VideoFileInfo(FileInfo info) {super(info);}

        VideoFileInfo(Uri uri, long limited) {
            super(uri, limited);
        }

        public long getBitrate() {
            return bitrate;
        }

    }
}
