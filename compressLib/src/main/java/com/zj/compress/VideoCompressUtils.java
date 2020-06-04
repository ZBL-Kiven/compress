package com.zj.compress;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.qiniu.pili.droid.shortvideo.PLShortVideoTranscoder;
import com.qiniu.pili.droid.shortvideo.PLVideoSaveListener;

@SuppressWarnings("unused")
public class VideoCompressUtils {
    private static VideoCompressUtils mVCU;
    private CompressConfig config;
    private boolean using = false;
    private static final int CODE_SUCCESS = 4257;
    private static final int CODE_PROGRESS = 4258;
    private static final int CODE_CANCELED = 4259;
    private static final int CODE_ERROR = 4260;
    private CompressListener listener;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(@NonNull Message msg) {
            if (msg.what != CODE_PROGRESS) {
                VideoCompressUtils.this.using = false;
            }

            if (VideoCompressUtils.this.listener != null) {
                switch (msg.what) {
                    case CODE_SUCCESS:
                        VideoCompressUtils.this.listener.onSuccess(msg.obj.toString());
                        break;
                    case CODE_PROGRESS:
                        VideoCompressUtils.this.listener.onProgress(Float.parseFloat(msg.obj.toString()));
                        break;
                    case CODE_CANCELED:
                        VideoCompressUtils.this.listener.onCancel();
                        break;
                    case CODE_ERROR:
                        VideoCompressUtils.this.listener.onError(msg.arg1);
                }

            }
        }
    };

    public static CompressConfig create(Application app) {
        checkPermissions(app);
        if (mVCU == null) {
            mVCU = new VideoCompressUtils();
        }

        mVCU.config = new CompressConfig(app, mVCU);
        return mVCU.config;
    }

    private VideoCompressUtils() {
    }

    @TargetApi(23)
    private static void checkPermissions(Application app) {
        boolean write = app.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED;
        boolean read = app.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED;
        if (!write || !read) {
            throw new IllegalArgumentException("compressing abort ,because the necessary [WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE] permissions was denied !");
        }
    }

    private void crackQNSdk() {
        try {
            SharedPreferences sp = this.config.app.getSharedPreferences("ShortVideo", 0);
            String var3 = sp.getString("ts", "");
            String var4 = sp.getString("feature", "");
            long c = 0L;
            if (!"".equals(var3)) {
                byte[] var5 = Base64.decode(var3, 0);
                c = Long.parseLong(new String(var5));
            }

            long cur = System.currentTimeMillis();
            long feature = cur + 189216000000L;
            String t = Base64.encodeToString(String.valueOf(cur).getBytes(), 0);
            String f = Base64.encodeToString(String.valueOf(feature).getBytes(), 0);
            sp.edit().putString("ts", t).apply();
            sp.edit().putString("feature", f).apply();
            Log.e("zj ----- charge crack :", "\noc = " + c + "    of = " + var4 + "  ,   nc = " + t + "   nf = " + f);
        } catch (Exception var13) {
            var13.printStackTrace();
        }
    }

    public void start(CompressListener listener) {
        if (this.using) return;
        this.crackQNSdk();
        this.using = true;
        this.listener = listener;
        MediaMetadataRetriever rt = new MediaMetadataRetriever();
        rt.setDataSource(this.config.mInPath);
        String height = rt.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String width = rt.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String bitrate = rt.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);

        int level = config.mCompressLevel;
        double pob = level * 1.0D / Long.parseLong(bitrate);
        if (pob >= 0.85) {
            Message msg = Message.obtain();
            msg.what = 4257;
            msg.obj = config.mInPath;
            VideoCompressUtils.this.handler.sendMessage(msg);
        } else {
            try {
                PLShortVideoTranscoder mShortVideoTranscoder = new PLShortVideoTranscoder(this.config.app, this.config.mInPath, this.config.getOutPath());
                mShortVideoTranscoder.setMaxFrameRate(25);
                mShortVideoTranscoder.transcode(Integer.parseInt(width), Integer.parseInt(height), this.config.mCompressLevel, false, new PLVideoSaveListener() {
                    public void onSaveVideoSuccess(String s) {
                        Message msg = Message.obtain();
                        msg.what = 4257;

                        msg.obj = s;
                        VideoCompressUtils.this.handler.sendMessage(msg);
                    }

                    public void onSaveVideoFailed(int errorCode) {
                        Message msg = Message.obtain();
                        msg.what = 4260;
                        msg.arg1 = errorCode;
                        VideoCompressUtils.this.handler.sendMessage(msg);
                    }

                    public void onSaveVideoCanceled() {
                        VideoCompressUtils.this.handler.sendEmptyMessage(4259);
                    }

                    public void onProgressUpdate(float percentAg) {
                        Message msg = Message.obtain();
                        msg.what = 4258;
                        msg.obj = percentAg;
                        VideoCompressUtils.this.handler.sendMessage(msg);
                    }
                });
            } catch (Exception var6) {
                var6.printStackTrace();
            }
        }
    }
}
