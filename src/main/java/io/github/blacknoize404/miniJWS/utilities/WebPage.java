package io.github.blacknoize404.miniJWS.utilities;

import io.github.blacknoize404.miniJWS.HttpServer;
import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class WebPage implements Runnable {

    Thread thread;
    HttpServer server;
    Path route;

    public static void main(String[] args) throws IOException {

        WebPage wp = new WebPage(80, Path.of("data"));

        wp.start();
        try {
            wp.idle();
        } catch (RuntimeException e) {
            System.out.println(e.getLocalizedMessage());
        }
        wp.stop();

    }

    public WebPage(int port, Path route) throws IOException, IllegalArgumentException {

        Objects.requireNonNull(route);

        thread = new Thread(this);
        thread.setDaemon(true);
        this.server = new HttpServer(port);
        this.route = route;

        File f = route.toFile();
        if (!f.isDirectory()) {
            throw new IllegalArgumentException("No se puede cargar un sitio web desde una ruta que no sea un directorio");
        }

        recorrerArbolDirectorios(f);

    }

    @Override
    public void run() {

        server.run();

    }

    public void start() {

        thread.start();

    }


    public void idle() throws RuntimeException {

        server.idle();
    }


    public void stop() {
        server.stop();
    }

    public void createRoute(File file) {

        assert file != null;

        String path = file.getPath()
                .replaceFirst(route.toString(), "")
                .replaceAll("\\\\", "/");

        int separator = file.getName().lastIndexOf(".");
        String fileFullName = file.getName();
        String fileExtension = fileFullName.substring(separator + 1);


        if (path.equals("/index.html")) {
            path = path.replace("index.html", "");
        } else if (fileFullName.endsWith("index.html")) {
            path = path.replace("index.html", "");
        }

        String finalPath = path;
        server.addRoute(HttpMethod.GET, path, request -> {

            if (!file.exists()) return getErrorResponse();

            try {
                HttpResponse.Builder response = new HttpResponse.Builder();

                byte[] fileBytes = Files.readAllBytes(file.toPath());

                ContentType contentType = ContentType
                        .getTypeOf(fileExtension.toUpperCase())
                        .orElse(ContentType.FILE);

//                if (finalPath.equals("/")) {
//
//
//                    String s = new String(fileBytes, StandardCharsets.UTF_8);
//                    String qrString;
//                    try {
//
//                        qrString = QRCodeGenerator.generateSVG("http://" + GetIp.getIp() + "/enzona.apk", 200);
//                        s = s.replaceAll("\\{\\{enZonaSVG}}", qrString);
//
//                        qrString = QRCodeGenerator.generateSVG("http://" + GetIp.getIp() + "/elsaborcubano.apk", 200);
//                        s = s.replaceAll("\\{\\{elSaborCubanoSVG}}", qrString);
//
//                        qrString = QRCodeGenerator.generateSVG("http://" + GetIp.getIp() + "/ticket.apk", 200);
//                        s = s.replaceAll("\\{\\{ticketSVG}}", qrString);
//                    } catch (WriterException e) {
//                        throw new RuntimeException(e);
//                    }
//
//                    return response
//                            .setContentType(contentType)
//                            .setStatusCode(200)
//                            .setBody(s.getBytes(StandardCharsets.UTF_8))
//                            .build();
//
//                }

                return response
                        .setContentType(contentType)
                        .setStatusCode(200)
                        .setBody(fileBytes)
                        .build();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        if (file.isDirectory()) throw new IllegalArgumentException("Error creando una ruta con un directorio");

    }

    public HttpResponse getErrorResponse() {

        HttpResponse.Builder response = new HttpResponse.Builder();
        return response
                .setContentType(ContentType.TEXT)
                .setStatusCode(404)
                .setBody("No se encuentra el archivo en el servidor")
                .build();
    }

    public void recorrerArbolDirectorios(File f) {

        if (!f.isDirectory()) {
            createRoute(f);
            return;
        }

        File[] files = f.listFiles();

        assert files != null;

        for (File file : files) {
            recorrerArbolDirectorios(file);
        }

    }
}
