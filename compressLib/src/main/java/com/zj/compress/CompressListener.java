package com.zj.compress;

public interface CompressListener {

    boolean onFilePatched(final String path);

    void onProgress(float fraction);

    void onSuccess(String path);

    void onError(int errCode, String s);

    void onCancel();
}