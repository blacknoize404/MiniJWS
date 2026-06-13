package io.github.blacknoize404.miniJWS.primitives;

import java.util.Optional;

public enum ContentType {

    FILE("application/octet-stream"),
    JSON("application/json;charset=utf-8"),
    CSS("text/css;charset=utf-8"),
    JS("text/javascript;charset=utf-8"),
    HTML("text/html;charset=utf-8"),
    XML("text/xml;charset=utf-8"),
    TEXT("text/plain;charset=utf-8"),
    SVG("image/svg+xml"),
    ICO("image/x-icon"),
    PNG("image/png"),
    JPEG("image/jpeg"),
    GIF("image/gif"),
    WEBP("image/webp"),
    MP4("video/mp4"),
    WEBM("video/webm"),
    WOFF2("font/woff2"),
    TTF("font/ttf"),
    PDF("application/pdf"),
    ZIP("application/zip");

    private final String mime;

    private static final java.util.Map<String, ContentType> EXT_MAP = new java.util.HashMap<>();

    static {
        for (var type : values()) {
            EXT_MAP.put(type.name().toLowerCase(), type);
        }
        EXT_MAP.put("jpg", JPEG);
        EXT_MAP.put("jpeg", JPEG);
        EXT_MAP.put("tiff", FILE);
    }

    ContentType(String mime) {
        this.mime = mime;
    }

    public static Optional<ContentType> fromExtension(String ext) {
        if (ext == null || ext.isBlank()) return Optional.empty();
        return Optional.ofNullable(EXT_MAP.get(ext.strip().toLowerCase()));
    }

    public static ContentType fromMime(String mime) {
        for (ContentType type : values()) {
            if (type.mime.equals(mime)) return type;
        }
        throw new IllegalArgumentException("Unknown MIME type: " + mime);
    }

    public String mime() {
        return mime;
    }
}
