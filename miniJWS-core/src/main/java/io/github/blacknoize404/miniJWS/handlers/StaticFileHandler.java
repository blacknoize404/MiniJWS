package io.github.blacknoize404.miniJWS.handlers;

import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniJWS.primitives.RequestRunner;
import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class StaticFileHandler implements RequestRunner {

    private final Path baseDir;
    private final String[] indexFiles;

    public StaticFileHandler(String directory) {
        this(directory, "index.html");
    }

    public StaticFileHandler(String directory, String... indexFiles) {
        this.baseDir = new File(directory).toPath().normalize();
        this.indexFiles = indexFiles.length > 0 ? indexFiles : new String[]{"index.html"};
    }

    @Override
    public HttpResponse run(HttpRequest request) {
        String rawPath = request.getUri().getRawPath();

        if (rawPath == null || rawPath.contains("..")) {
            return new HttpResponse.Builder()
                    .setStatusCode(400)
                    .setContentType(ContentType.TEXT)
                    .setBody("400 - Bad Request")
                    .build();
        }

        String relative = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
        Path resolved = baseDir.resolve(relative).normalize();

        if (!resolved.startsWith(baseDir)) {
            return new HttpResponse.Builder()
                    .setStatusCode(403)
                    .setContentType(ContentType.TEXT)
                    .setBody("403 - Forbidden")
                    .build();
        }

        File file = resolved.toFile();

        if (file.isDirectory()) {
            for (String index : indexFiles) {
                File indexFile = new File(file, index);
                if (indexFile.isFile()) {
                    return serveFile(indexFile);
                }
            }
            return new HttpResponse.Builder()
                    .setStatusCode(404)
                    .setContentType(ContentType.TEXT)
                    .setBody("404 - Not Found")
                    .build();
        }

        if (!file.isFile() || !file.exists()) {
            return new HttpResponse.Builder()
                    .setStatusCode(404)
                    .setContentType(ContentType.TEXT)
                    .setBody("404 - Not Found")
                    .build();
        }

        return serveFile(file);
    }

    private HttpResponse serveFile(File file) {
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            String ext = getExtension(file.getName());
            ContentType contentType = ContentType.fromExtension(ext)
                    .orElse(ContentType.FILE);

            return new HttpResponse.Builder()
                    .setStatusCode(200)
                    .setContentType(contentType)
                    .setBody(data)
                    .build();
        } catch (NoSuchFileException e) {
            return new HttpResponse.Builder()
                    .setStatusCode(404)
                    .setContentType(ContentType.TEXT)
                    .setBody("404 - Not Found")
                    .build();
        } catch (IOException e) {
            return new HttpResponse.Builder()
                    .setStatusCode(500)
                    .setContentType(ContentType.TEXT)
                    .setBody("500 - Internal Server Error")
                    .build();
        }
    }

    private static String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot == -1) ? "" : name.substring(dot + 1);
    }
}
