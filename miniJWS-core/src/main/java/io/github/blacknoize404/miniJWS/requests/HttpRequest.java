package io.github.blacknoize404.miniJWS.requests;

import io.github.blacknoize404.miniJWS.primitives.HttpMethod;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpRequest {

    private final HttpMethod httpMethod;
    private final URI uri;
    private final String protocolVersion;
    private final Map<String, List<String>> headers;
    private final Map<String, String> parameters;
    private final Map<String, String> cookies;
    private final Optional<byte[]> body;

    private HttpRequest(HttpMethod method, URI uri, String protocolVersion,
                        Map<String, List<String>> headers,
                        Map<String, String> parameters,
                        Optional<byte[]> body) {
        this.httpMethod = method;
        this.uri = uri;
        this.protocolVersion = protocolVersion;
        this.headers = headers;
        this.parameters = parameters;
        this.cookies = parseCookies(headers);
        this.body = body;
    }

    public URI getUri() { return uri; }
    public HttpMethod getHttpMethod() { return httpMethod; }
    public Map<String, List<String>> getHeaders() { return headers; }
    public Map<String, String> getParameters() { return parameters; }
    public Map<String, String> getCookies() { return cookies; }
    public String getProtocolVersion() { return protocolVersion; }
    public Optional<byte[]> getBody() { return body; }

    public Optional<String> getHeader(String name) {
        var values = headers.get(name);
        if (values == null || values.isEmpty()) return Optional.empty();
        return Optional.of(String.join(", ", values));
    }

    private static Map<String, String> parseCookies(Map<String, List<String>> headers) {
        var values = headers.get("Cookie");
        if (values == null || values.isEmpty()) return Map.of();

        Map<String, String> cookies = new LinkedHashMap<>();
        for (String value : values) {
            for (String pair : value.split(";")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    cookies.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
        return cookies;
    }

    public Optional<String> bodyAsString() {
        return body.map(b -> new String(b, StandardCharsets.UTF_8));
    }

    public Optional<Map<String, String>> bodyAsForm() {
        return bodyAsString().map(this::parseFormBody);
    }

    public Optional<Map<String, String>> bodyAsJson() {
        return bodyAsString().map(this::parseFlatJson);
    }

    private Map<String, String> parseFormBody(String data) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : data.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                map.put(key, value);
            }
        }
        return map;
    }

    private Map<String, String> parseFlatJson(String data) {
        Map<String, String> map = new LinkedHashMap<>();
        data = data.trim();
        if (data.startsWith("{") && data.endsWith("}")) {
            data = data.substring(1, data.length() - 1).trim();
        }
        int depth = 0;
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean inKey = true;
        boolean inString = false;
        char quote = 0;

        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);

            if (inString) {
                if (c == '\\' && i + 1 < data.length()) {
                    i++;
                    c = data.charAt(i);
                } else if (c == quote) {
                    inString = false;
                    continue;
                }
                (inKey ? key : value).append(c);
                continue;
            }

            if (c == '"' || c == '\'') {
                inString = true;
                quote = c;
                continue;
            }

            if (c == '{' || c == '[') { depth++; continue; }
            if (c == '}' || c == ']') { depth--; continue; }

            if (depth == 0) {
                if (c == ':') { inKey = false; continue; }
                if (c == ',' || c == ';') {
                    addJsonEntry(map, key.toString().trim(), value.toString().trim());
                    key.setLength(0);
                    value.setLength(0);
                    inKey = true;
                    continue;
                }
            }
            (inKey ? key : value).append(c);
        }
        addJsonEntry(map, key.toString().trim(), value.toString().trim());
        return map;
    }

    private void addJsonEntry(Map<String, String> map, String key, String value) {
        if (key.isEmpty()) return;
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }
        map.put(key, value);
    }

    public static class Builder {
        private HttpMethod httpMethod;
        private URI uri;
        private String protocolVersion;
        private Map<String, List<String>> headers = new HashMap<>();
        private Map<String, String> parameters = new HashMap<>();
        private Optional<byte[]> body = Optional.empty();

        public Builder setHttpMethod(HttpMethod method) { this.httpMethod = method; return this; }
        public Builder setUri(URI uri) { this.uri = uri; return this; }
        public Builder setProtocolVersion(String version) { this.protocolVersion = version; return this; }
        public Builder setHeaders(Map<String, List<String>> headers) { this.headers = headers; return this; }
        public Builder addHeader(String key, List<String> value) { headers.put(key, value); return this; }
        public Builder setParameters(Map<String, String> params) { this.parameters = params; return this; }
        public Builder addParameter(String key, String value) { parameters.put(key, value); return this; }
        public Builder setBody(byte[] data) { this.body = Optional.ofNullable(data); return this; }

        public HttpRequest build() {
            Objects.requireNonNull(httpMethod, "HTTP method must not be null");
            Objects.requireNonNull(uri, "URI must not be null");
            return new HttpRequest(httpMethod, uri, protocolVersion, headers, parameters, body);
        }
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "method=" + httpMethod +
                ", uri=" + uri +
                ", protocol='" + protocolVersion + '\'' +
                ", headers=" + headers +
                ", params=" + parameters +
                ", body=" + body.map(b -> "<" + b.length + " bytes>").orElse("empty") +
                '}';
    }
}
