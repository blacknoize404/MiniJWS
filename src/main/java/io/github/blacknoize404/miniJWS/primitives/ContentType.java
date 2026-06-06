package io.github.blacknoize404.miniJWS.primitives;

import java.util.Optional;

public enum ContentType {

    // Application
    FILE("application/octet-stream"),
    JSON("application/json;charset=utf-8"),

    // Text
    CSS("text/css;charset=utf-8"),
    JS("text/javascript;charset=utf-8"),
    HTML("text/html;charset=utf-8"),
    XML("text/xml;charset=utf-8"),
    TEXT("text/plain;charset=utf-8"),

    // Image
    SVG("image/svg+xml"),
    PNG("image/png"),

    // Video
    MP4("video/mp4");

    private final String type;

    ContentType(String type) {
        this.type = type;
    }

    public static Optional<ContentType> getTypeOf(String value) {
        for (ContentType type: ContentType.values()) {

            if (type.name().startsWith(value)) return Optional.of(type);

        }
        return Optional.empty();
    }

    public static ContentType getByValue(String value) {

        for (ContentType type: ContentType.values()) {

            if (type.getType().equals(value)) {
                return type;
            }

        }

        throw new IllegalArgumentException("No se encontró la constante cuyo valor es " + value);

    }

    public String getType() {
        return type;
    }
}
