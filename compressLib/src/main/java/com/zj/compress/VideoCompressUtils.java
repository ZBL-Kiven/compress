package com.zj.compress;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.qiniu.pili.droid.shortvideo.PLErrorCode;
import com.qiniu.pili.droid.shortvideo.PLShortVideoTranscoder;
import com.qiniu.pili.droid.shortvideo.PLVideoSaveListener;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(@NonNull Message msg) {
            if (msg.what != CODE_PROGRESS) {
                VideoCompressUtils.this.using = false;
            }
            if (listener != null) {
                switch (msg.what) {
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
            Log.e("zj ----- reflect:", "the error code parsed fail, case :" + e.getMessage());
        }
        return new Pair<>(code, "UN_KNOW_ERROR");
    }

    public static CompressConfig create(Application app) {
        if (mVCU == null) {
            mVCU = new VideoCompressUtils();
        }
        VideoCompressUtils.mVCU.config = new CompressConfig(app, mVCU);
        return VideoCompressUtils.mVCU.config;
    }

    private VideoCompressUtils() { }

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

    public void start(CompressListener l) {
        checkPermissions(config.app);
        if (this.using) return;
        this.crackQNSdk();
        this.using = true;
        listener = l;
        MediaMetadataRetriever rt = new MediaMetadataRetriever();
        rt.setDataSource(this.config.app, this.config.mInPath);
        final int height = Integer.parseInt(rt.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        final int width = Integer.parseInt(rt.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        String bitrate = rt.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
        int level = config.mCompressLevel;
        double pob = level * 1.0D / Long.parseLong(bitrate);
        boolean permissions = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasR = config.app.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean hasW = config.app.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            permissions = hasR && hasW;
        }
        if (pob >= 0.85 || !permissions) {
            skipCompress();
        } else {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                changeExternalFileData(new OnExchangeResult() {
                    @Override
                    public void onResult(String path) {
                        if (TextUtils.isEmpty(path)) {
                            sendError(-100);
                            VideoCompressUtils.this.using = false;
                        } else {
                            VideoCompressUtils.this.config.mDataPath = path;
                            startCompress(width, height, true);
                        }
                    }
                });
            } else {
                startCompress(width, height, false);
            }
        }
    }

    private void startCompress(int width, int height, final boolean fromTransFile) {
        if (!listener.onFilePatched(this.config.mDataPath)) {
            VideoCompressUtils.this.using = false;
            return;
        }
        try {
            PLShortVideoTranscoder mShortVideoTranscoder = new PLShortVideoTranscoder(this.config.app, this.config.mDataPath, this.config.getOutPath());
            mShortVideoTranscoder.setMaxFrameRate(25);
            mShortVideoTranscoder.transcode(width, height, this.config.mCompressLevel, new PLVideoSaveListener() {

                @Override
                public void onSaveVideoSuccess(String s) {
                    Message msg = Message.obtain();
                    msg.what = CODE_SUCCESS;
                    msg.obj = s;
                    VideoCompressUtils.this.handler.sendMessage(msg);
                    if (fromTransFile) FileUtils.delete(VideoCompressUtils.this.config.mDataPath);
                }

                @Override
                public void onSaveVideoFailed(int errorCode) {
                    sendError(errorCode);
                    if (fromTransFile) FileUtils.delete(VideoCompressUtils.this.config.mDataPath);
                }

                @Override
                public void onSaveVideoCanceled() {
                    VideoCompressUtils.this.handler.sendEmptyMessage(CODE_CANCELED);
                    if (fromTransFile) FileUtils.delete(VideoCompressUtils.this.config.mDataPath);
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
            if (fromTransFile) FileUtils.delete(VideoCompressUtils.this.config.mDataPath);
        }
    }

    private void changeExternalFileData(OnExchangeResult onExchangeResult) {
        File internalCachedFile = this.config.app.getCacheDir();
        File file = new File(internalCachedFile, "temp/uploads/");
        Uri uri = this.config.mInPath;
        if (uri == null) onExchangeResult.onResult("");
        else {
            if (uri.getScheme() == null || uri.getScheme().equals("file")) {
                try (Cursor cursor = this.config.app.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Video.Media._ID}, MediaStore.Video.Media.DATA + "=? ", new String[]{uri.getPath()}, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                        Uri baseUri = Uri.parse("content://media/external/video/media");
                        uri = Uri.withAppendedPath(baseUri, "" + id);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    onExchangeResult.onResult("");
                }
            }
            Executor ex = Executors.newSingleThreadExecutor();
            ex.execute(new FileExchangeTask(this.config.app, uri, file.getPath(), onExchangeResult));
        }
    }

    private void skipCompress() {
        Message msg = Message.obtain();
        msg.what = CODE_SUCCESS;
        msg.obj = config.mInPath.toString();
        VideoCompressUtils.this.handler.sendMessage(msg);
    }

    private void sendError(int errorCode) {
        Message msg = Message.obtain();
        msg.what = CODE_ERROR;
        msg.arg1 = errorCode;
        VideoCompressUtils.this.handler.sendMessage(msg);
    }

    @TargetApi(23)
    private void checkPermissions(Application app) {
        if (Build.VERSION.SDK_INT < 23) return;
        try {
            boolean write = app.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED;
            boolean read = app.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED;
            if (!write || !read) {
                throw new IllegalArgumentException("compressing abort ,because the necessary [WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE] permissions was denied !");
            }
        } catch (Exception e) {
            final CompressListener l = listener;
            if (l != null) {
                l.onError(443, e.getMessage());
            }
        }
    }

    private interface OnExchangeResult {
        void onResult(String path);
    }

    private static class FileExchangeTask implements Runnable {
        final OnExchangeResult result;
        final String outputPath;
        final Uri inputPath;
        final Context context;

        public FileExchangeTask(Context context, Uri inputPath, String outputPath, OnExchangeResult result) {
            this.result = result;
            this.context = context;
            this.inputPath = inputPath;
            this.outputPath = outputPath;
        }

        @Override
        public void run() {
            ContentResolver contentResolver = context.getContentResolver();
            FileInputStream fis = null;
            OutputStream to = null;
            try {
                ParcelFileDescriptor descriptor = contentResolver.openFileDescriptor(inputPath, "r");
                if (descriptor == null) {
                    result.onResult("");
                    return;
                }
                FileDescriptor fd = descriptor.getFileDescriptor();
                fis = new FileInputStream(fd);
                String filePath = outputPath + "/videos/";
                File file = new File(filePath);
                if (!file.mkdirs() && !file.exists()) {
                    result.onResult("");
                    return;
                }
                String fileName = System.currentTimeMillis() + ".mp4";
                File f1 = new File(file, fileName);
                if (!f1.createNewFile() && !f1.exists()) {
                    result.onResult("");
                    return;
                }
                to = new FileOutputStream(f1);
                byte[] b = new byte[1024];
                int c;
                while ((c = fis.read(b)) > 0) {
                    to.write(b, 0, c);
                }
                result.onResult(filePath + fileName);
            } catch (Exception e) {
                e.printStackTrace();
                result.onResult("");
            } finally {
                try {
                    if (fis != null) fis.close();
                    if (to != null) to.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
