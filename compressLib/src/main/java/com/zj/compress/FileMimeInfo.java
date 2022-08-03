package com.zj.compress;

final class FileMimeInfo {

    String mimeType;
    String suffix;
    int contentType;

    FileMimeInfo(String mimeType, String suffix, int contentType) {
        this.mimeType = mimeType;
        this.suffix = suffix;
        this.contentType = contentType;
    }
}