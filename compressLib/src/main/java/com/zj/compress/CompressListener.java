package com.zj.compress;

public interface CompressListener {

    void onProgress(float var1);

    void onSuccess(String var1);

    void onError(int var1);

    void onCancel();
}