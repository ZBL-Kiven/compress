package com.zj.compress.videos;

import com.zj.compress.FileInfo;

public interface CompressListener {

    void onFileTransform(final FileInfo info, final boolean compressEnable);

    void onProgress(float fraction);

    void onSuccess(String path);

    void onError(int errCode, String s);

    void onCancel();
}