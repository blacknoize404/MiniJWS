package io.github.blacknoize404.miniJWS.handlers;

import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StaticFileHandlerTest {

    private HttpRequest request(HttpMethod method, String path) {
        return new HttpRequest.Builder()
            .setHttpMethod(method)
            .setUri(URI.create(path))
            .setProtocolVersion("HTTP/1.1")
            .build();
    }

    @Test
    void run_servesExistingFile(@TempDir Path tempDir) throws Exception {
        var subdir = tempDir.resolve("sub");
        Files.createDirectories(subdir);
        Files.writeString(subdir.resolve("test.txt"), "hello");
        var handler = new StaticFileHandler(subdir.toString());
        var response = handler.run(request(HttpMethod.GET, "/test.txt"));
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().isPresent());
        assertEquals("hello", new String(response.getBody().get()));
    }

    @Test
    void run_returns404ForNonExistentFile(@TempDir Path tempDir) {
        var handler = new StaticFileHandler(tempDir.toString());
        var response = handler.run(request(HttpMethod.GET, "/no-such-file.txt"));
        assertEquals(404, response.getStatusCode());
    }

    @Test
    void run_returns400ForPathTraversal(@TempDir Path tempDir) {
        var handler = new StaticFileHandler(tempDir.toString());
        var response = handler.run(request(HttpMethod.GET, "/../etc/passwd"));
        assertEquals(400, response.getStatusCode());
    }

    @Test
    void run_blocksPathTraversal(@TempDir Path tempDir) throws Exception {
        var subdir = tempDir.resolve("sub");
        Files.createDirectories(subdir);
        var handler = new StaticFileHandler(subdir.toString());
        var response = handler.run(request(HttpMethod.GET, "/../outside.txt"));
        assertEquals(400, response.getStatusCode());
    }

    @Test
    void run_symlinkPointingOutside_returns403(@TempDir Path tempDir) throws Exception {
        var base = tempDir.resolve("base");
        Files.createDirectories(base);
        var outside = tempDir.resolve("secret.txt");
        Files.writeString(outside, "secret");
        try {
            Files.createSymbolicLink(base.resolve("link.txt"), outside);
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Symlinks no disponibles: " + e);
        }
        var handler = new StaticFileHandler(base.toString());
        var response = handler.run(request(HttpMethod.GET, "/link.txt"));
        assertEquals(403, response.getStatusCode());
    }

    @Test
    void run_oversizedFile_returns413(@TempDir Path tempDir) throws Exception {
        var big = tempDir.resolve("big.bin");
        try (var raf = new java.io.RandomAccessFile(big.toFile(), "rw")) {
            raf.setLength(50L * 1024 * 1024 + 1);
        }
        var handler = new StaticFileHandler(tempDir.toString());
        var response = handler.run(request(HttpMethod.GET, "/big.bin"));
        assertEquals(413, response.getStatusCode());
    }

    @Test
    void run_servesIndexFileForDirectory(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("index.html"), "<h1>Hello</h1>");
        var handler = new StaticFileHandler(tempDir.toString());
        var response = handler.run(request(HttpMethod.GET, "/"));
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().isPresent());
        assertEquals("<h1>Hello</h1>", new String(response.getBody().get()));
    }

    @Test
    void run_returns404ForDirectoryWithoutIndex(@TempDir Path tempDir) {
        var handler = new StaticFileHandler(tempDir.toString());
        var response = handler.run(request(HttpMethod.GET, "/"));
        assertEquals(404, response.getStatusCode());
    }

    @Test
    void run_detectsContentTypeFromExtension(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("style.css"), "body {}");
        var handler = new StaticFileHandler(tempDir.toString());
        var response = handler.run(request(HttpMethod.GET, "/style.css"));
        assertEquals(200, response.getStatusCode());
        var ct = response.getHeaders().get("Content-Type");
        assertNotNull(ct);
        assertTrue(ct.get(0).contains("css"));
    }

    @Test
    void run_usesCustomIndexFiles(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("default.htm"), "default");
        var handler = new StaticFileHandler(tempDir.toString(), "default.htm");
        var response = handler.run(request(HttpMethod.GET, "/"));
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void run_servesFileWithoutExtension(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("data"), "binary content");
        var handler = new StaticFileHandler(tempDir.toString());
        var response = handler.run(request(HttpMethod.GET, "/data"));
        assertEquals(200, response.getStatusCode());
        assertEquals("binary content", new String(response.getBody().orElseThrow()));
    }
}
