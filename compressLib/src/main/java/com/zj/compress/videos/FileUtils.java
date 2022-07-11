package com.zj.compress.videos;


import com.zj.compress.CompressLog;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;

@SuppressWarnings({"unused", "WeakerAccess"})
public class FileUtils {
    private FileUtils() { }

    private static long getFileSize(File file) {
        long size = 0L;

        try {
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                size = fis.available();
            } else {
                CompressLog.e("cannot get file size ,the file is not exists");
            }

            return size;
        } catch (Exception var4) {
            var4.printStackTrace();
            return size;
        }
    }

    public static String getFormatSize(String path) {
        long size = getFileSize(new File(path));
        return getFormatSize(size);
    }

    public static String getFormatSize(double size) {
        double kiloByte = size / 1024.0D;
        if (kiloByte < 1.0D) {
            return size + "MB";
        } else {
            double megaByte = kiloByte / 1024.0D;
            if (megaByte < 1.0D) {
                BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
                return result1.setScale(2, 4).toPlainString() + "KB";
            } else {
                double gigaByte = megaByte / 1024.0D;
                if (gigaByte < 1.0D) {
                    BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
                    return result2.setScale(2, 4).toPlainString() + "MB";
                } else {
                    double teraBytes = gigaByte / 1024.0D;
                    BigDecimal result4;
                    if (teraBytes < 1.0D) {
                        result4 = new BigDecimal(Double.toString(gigaByte));
                        return result4.setScale(2, 4).toPlainString() + "GB";
                    } else {
                        result4 = new BigDecimal(teraBytes);
                        return result4.setScale(2, 4).toPlainString() + "TB";
                    }
                }
            }
        }
    }

    public static void delete(String path) {
        File file = new File(path);
        if (file.exists() && file.delete()) {
            CompressLog.d("the temp file " + path + " has delete success!");
        } else {
            CompressLog.d("the temp file " + path + " was delete failed!");
        }
    }
}
