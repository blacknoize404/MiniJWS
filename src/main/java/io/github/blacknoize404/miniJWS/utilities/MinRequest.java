package io.github.blacknoize404.miniJWS.utilities;

import io.github.blacknoize404.miniJWS.primitives.HttpMethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * @param method   Método de petición
 * @param uri      Recurso de la petición
 * @param protocol Tipo de protocolo
 * @param headers  Cabezales
 */
public record MinRequest(HttpMethod method, URI uri, String protocol, Map<String, List<String>> headers) {

    public static MinRequest create(InputStream is) throws IOException, URISyntaxException {

        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        ArrayList<String> lines = new ArrayList<>();

        String line;
        while (!(line = in.readLine()).isEmpty()) {
            lines.add(line);
        }

        Iterator<String> messages = lines.iterator();

        // TipoPetición Recurso Protocolo
        String[] httpInfo = messages.next().split(" ");

        assert httpInfo.length == 3;

        HttpMethod method = HttpMethod.valueOf(httpInfo[0]);
        URI uri = new URI(httpInfo[1]);
        String protocol = httpInfo[2];

        // Procesando los cabezales
        Map<String, List<String>> headers = new HashMap<>();
        while (messages.hasNext()) {

            String[] header = messages.next().split(": ");

            assert header.length == 2;

            String key = header[0];
            String value = header[1];

            headers.put(key, List.of(value));

        }

        return new MinRequest(method, uri, protocol, headers);

    }

    @Override
    public String toString() {
        return "MinRequest{" +
                "method=" + method +
                ", uri=" + uri +
                ", protocol='" + protocol + '\'' +
                ", headers=" + headers +
                '}';
    }
}
