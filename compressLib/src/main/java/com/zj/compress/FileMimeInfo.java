package com.zj.compress;

final class FileMimeInfo {

    static final int CONTENT_IMAGE = 0;
    static final int CONTENT_VIDEO = 1;
    static final int CONTENT_FILE = 2;

    String mimeType;
    String suffix;
    int contentType;

    FileMimeInfo(String mimeType, String suffix, int contentType) {
        this.mimeType = mimeType;
        this.suffix = suffix;
        this.contentType = contentType;
    }
}