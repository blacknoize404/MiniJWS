package io.github.blacknoize404.miniJWS.requests;

import io.github.blacknoize404.miniJWS.primitives.HttpMethod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * HttpDecoder:
 * InputStreamReader -> bytes to characters ( decoded with certain Charset ( ascii ) )
 * BufferedReader    -> character stream to text
 */
public class HttpDecoder {

    public static Optional<HttpRequest> decode(final InputStream inputStream) {
        Optional<List<String>> message = readMessage(inputStream);
        System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] Petición decodificada");
        return message.flatMap(HttpDecoder::buildRequest);
    }

    /**
     * Convierte el mensaje recibido en líneas separadas de la petición
     *
     * @param inputStream el flujo de datos de entrada
     * @return dicha lista
     */
    private static Optional<List<String>> readMessage(final InputStream inputStream) {

        System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] Decodificando petición");

        try {
            if (inputStream.available() == 0) {
                System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] Petición vacía");
                return Optional.empty();
            }

            final char[] inBuffer = new char[inputStream.available()];
            final InputStreamReader inReader = new InputStreamReader(inputStream);
            final int read = inReader.read(inBuffer);

            List<String> message = new ArrayList<>();

            try (Scanner sc = new Scanner(new String(inBuffer))) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    message.add(line);
                }
            }

            return Optional.of(message);
        } catch (Exception e) {
            System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] [ERROR] " + e.getLocalizedMessage());
            return Optional.empty();
        }
    }

    /**
     *
     * @param data Una cadena con la estructura de ejemplo ?clave1=valor1&clave2=valor2
     * @return un opcional con las claves y sus valores
     */
    private static Optional<HashMap<String, String>> parseParameters(String data) {

        // TODO: Capturar errores
        if (!data.startsWith("?")) throw new IllegalArgumentException("Los parámetros no tienen el formato apropiado.");

        data = data.substring(1);

        HashMap<String, String> parameters = new HashMap<>();

        Arrays.stream(data.split("&"))
                .forEach(s -> {
                    String[] keyValue = s.split("=");
                    parameters.put(keyValue[0], keyValue[1]);
                });

        if (parameters.isEmpty()) return Optional.empty();
        return Optional.of(parameters);
    }

    private static Optional<HttpRequest> buildRequest(List<String> message) {

        System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] Construyendo petición");
        if (message.isEmpty()) return Optional.empty();

        Iterator<String> messages = message.iterator();

        // Divide la primera petición en sus 3 partes con el formato:
        // TipoPetición Recurso Protocolo
        String[] httpInfo = messages.next().split(" ");

        if (httpInfo.length != 3) return Optional.empty();

        // Procesando la entrada
        try {

            HttpRequest.Builder requestBuilder = new HttpRequest.Builder();

            String httpMethod = httpInfo[0];
            String uri;
            String protocolVersion = httpInfo[2];

            Optional<HashMap<String, String>> parameters = Optional.empty();

            if (httpInfo[1].contains("?")) {

                int indexDivisor = httpInfo[1].indexOf("?");
                uri = httpInfo[1].substring(0, indexDivisor);
                String parametersRaw = httpInfo[1].substring(indexDivisor);

                parameters = parseParameters(parametersRaw);

            }
            else {
                uri = httpInfo[1];
            }

            // Si no es el protocolo HTTP/1.1 no proceso la entrada
            if (!protocolVersion.equals("HTTP/1.1")) return Optional.empty();

            requestBuilder.setHttpMethod(HttpMethod.valueOf(httpMethod));
            requestBuilder.setUri(new URI(uri));
            requestBuilder.setProtocolVersion(protocolVersion);

            // Procesando los cabezales
            Map<String, List<String>> headers = new HashMap<>();

            while (messages.hasNext()) {

                String[] header = messages.next().split(": ");

                if (header.length < 2) break;

                String key = header[0];
                String value = header[1];

                headers.put(key, List.of(value));

            }

            requestBuilder.setRequestHeaders(headers);

            parameters.ifPresent(requestBuilder::setParameters);

            // Procesando el cuerpo
            HttpRequest newRequest = requestBuilder.build();

            System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] Petición construida");
            System.out.println(newRequest);
            return Optional.of(newRequest);
        } catch (URISyntaxException | IllegalArgumentException e) {
            System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] [ERROR] " + e.getLocalizedMessage());
            return Optional.empty();
        }
    }

    private static HttpRequest addRequestHeaders(final List<String> message, final HttpRequest.Builder builder) {
        final Map<String, List<String>> requestHeaders = new HashMap<>();

        if (message.size() > 1) {
            for (int i = 1; i < message.size(); i++) {
                String header = message.get(i);
                int colonIndex = header.indexOf(':');

                if (!(colonIndex > 0 && header.length() > colonIndex + 1)) {
                    break;
                }

                String headerName = header.substring(0, colonIndex);
                String headerValue = header.substring(colonIndex + 1);

                requestHeaders.compute(headerName, (key, values) -> {
                    if (values != null) {
                        values.add(headerValue);
                    } else {
                        values = new ArrayList<>();
                    }
                    return values;
                });
            }
        }

        builder.setRequestHeaders(requestHeaders);
        return builder.build();
    }

}
