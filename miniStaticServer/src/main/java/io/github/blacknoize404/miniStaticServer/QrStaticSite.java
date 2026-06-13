package io.github.blacknoize404.miniStaticServer;

import com.google.zxing.WriterException;
import io.github.blacknoize404.miniQR.QRCodeGenerator;
import io.github.blacknoize404.miniJWS.HttpServer;
import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class QrStaticSite {

    private final HttpServer server;
    private final Path root;
    private final Map<String, String> qrPlaceholders = new HashMap<>();

    public QrStaticSite(int port, Path rootDirectory) throws IOException {
        Objects.requireNonNull(rootDirectory);
        if (!rootDirectory.toFile().isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + rootDirectory);
        }

        this.root = rootDirectory;
        this.server = new HttpServer(port);
    }

    public QrStaticSite addQrPlaceholder(String placeholder, String url, int qrSize) {
        qrPlaceholders.put(placeholder, url + "|" + qrSize);
        return this;
    }

    public void start() {
        scanDirectory(root.toFile());
        new Thread(server::run).start();
    }

    public void idle() { server.idle(); }
    public void stop() { server.stop(); }

    private void scanDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file);
            } else {
                addFileRoute(file);
            }
        }
    }

    private void addFileRoute(File file) {
        String path = file.getPath()
                .replaceFirst(root.toAbsolutePath().toString(), "")
                .replace("\\", "/");

        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String ext = (dot > 0) ? name.substring(dot + 1) : "";

        if (path.equals("/index.html")) path = "/";
        else if (name.equals("index.html")) {
            path = path.replace("/index.html", "");
            if (path.isEmpty()) path = "/";
        }

        ContentType contentType = ContentType.fromExtension(ext).orElse(ContentType.FILE);
        boolean isHtml = contentType == ContentType.HTML;

        server.addRoute(HttpMethod.GET, path, request -> {
            if (!file.exists()) {
                return new HttpResponse.Builder()
                        .setStatusCode(404)
                        .setContentType(ContentType.TEXT)
                        .setBody("File not found")
                        .build();
            }

            try {
                byte[] data = Files.readAllBytes(file.toPath());

                if (isHtml && !qrPlaceholders.isEmpty()) {
                    String content = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                    for (var entry : qrPlaceholders.entrySet()) {
                        String[] parts = entry.getValue().split("\\|");
                        String targetUrl = parts[0];
                        int size = parts.length > 1 ? Integer.parseInt(parts[1]) : 200;
                        try {
                            String svg = QRCodeGenerator.generateSVG(targetUrl, size);
                            content = content.replace("{{" + entry.getKey() + "}}", svg);
                        } catch (WriterException e) {
                            System.err.println("[QrStaticSite] QR generation failed for " + targetUrl);
                        }
                    }
                    return new HttpResponse.Builder()
                            .setContentType(contentType)
                            .setStatusCode(200)
                            .setBody(content.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                            .build();
                }

                return new HttpResponse.Builder()
                        .setContentType(contentType)
                        .setStatusCode(200)
                        .setBody(data)
                        .build();

            } catch (IOException e) {
                return new HttpResponse.Builder()
                        .setStatusCode(500)
                        .setContentType(ContentType.TEXT)
                        .setBody("Internal server error")
                        .build();
            }
        });
    }

    public static String getLocalIp() {
        try (var socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 12345);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
