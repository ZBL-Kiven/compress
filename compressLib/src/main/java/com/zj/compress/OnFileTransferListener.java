package com.zj.compress;

import androidx.annotation.Nullable;

public interface OnFileTransferListener<T> {

    void onChanged(T info,@Nullable Throwable e);

}
