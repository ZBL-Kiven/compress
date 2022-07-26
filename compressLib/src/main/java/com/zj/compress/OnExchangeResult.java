package com.zj.compress;

public interface OnExchangeResult<T extends FileInfo> {

    void onResult(T info);
}