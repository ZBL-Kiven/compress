package com.zj.compress.videos;

import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.qiniu.pili.droid.shortvideo.PLErrorCode;
import com.qiniu.pili.droid.shortvideo.PLShortVideoTranscoder;
import com.qiniu.pili.droid.shortvideo.PLVideoSaveListener;
import com.zj.compress.CompressLog;

import java.lang.reflect.Field;

@SuppressWarnings("unused")
public class VideoCompressUtils {

    private final VideoCompressBuilder config;
    private boolean using = false;
    private static final int CODE_START = 4256;
    private static final int CODE_SUCCESS = 4257;
    private static final int CODE_PROGRESS = 4258;
    private static final int CODE_CANCELED = 4259;
    private static final int CODE_ERROR = 4260;
    private final CompressListener listener;

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(@NonNull Message msg) {
            if (msg.what != CODE_PROGRESS) {
                VideoCompressUtils.this.using = false;
            }
            if (listener != null) {
                switch (msg.what) {
                    case CODE_START:
                        listener.onFileTransform(config.dataSource.fileInfo, config.dataSource.compressEnable);
                        break;
                    case CODE_SUCCESS:
                        listener.onSuccess(msg.obj.toString());
                        break;
                    case CODE_PROGRESS:
                        listener.onProgress(Float.parseFloat(msg.obj.toString()));
                        break;
                    case CODE_CANCELED:
                        listener.onCancel();
                        break;
                    case CODE_ERROR:
                        int code = msg.arg1;
                        Pair<Integer, String> e = (code == -1) ? new Pair<>(-1, "DROID_PLUGINS_ERROR") : getMsgWithErrorCode(code);
                        listener.onError(e.first, e.second);
                }
            }
        }
    };

    VideoCompressUtils(VideoCompressBuilder config, CompressListener l) {
        this.config = config;
        this.listener = l;
        if (TextUtils.isEmpty(this.getPath())) {
            sendError(404);
            return;
        }
        if (this.using) {
            sendError(500);
            if (VideoCompressUtils.this.config.dataSource.fromTransFile) {
                FileUtils.delete(VideoCompressUtils.this.getPath());
            }
            return;
        }
        this.using = true;
        handler.sendEmptyMessage(CODE_START);
        if (!config.dataSource.compressEnable) return;
        parseVideoInfo();
    }

    private Pair<Integer, String> getMsgWithErrorCode(int code) {
        try {
            Class<?> cls = PLErrorCode.class;
            Field[] fields = cls.getFields();
            for (Field f : fields) {
                if (!f.isAccessible()) f.setAccessible(true);
                Object o = f.get(null);
                int fc = (int) (o == null ? 0 : o);
                if (fc == code) {
                    return new Pair<>(-1, f.getName());
                }
            }
        } catch (Exception e) {
            CompressLog.e("the error code parsed fail, case :" + e.getMessage());
        }
        String s;
        switch (code) {
            case -100:
                s = "exchange error";
                break;
            case -101:
                s = "permission denied";
                break;
            case 404:
                s = "path is null or empty";
                break;
            case 500:
                s = "another video already in compressing!";
                break;
            default:
                s = "UN_KNOW_ERROR";

        }
        return new Pair<>(code, s);
    }

    private void crackQNSdk() {
        try {
            SharedPreferences sp = this.config.context.getSharedPreferences("ShortVideo", 0);
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
            CompressLog.d("\noc = " + c + "    of = " + var4 + "  ,   nc = " + t + "   nf = " + f);
        } catch (Exception var13) {
            var13.printStackTrace();
        }
    }

    private void parseVideoInfo() {
        String path = this.getPath();
        String bitrate = null;
        int height = 0;
        int width = 0;
        try {
            MediaMetadataRetriever rt = new MediaMetadataRetriever();
            rt.setDataSource(this.config.context, Uri.parse(path));
            height = Integer.parseInt(rt.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            width = Integer.parseInt(rt.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            bitrate = rt.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (bitrate == null) {
            skipCompress();
            return;
        }
        int level = config.mCompressLevel;
        double pob = level * 1.0D / Long.parseLong(bitrate);
        if (pob >= 0.85) {
            skipCompress();
        } else {
            this.crackQNSdk();
            startCompress(width, height);
        }
    }

    private void startCompress(int width, int height) {
        try {
            PLShortVideoTranscoder mShortVideoTranscoder = new PLShortVideoTranscoder(this.config.context, this.getPath(), this.config.getOutPath());
            mShortVideoTranscoder.setMaxFrameRate(25);
            mShortVideoTranscoder.transcode(width, height, this.config.mCompressLevel, new PLVideoSaveListener() {

                @Override
                public void onSaveVideoSuccess(String s) {
                    Message msg = Message.obtain();
                    msg.what = CODE_SUCCESS;
                    msg.obj = s;
                    VideoCompressUtils.this.handler.sendMessage(msg);
                    if (VideoCompressUtils.this.config.dataSource.fromTransFile) {
                        FileUtils.delete(VideoCompressUtils.this.getPath());
                    }
                }

                @Override
                public void onSaveVideoFailed(int errorCode) {
                    sendError(errorCode);
                    if (VideoCompressUtils.this.config.dataSource.fromTransFile) {
                        FileUtils.delete(VideoCompressUtils.this.getPath());
                    }
                }

                @Override
                public void onSaveVideoCanceled() {
                    VideoCompressUtils.this.handler.sendEmptyMessage(CODE_CANCELED);
                    if (VideoCompressUtils.this.config.dataSource.fromTransFile) {
                        FileUtils.delete(VideoCompressUtils.this.getPath());
                    }
                }

                @Override
                public void onProgressUpdate(float percentAg) {
                    Message msg = Message.obtain();
                    msg.what = CODE_PROGRESS;
                    msg.obj = percentAg;
                    VideoCompressUtils.this.handler.sendMessage(msg);
                }
            });
        } catch (Exception var6) {
            var6.printStackTrace();
            sendError(-1);
            this.using = false;
            if (VideoCompressUtils.this.config.dataSource.fromTransFile) {
                FileUtils.delete(VideoCompressUtils.this.getPath());
            }
        }
    }

    private String getPath() {
        return config.dataSource.fileInfo.path;
    }

    private void skipCompress() {
        Message msg = Message.obtain();
        msg.what = CODE_SUCCESS;
        msg.obj = getPath();
        VideoCompressUtils.this.handler.sendMessage(msg);
    }

    private void sendError(int errorCode) {
        Message msg = Message.obtain();
        msg.what = CODE_ERROR;
        msg.arg1 = errorCode;
        VideoCompressUtils.this.handler.sendMessage(msg);
    }
}
