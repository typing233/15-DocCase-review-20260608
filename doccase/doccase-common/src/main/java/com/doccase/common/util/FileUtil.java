package com.doccase.common.util;

import java.util.UUID;

public final class FileUtil {

    private FileUtil() {}

    public static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    public static String generateObjectKey(String prefix, String originalFilename) {
        String ext = getExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + "/" + uuid + (ext.isEmpty() ? "" : "." + ext);
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static String getMimeType(String extension) {
        return switch (extension.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "tiff", "tif" -> "image/tiff";
            case "bmp" -> "image/bmp";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            default -> "application/octet-stream";
        };
    }
}
