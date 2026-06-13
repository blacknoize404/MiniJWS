package io.github.blacknoize404.miniJWS.content;

import java.util.Map;

public final class ContentTypes {

    private ContentTypes() {}

    public static final Map<String, String> EXTENSION_TO_MIME = Map.ofEntries(
        Map.entry("html", "text/html;charset=utf-8"),
        Map.entry("css", "text/css;charset=utf-8"),
        Map.entry("js", "text/javascript;charset=utf-8"),
        Map.entry("json", "application/json;charset=utf-8"),
        Map.entry("xml", "text/xml;charset=utf-8"),
        Map.entry("txt", "text/plain;charset=utf-8"),
        Map.entry("svg", "image/svg+xml"),
        Map.entry("ico", "image/x-icon"),
        Map.entry("png", "image/png"),
        Map.entry("jpg", "image/jpeg"),
        Map.entry("jpeg", "image/jpeg"),
        Map.entry("gif", "image/gif"),
        Map.entry("webp", "image/webp"),
        Map.entry("mp4", "video/mp4"),
        Map.entry("webm", "video/webm"),
        Map.entry("woff2", "font/woff2"),
        Map.entry("ttf", "font/ttf"),
        Map.entry("pdf", "application/pdf"),
        Map.entry("zip", "application/zip"),
        Map.entry("apk", "application/vnd.android.package-archive")
    );

    public static String forExtension(String ext) {
        if (ext == null) return "application/octet-stream";
        return EXTENSION_TO_MIME.getOrDefault(ext.toLowerCase(), "application/octet-stream");
    }
}
