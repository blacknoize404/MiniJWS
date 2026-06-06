package io.github.blacknoize404.miniJWS.utilities;

import com.sun.net.httpserver.SimpleFileServer;
import io.github.blacknoize404.miniJWS.HttpServer;
import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import io.github.blacknoize404.miniJWS.primitives.RequestRunner;
import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    private static HttpServer hs;

    public static void main(String[] args) throws IOException {

        hs = new HttpServer(80);

        hs.addRoute(HttpMethod.GET, "/Eskos.mp4", new RequestRunner() {
            @Override
            public HttpResponse run(HttpRequest request) {

                HttpResponse.Builder response = new HttpResponse.Builder();

                File file = new File("data/Eskos.mp4");

                if (!file.exists()) {
                    return response
                            .setContentType(ContentType.TEXT)
                            .setStatusCode(404)
                            .setBody("No se encuentra el archivo en el servidor")
                            .build();
                }

                byte[] fileBytes = null;

                try {
                    fileBytes = Files.readAllBytes(file.toPath());
                } catch (IOException e) {
                    System.out.println(e.getLocalizedMessage());
                }

                return response
                        .setContentType(ContentType.MP4)
                        .setStatusCode(200)
                        .setBody(fileBytes)
                        .build();
            }
        });

        hs.addRoute(HttpMethod.GET, "/", new RequestRunner() {
            @Override
            public HttpResponse run(HttpRequest request) {
                HttpResponse.Builder response = new HttpResponse.Builder();

                File file = new File("data/index.html");
                if (!file.exists()) {

                    return response
                            .setContentType(ContentType.TEXT)
                            .setStatusCode(404)
                            .setBody("No se encuentra el archivo en el servidor")
                            .build();
                }

                try {
                    String data = Files.readString(file.toPath(), StandardCharsets.UTF_8);

                    response.setContentType(ContentType.HTML)
                            .setStatusCode(200)
                            .setBody(data);

                    return response.build();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        });

        hs.addRoute(HttpMethod.GET, "/style.css", new RequestRunner() {
            @Override
            public HttpResponse run(HttpRequest request) {
                HttpResponse.Builder response = new HttpResponse.Builder();

                File file = new File("data/style.css");
                if (!file.exists()) {

                    return response
                            .setContentType(ContentType.TEXT)
                            .setStatusCode(404)
                            .setBody("No se encuentra el archivo en el servidor")
                            .build();
                }

                try {
                    String data = Files.readString(file.toPath(), StandardCharsets.UTF_8);

                    response.setContentType(ContentType.CSS)
                            .setStatusCode(200)
                            .setBody(data);

                    return response.build();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        });

        hs.run();

    }

    public static void runFileServerAt(String location, int port) {

        var rootDirectory = Path.of(location);
        var outputLevel = SimpleFileServer.OutputLevel.VERBOSE;
        com.sun.net.httpserver.HttpServer server = SimpleFileServer.createFileServer(
                new InetSocketAddress(port),
                rootDirectory,
                outputLevel
        );

        server.start();

    }
}