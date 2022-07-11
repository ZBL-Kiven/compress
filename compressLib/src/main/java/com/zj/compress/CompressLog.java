package com.zj.compress;

import android.util.Log;

public class CompressLog {

    public static void e(String s) {
        Log.e("------ ZJ.Compress", " error case: " + s);
    }

    public static void d(String s) {
        Log.e("------ ZJ.Compress", " debug : " + s);
    }

    public static void e(Exception e) {
        Log.e("------ ZJ.Compress", " error case: " + e.getMessage());
    }
}
