package io.github.blacknoize404.miniJWS.responses;

import io.github.blacknoize404.miniJWS.HttpServer;
import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniJWS.primitives.HttpMethod;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Define una respuesta a las peticiones...
 */
public class HttpResponse {

    /**
     * Cabezales de respuesta
     */
    private final Map<String, List<String>> headers;

    /**
     * Estado de respuesta
     */
    private final int statusCode;

    /**
     *
     */
    private final HttpMethod method;

    /**
     * Protocolo
     */
    private final String protocolVersion;

    /**
     * Datos de respuesta
     */
    private final Optional<byte[]> body;

    /**
     * Headers should contain the following:
     * Date: < date >
     * Server: < my server >
     * Content-Type: text/plain, application/json etc...
     * Content-Length: size of payload
     */
    private HttpResponse(final Map<String, List<String>> headers, final int statusCode, HttpMethod method, String protocolVersion, final Optional<byte[]> body) {
        this.headers = headers;
        this.statusCode = statusCode;
        this.method = method;
        this.protocolVersion = protocolVersion;
        this.body = body;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Optional<byte[]> getBody() {
        return body;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public static class Builder {

        private final Map<String, List<String>> headers;

        private int statusCode;

        private String protocolVersion;

        private HttpMethod method;

        private Optional<byte[]> body;

        public Builder() {

            // Crea una respuesta con la versión 1.1 del protocolo http por defecto
            protocolVersion = "HTTP/1.1";

            // Create default headers - server etc
            headers = new HashMap<>();

            method = HttpMethod.GET;

            // Nombre del servidor
            addHeader("Server", HttpServer.serverName);

            // Genero la fecha actual de la respuesta a la petición
            addHeader("Date", formatDate(ZonedDateTime.now(ZoneOffset.UTC)));

            body = Optional.empty();
        }


        private String formatDate(ZonedDateTime dateTime) {

            return dateTime.format(DateTimeFormatter.RFC_1123_DATE_TIME);

        }

        public Builder setStatusCode(final int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder setProtocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Builder setMethod(HttpMethod method) {
            this.method = method;
            return this;
        }

        /**
         * Define el tipo de contenido de respuesta
         * @param type el tipo de contenido definido
         * @return la instancia del Builder
         */
        public Builder setContentType(ContentType type) {
            this.addHeader("Content-Type", type.getType());
            return this;
        }

        /**
         * Añade un encabezado
         * @param name clave del encabezado
         * @param value valor del encabezado
         * @return la instancia del Builder
         */
        public Builder addHeader(final String name, final String value) {
            headers.put(name, List.of(value));
            return this;
        }

        public Builder addHeader(final String name, final List<String> values) {
            headers.put(name, values);
            return this;
        }

        /**
         * Asigna como cuerpo de la respuesta una cadena de texto
         * @param body dicha cadena
         * @return el constructor.
         */
        public Builder setBody(final String body) {
            if (body != null) {

                this.body = Optional.of(body.getBytes(StandardCharsets.UTF_8));
            }
            return this;
        }

        /**
         * Asigna como cuerpo un arreglo de bytes
         * @param body dicho arreglo
         * @return el constructor.
         */
        public Builder setBody(final byte[] body) {
            if (body != null) {
                this.body = Optional.of(body);
            }
            return this;
        }


        public HttpResponse build() {
            return new HttpResponse(headers, statusCode, method, protocolVersion, body);
        }

    }

}