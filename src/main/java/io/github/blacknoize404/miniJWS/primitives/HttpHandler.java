package io.github.blacknoize404.miniJWS.primitives;

import io.github.blacknoize404.miniJWS.requests.HttpDecoder;
import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpEncoder;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * Maneja el ciclo de vida de la petición de respuesta.
 */
public class HttpHandler {

    private final Map<String, RequestRunner> routes;

    public HttpHandler(final Map<String, RequestRunner> routes) {
        this.routes = routes;
    }

    public void handleConnection(final InputStream inputStream, final OutputStream outputStream) throws IOException {

        // Decodifico la petición
        Optional<HttpRequest> request = HttpDecoder.decode(inputStream);

        // Proceso la petición
        request.ifPresentOrElse(httpRequest -> {
            handleValidRequest(httpRequest, outputStream);
        }, () -> {
            handleInvalidRequest(outputStream);
        });
        System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] Respuesta enviada");


        outputStream.close();
        inputStream.close();
    }

    /**
     * Manejo la petición y devuelvo una respuesta acorde al tipo de petición y la ruta
     *
     * @param request petición
     */
    private void handleValidRequest(final HttpRequest request, final OutputStream outputStream) {

        // System.out.println(routes);
        System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] Leyendo petición válida");

        String uri = request.getUri().getRawPath();


        if (!uri.equals("/") && uri.endsWith("/")) uri = uri.substring(0, uri.length() - 1);

        final String routeKey = request.getHttpMethod().name() + uri;

        // Si la petición no se encuentra en las rutas
        if (!routes.containsKey(routeKey)) {
            System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] Ruta no encontrada, generando respuesta 404");
            HttpEncoder.sendResponse(
                    new HttpResponse.Builder()
                            .setStatusCode(404)
                            .setContentType(ContentType.TEXT)
                            .setBody("Route Not Found...")
                            .build(),
                    outputStream);

            return;
        }
        // Si en las rutas se encuentra la clave de la petición, emite una respuesta acorde a dicha ruta.
        HttpResponse response = routes.get(routeKey).run(request);
        HttpEncoder.sendResponse(response, outputStream);

    }

    private void handleInvalidRequest(final OutputStream outputStream) {

        System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] Leyendo petición inválida");
        HttpResponse notFoundResponse = new HttpResponse
                .Builder()
                .setStatusCode(400)
                .setContentType(ContentType.TEXT)
                .setBody("Bad Request...")
                .build();
        HttpEncoder.sendResponse(notFoundResponse, outputStream);

    }
}