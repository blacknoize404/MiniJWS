package io.github.blacknoize404.miniJWS.primitives;

import java.util.Map;

public final class HttpStatusCode {

    private HttpStatusCode() {}

    public static final Map<Integer, String> STATUS_CODES = Map.ofEntries(
        Map.entry(100, "CONTINUE"),
        Map.entry(101, "SWITCHING_PROTOCOLS"),
        Map.entry(200, "OK"),
        Map.entry(201, "CREATED"),
        Map.entry(202, "ACCEPTED"),
        Map.entry(203, "NON_AUTHORITATIVE_INFORMATION"),
        Map.entry(204, "NO_CONTENT"),
        Map.entry(206, "PARTIAL_CONTENT"),
        Map.entry(301, "MOVED_PERMANENTLY"),
        Map.entry(302, "FOUND"),
        Map.entry(304, "NOT_MODIFIED"),
        Map.entry(307, "TEMPORARY_REDIRECT"),
        Map.entry(308, "PERMANENT_REDIRECT"),
        Map.entry(400, "BAD_REQUEST"),
        Map.entry(401, "UNAUTHORIZED"),
        Map.entry(403, "FORBIDDEN"),
        Map.entry(404, "NOT_FOUND"),
        Map.entry(405, "METHOD_NOT_ALLOWED"),
        Map.entry(408, "REQUEST_TIMEOUT"),
        Map.entry(413, "PAYLOAD_TOO_LARGE"),
        Map.entry(415, "UNSUPPORTED_MEDIA_TYPE"),
        Map.entry(426, "UPGRADE_REQUIRED"),
        Map.entry(429, "TOO_MANY_REQUESTS"),
        Map.entry(500, "INTERNAL_SERVER_ERROR"),
        Map.entry(502, "BAD_GATEWAY"),
        Map.entry(503, "SERVICE_UNAVAILABLE"),
        Map.entry(504, "GATEWAY_TIMEOUT")
    );

    public static String getMessage(int code) {
        return STATUS_CODES.getOrDefault(code, "UNKNOWN");
    }
}
