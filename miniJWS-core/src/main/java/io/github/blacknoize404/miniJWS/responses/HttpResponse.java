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
            headers.put(name, List.of(value));
            return this;
        }

        public Builder addHeader(String name, List<String> values) {
            headers.put(name, values);
            return this;
        }

        public Builder setCookie(String name, String value) {
            return addHeader("Set-Cookie", name + "=" + value);
        }

        public Builder setCookie(String name, String value, int maxAge, String path, boolean httpOnly) {
            var sb = new StringBuilder();
            sb.append(name).append("=").append(value);
            if (maxAge > 0) sb.append("; Max-Age=").append(maxAge);
            if (path != null) sb.append("; Path=").append(path);
            if (httpOnly) sb.append("; HttpOnly");
            return addHeader("Set-Cookie", sb.toString());
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
