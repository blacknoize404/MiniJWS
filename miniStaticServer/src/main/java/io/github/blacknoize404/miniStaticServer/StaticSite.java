package io.github.blacknoize404.miniStaticServer;

import io.github.blacknoize404.miniJWS.HttpServer;
import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class StaticSite {

    private final HttpServer server;
    private final Path root;
    private final Map<String, String> templates = new HashMap<>();

    public StaticSite(int port, Path rootDirectory) throws IOException {
        Objects.requireNonNull(rootDirectory, "Root directory must not be null");
        if (!rootDirectory.toFile().isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + rootDirectory);
        }

        this.root = rootDirectory;
        this.server = new HttpServer(port);
        scanDirectory(rootDirectory.toFile());
    }

    public void addTemplate(String key, String value) {
        templates.put(key, value);
    }

    public void start() {
        new Thread(server::run).start();
    }

    public void idle() { server.idle(); }
    public void stop() { server.stop(); }
    public HttpServer getServer() { return server; }

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

        if (path.equals("/index.html")) {
            path = "/";
        } else if (name.equals("index.html")) {
            path = path.replace("/index.html", "");
            if (path.isEmpty()) path = "/";
        }

        ContentType contentType = ContentType.fromExtension(ext).orElse(ContentType.FILE);
        boolean processTemplates = contentType == ContentType.HTML && !templates.isEmpty();

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

                if (processTemplates) {
                    String content = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                    for (var entry : templates.entrySet()) {
                        content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
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

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: StaticSite <port> <root-directory>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        Path root = Path.of(args[1]);

        StaticSite site = new StaticSite(port, root);
        site.start();
        System.out.println("[StaticSite] Serving " + root + " on port " + port);
        site.idle();
    }
}
