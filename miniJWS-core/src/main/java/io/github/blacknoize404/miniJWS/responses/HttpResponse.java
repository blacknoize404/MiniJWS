package io.github.blacknoize404.miniJWS.responses;

import io.github.blacknoize404.miniJWS.HttpServer;
import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniJWS.primitives.HttpMethod;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HttpResponse {

    private final Map<String, List<String>> headers;
    private final int statusCode;
    private final HttpMethod method;
    private final String protocolVersion;
    private final Optional<byte[]> body;

    private HttpResponse(Map<String, List<String>> headers, int statusCode,
                         HttpMethod method, String protocolVersion, Optional<byte[]> body) {
        this.headers = headers;
        this.statusCode = statusCode;
        this.method = method;
        this.protocolVersion = protocolVersion;
        this.body = body;
    }

    public Map<String, List<String>> getHeaders() { return headers; }
    public int getStatusCode() { return statusCode; }
    public HttpMethod getMethod() { return method; }
    public String getProtocolVersion() { return protocolVersion; }
    public Optional<byte[]> getBody() { return body; }

    public static HttpResponse redirect(String location) {
        return redirect(location, 302);
    }

    public static HttpResponse redirect(String location, int statusCode) {
        return new HttpResponse.Builder()
                .setStatusCode(statusCode)
                .addHeader("Location", location)
                .build();
    }

    public static class Builder {
        private final Map<String, List<String>> headers = new LinkedHashMap<>();
        private int statusCode = 200;
        private String protocolVersion = "HTTP/1.1";
        private HttpMethod method = HttpMethod.GET;
        private Optional<byte[]> body = Optional.empty();

        public Builder() {
            addHeader("Server", HttpServer.SERVER_NAME);
            addHeader("Date", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME));
        }

        public Builder setStatusCode(int code) { this.statusCode = code; return this; }
        public Builder setProtocolVersion(String v) { this.protocolVersion = v; return this; }
        public Builder setMethod(HttpMethod m) { this.method = m; return this; }

        public Builder setContentType(ContentType type) {
            return addHeader("Content-Type", type.mime());
        }

        public Builder setContentType(String mime) {
            return addHeader("Content-Type", mime);
        }

        public Builder addHeader(String name, String value) {
            validateHeader(name, value);
            headers.put(name, List.of(value));
            return this;
        }

        public Builder addHeader(String name, List<String> values) {
            Objects.requireNonNull(values, "Header values must not be null");
            values.forEach(v -> validateHeader(name, v));
            headers.put(name, List.copyOf(values));
            return this;
        }

        private static void validateHeader(String name, String value) {
            if (name == null || name.isBlank() || name.contains(":")
                    || name.indexOf('\r') >= 0 || name.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("Invalid header name");
            }
            if (value == null || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("Invalid header value for " + name);
            }
        }

        private static void validateCookiePart(String part, String what) {
            if (part == null || part.indexOf('\r') >= 0 || part.indexOf('\n') >= 0
                    || part.indexOf(';') >= 0) {
                throw new IllegalArgumentException("Invalid cookie " + what);
            }
        }

        public Builder setCookie(String name, String value) {
            validateCookiePart(name, "name");
            if (name.contains("=")) throw new IllegalArgumentException("Invalid cookie name");
            validateCookiePart(value, "value");
            return appendSetCookie(name + "=" + value);
        }

        public Builder setCookie(String name, String value, int maxAge, String path, boolean httpOnly) {
            return setCookie(name, value, maxAge, path, httpOnly, false, null);
        }

        public Builder setCookie(String name, String value, int maxAge, String path,
                                  boolean httpOnly, boolean secure, String sameSite) {
            validateCookiePart(name, "name");
            if (name.contains("=")) throw new IllegalArgumentException("Invalid cookie name");
            validateCookiePart(value, "value");
            if (path != null) validateCookiePart(path, "path");
            if (sameSite != null) {
                if (!sameSite.equalsIgnoreCase("Lax")
                        && !sameSite.equalsIgnoreCase("Strict")
                        && !sameSite.equalsIgnoreCase("None")) {
                    throw new IllegalArgumentException("Invalid SameSite value: " + sameSite);
                }
                if (sameSite.equalsIgnoreCase("None") && !secure) {
                    throw new IllegalArgumentException("SameSite=None requires Secure");
                }
            }

            var sb = new StringBuilder();
            sb.append(name).append("=").append(value);
            if (maxAge > 0) sb.append("; Max-Age=").append(maxAge);
            if (path != null) sb.append("; Path=").append(path);
            if (httpOnly) sb.append("; HttpOnly");
            if (secure) sb.append("; Secure");
            if (sameSite != null) sb.append("; SameSite=").append(sameSite);
            return appendSetCookie(sb.toString());
        }

        private Builder appendSetCookie(String cookie) {
            List<String> cookies = new ArrayList<>(headers.getOrDefault("Set-Cookie", List.of()));
            cookies.add(cookie);
            headers.put("Set-Cookie", cookies);
            return this;
        }

        public Builder setBody(String text) {
            this.body = (text != null)
                    ? Optional.of(text.getBytes(StandardCharsets.UTF_8))
                    : Optional.empty();
            return this;
        }

        public Builder setBody(byte[] data) {
            this.body = Optional.ofNullable(data);
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(headers, statusCode, method, protocolVersion, body);
        }
    }
}
