package io.github.blacknoize404.miniJWS.responses;

import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniJWS.primitives.HttpStatusCode;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Clase HttpEncoder para escribir la respuesta HTTP en un BufferedWriter
 */
public class HttpEncoder {

    /**
     * Escribe una HTTPResponse a un outputStream
     *
     * @param outputStream    - el flujo de respuesta
     * @param initialResponse - la respuesta como objeto
     */
    public static void sendResponse(final HttpResponse initialResponse, final OutputStream outputStream) {
        try {

            System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] Generando respuesta");
            // Creo una instancia de un escritor en el buffer de salida
            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

            // Encabezados de la respuesta
            final int statusCode = initialResponse.getStatusCode();
            final String statusCodeMeaning = HttpStatusCode.STATUS_CODES.get(statusCode);
            final String protocolVersion = initialResponse.getProtocolVersion();

            writer.write(protocolVersion + " " + statusCode + " " + statusCodeMeaning + "\r\n");

            // Cabezales de la respuesta
            final List<String> responseHeaders = buildHeaderStrings(initialResponse.getHeaders());

            for (String header : responseHeaders) {
                writer.write(header);
            }

            // Cuerpo de la respuesta
            if (initialResponse.getBody().isEmpty()) {
                writer.write("\r\n");
                writer.close();
                return;
            }

            ContentType contentType = ContentType.getByValue(initialResponse.getHeaders().get("Content-Type").getFirst());

            switch (contentType) {

                case JS, CSS, HTML, TEXT -> {

                    final Optional<byte[]> bytes = initialResponse.getBody();
                    final String bodyString = new String(bytes.get(), StandardCharsets.UTF_8);

                    writer.write("Content-Length: " + bodyString.getBytes().length + "\r\n");
                    writer.write("\r\n");
                    writer.write(bodyString);
                }
                default -> {
                    byte[] data = initialResponse.getBody().get();

                    writer.write("Content-Length: " + data.length + "\r\n");
                    writer.write("\r\n");
                    writer.flush();
                    outputStream.write(data);
                    outputStream.flush();

                }
            }

            writer.close();

        } catch (Exception e) {

            System.out.println(e.getLocalizedMessage());
        }
    }

    /**
     * Construye los cabezales en el formato de la respuesta
     *
     * @param responseHeaders el diccionario con los cabezales
     * @return una lista de cadenas formateadas
     */
    private static List<String> buildHeaderStrings(final Map<String, List<String>> responseHeaders) {
        final List<String> responseHeadersList = new ArrayList<>();

        responseHeaders.forEach((key, values) -> {

            final StringBuilder valuesCombined = new StringBuilder();
            values.forEach(valuesCombined::append);
            responseHeadersList.add(key + ": " + valuesCombined + "\r\n");

        });

        return responseHeadersList;
    }

    /**
     * Obtiene la cadena que representa la respuesta al cuerpo de la petición
     *
     * @param entity dicho cuerpo
     * @return dicha cadena
     */
    private static Optional<String> getResponseString(final Object entity) {

//        if (!entity.isEmpty()) return Optional.of(entity);

        // Currently only supporting Strings
        if (entity instanceof String) {
            try {

                final String encodedString = new String(((String) entity).getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

                return Optional.of(encodedString);
            } catch (Exception e) {
//                System.out.println(e.getMessage());
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

