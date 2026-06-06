package io.github.blacknoize404.miniJWS.requests;

import io.github.blacknoize404.miniJWS.primitives.HttpMethod;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequest {

    /**
     * Método de petición
     */
    private final HttpMethod httpMethod;

    /**
     * Recurso de la petición
     */
    private final URI uri;


    /**
     * Tipo de protocolo
     */
    private final String protocolVersion;

    /**
     * Cabezales
     */
    private final Map<String, List<String>> headers;

    /**
     * Parámetros
     */
    private final Map<String, String> parameters;

    private HttpRequest(HttpMethod opCode,
                        URI uri, String protocolVersion,
                        Map<String, List<String>> requestHeaders,
                        Map<String, String> parameters) {

        this.httpMethod = opCode;
        this.uri = uri;
        this.protocolVersion = protocolVersion;
        this.headers = requestHeaders;
        this.parameters = parameters;

    }

    public URI getUri() {
        return uri;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public static class Builder {
        private HttpMethod httpMethod;
        private URI uri;
        private String protocolVersion;
        private Map<String, List<String>> requestHeaders;
        private Map<String, String> parameters;

        public Builder() {
            parameters = new HashMap<>();
            requestHeaders = new HashMap<>();
        }

        public void setHttpMethod(HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
        }

        public void setUri(URI uri) {
            this.uri = uri;
        }

        public void setProtocolVersion(String protocol) {
            this.protocolVersion = protocol;
        }

        public void setRequestHeaders(Map<String, List<String>> requestHeaders) {
            this.requestHeaders = requestHeaders;
        }

        public void addRequestHeader(String key, List<String> value) {
            requestHeaders.put(key, value);
        }

        public void setParameters(Map<String, String> parameters) {
            this.parameters = parameters;
        }

        public void addParameter(String key, String value) {
            this.parameters.put(key, value);
        }

        public HttpRequest build() {
            return new HttpRequest(httpMethod, uri, protocolVersion, requestHeaders, parameters);
        }
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "httpMethod=" + httpMethod +
                ", uri='" + uri + '\'' +
                ", protocol='" + protocolVersion + '\'' +
                ", headers=" + headers +
                ", parameters=" + parameters +
                '}';
    }
}