package com.zj.compress;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
public class Constance {

    public static String getIMEType(String fileName) {
        String type = "*/*";
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
        String end = fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
        if (TextUtils.isEmpty(end)) {
            return type;
        }
        for (Map.Entry<String, String> entry : getMimeTable().entrySet()) {
            if (end.equals(entry.getKey())) {
                type = entry.getValue();
                break;
            }
        }
        return type;
    }

    static Map<String, String> mimeTable;
    static Map<String, String> specialTransferMimeTable;

    public static String transformSpecialToNormal(String fileName) {
        String replaced = fileName;
        for (Map.Entry<String, String> entry : getSpecialTransferMimeTable().entrySet()) {
            if (replaced.contains(entry.getKey())) {
                replaced = fileName.replace(entry.getKey(), entry.getValue());
                break;
            }
        }
        return replaced;
    }

    public static Map<String, String> getSpecialTransferMimeTable() {
        if (specialTransferMimeTable == null) {
            specialTransferMimeTable = new HashMap<>();
            specialTransferMimeTable.put(apkVnSuffix, ".apk");
            specialTransferMimeTable.put(docSuffix, ".doc");
            specialTransferMimeTable.put(docxSuffix, ".docx");
            specialTransferMimeTable.put(xlsSuffix, ".xls");
            specialTransferMimeTable.put(m4uVnSuffix, ".m4u");
            specialTransferMimeTable.put(xlsxSuffix, ".xlsx");
            specialTransferMimeTable.put(ppsVnSuffix, ".pps");
            specialTransferMimeTable.put(pptVnSuffix, ".ppt");
            specialTransferMimeTable.put(pptxVnSuffix, ".pptx");
            specialTransferMimeTable.put(wpsVnSuffix, ".wps");
            specialTransferMimeTable.put(mpcVnSuffix, ".mpc");
            specialTransferMimeTable.put(msgVnSuffix, ".msg");
        }
        return specialTransferMimeTable;
    }

    public static Map<String, String> getMimeTable() {
        if (mimeTable == null) {
            mimeTable = new HashMap<>();
            mimeTable.put(".3gp", "video/3gpp");
            mimeTable.put(".apk", "application/" + apkVnSuffix);
            mimeTable.put(".asf", "video/x-ms-asf");
            mimeTable.put(".avi", "video/x-msvideo");
            mimeTable.put(".bin", "application/octet-stream");
            mimeTable.put(".bmp", "image/bmp");
            mimeTable.put(".c", "text/plain");
            mimeTable.put(".class", "application/octet-stream");
            mimeTable.put(".conf", "text/plain");
            mimeTable.put(".cpp", "text/plain");
            mimeTable.put(".doc", "application/" + docSuffix);
            mimeTable.put(".docx", "application/" + docxSuffix);
            mimeTable.put(".xls", "application/" + xlsSuffix);
            mimeTable.put(".xlsx", "application/" + xlsxSuffix);
            mimeTable.put(".exe", "application/octet-stream");
            mimeTable.put(".gif", "image/gif");
            mimeTable.put(".gtar", "application/x-gtar");
            mimeTable.put(".gz", "application/x-gzip");
            mimeTable.put(".h", "text/plain");
            mimeTable.put(".htm", "text/html");
            mimeTable.put(".html", "text/html");
            mimeTable.put(".jar", "application/java-archive");
            mimeTable.put(".java", "text/plain");
            mimeTable.put(".jpeg", "image/jpeg");
            mimeTable.put(".jpg", "image/jpeg");
            mimeTable.put(".js", "application/x-javascript");
            mimeTable.put(".log", "text/plain");
            mimeTable.put(".m3u", "audio/x-mpegurl");
            mimeTable.put(".m4a", "audio/mp4a-latm");
            mimeTable.put(".m4b", "audio/mp4a-latm");
            mimeTable.put(".m4p", "audio/mp4a-latm");
            mimeTable.put(".m4u", "video/" + m4uVnSuffix);
            mimeTable.put(".m4v", "video/x-m4v");
            mimeTable.put(".mov", "video/quicktime");
            mimeTable.put(".mp2", "audio/x-mpeg");
            mimeTable.put(".mp3", "audio/x-mpeg");
            mimeTable.put(".mp4", "video/mp4");
            mimeTable.put(".mpc", "application/" + mpcVnSuffix);
            mimeTable.put(".mpe", "video/mpeg");
            mimeTable.put(".mpeg", "video/mpeg");
            mimeTable.put(".mpg", "video/mpeg");
            mimeTable.put(".mpg4", "video/mp4");
            mimeTable.put(".mpga", "audio/mpeg");
            mimeTable.put(".msg", "application/" + msgVnSuffix);
            mimeTable.put(".ogg", "audio/ogg");
            mimeTable.put(".pdf", "application/pdf");
            mimeTable.put(".png", "image/png");
            mimeTable.put(".pps", "application/" + ppsVnSuffix);
            mimeTable.put(".ppt", "application/" + pptVnSuffix);
            mimeTable.put(".pptx", "application/" + pptxVnSuffix);
            mimeTable.put(".prop", "text/plain");
            mimeTable.put(".rc", "text/plain");
            mimeTable.put(".rmvb", "audio/x-pn-realaudio");
            mimeTable.put(".rtf", "application/rtf");
            mimeTable.put(".sh", "text/plain");
            mimeTable.put(".tar", "application/x-tar");
            mimeTable.put(".tgz", "application/x-compressed");
            mimeTable.put(".txt", "text/plain");
            mimeTable.put(".wav", "audio/x-wav");
            mimeTable.put(".wma", "audio/x-ms-wma");
            mimeTable.put(".wmv", "audio/x-ms-wmv");
            mimeTable.put(".wps", "application/" + wpsVnSuffix);
            mimeTable.put(".xml", "text/plain");
            mimeTable.put(".z", "application/x-compress");
            mimeTable.put(".zip", "application/x-zip-compressed");
            mimeTable.put("", "*/*");
        }
        return mimeTable;
    }

    public static String msgVnSuffix = "vnd.ms-outlook";
    public static String m4uVnSuffix = "vnd.mpegurl";
    public static String mpcVnSuffix = "vnd.mpohun.certificate";
    public static String wpsVnSuffix = "vnd.ms-works";
    public static String ppsVnSuffix = "vnd.ms-powerpoint";
    public static String pptVnSuffix = "vnd.ms-powerpoint";
    public static String apkVnSuffix = "vnd.android.package-archive";
    public static String pptxVnSuffix = "vnd.openxmlformats-officedocument.presentationml.presentation";
    public static String docSuffix = "msword";
    public static String docxSuffix = "vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static String xlsSuffix = "vnd.ms-excel";
    public static String xlsxSuffix = "vnd.openxmlformats-officedocument.spreadsheetml.sheet";

}
