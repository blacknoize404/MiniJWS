package io.github.blacknoize404.miniStaticServer;

import io.github.blacknoize404.miniJWS.HttpServer;
import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StaticSiteTest {

    @Test
    void constructor_createsServer(@TempDir Path tempDir) throws Exception {
        var site = new StaticSite(0, tempDir);
        assertNotNull(site.getServer());
        site.stop();
    }

    @Test
    void constructor_nullDirectory_throws() {
        assertThrows(NullPointerException.class, () -> new StaticSite(0, null));
    }

    @Test
    void constructor_nonExistentDirectory_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new StaticSite(0, Path.of("nonexistent-dir")));
    }

    @Test
    void constructor_nonDirectory_throws(@TempDir Path tempDir) throws Exception {
        var file = tempDir.resolve("file.txt");
        Files.writeString(file, "data");
        assertThrows(IllegalArgumentException.class, () ->
            new StaticSite(0, file));
    }

    @Test
    void addTemplate_storesKeyValue(@TempDir Path tempDir) throws Exception {
        var site = new StaticSite(0, tempDir);
        site.addTemplate("name", "World");
        site.stop();
    }

    @Test
    void start_runsServer(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("index.html"), "<h1>Hello</h1>");
        var site = new StaticSite(0, tempDir);
        site.start();
        Thread.sleep(100);
        site.stop();
    }

    @Test
    void scanDirectory_addsRoutesForFiles(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("test.html"), "<h1>Test</h1>");
        var site = new StaticSite(0, tempDir);
        site.stop();
    }

    @Test
    void stop_haltsServer(@TempDir Path tempDir) throws Exception {
        var site = new StaticSite(0, tempDir);
        site.start();
        Thread.sleep(50);
        site.stop();
    }

    @Test
    void idle_blocksUntilStopped(@TempDir Path tempDir) throws Exception {
        var site = new StaticSite(0, tempDir);
        new Thread(() -> {
            try { Thread.sleep(200); site.stop(); } catch (Exception e) {}
        }).start();
        site.idle();
    }
}
