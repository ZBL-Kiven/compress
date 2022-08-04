package com.zj.compress;

import androidx.annotation.Nullable;

public interface OnExchangeResult<T extends FileInfo> {

    void onResult(T info,@Nullable Throwable e);
}