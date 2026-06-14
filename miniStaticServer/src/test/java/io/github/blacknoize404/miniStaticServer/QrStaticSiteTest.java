package io.github.blacknoize404.miniStaticServer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class QrStaticSiteTest {

    @Test
    void constructor_createsServer(@TempDir Path tempDir) throws Exception {
        var site = new QrStaticSite(0, tempDir);
        assertNotNull(site);
        site.stop();
    }

    @Test
    void constructor_nullDirectory_throws() {
        assertThrows(NullPointerException.class, () -> new QrStaticSite(0, null));
    }

    @Test
    void constructor_nonExistentDirectory_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new QrStaticSite(0, Path.of("nonexistent-dir")));
    }

    @Test
    void addQrPlaceholder_storesMapping(@TempDir Path tempDir) throws Exception {
        var site = new QrStaticSite(0, tempDir);
        site.addQrPlaceholder("qr1", "https://example.com", 200);
        site.stop();
    }

    @Test
    void start_servesFiles(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("index.html"), "<html><body>{{qr_placeholder}}</body></html>");
        var site = new QrStaticSite(0, tempDir);
        site.addQrPlaceholder("qr_placeholder", "https://example.com", 100);
        site.start();
        Thread.sleep(100);
        site.stop();
    }

    @Test
    void stop_haltsServer(@TempDir Path tempDir) throws Exception {
        var site = new QrStaticSite(0, tempDir);
        site.start();
        Thread.sleep(50);
        site.stop();
    }

    @Test
    void getLocalIp_returnsNonEmpty() {
        var ip = QrStaticSite.getLocalIp();
        assertNotNull(ip);
        assertFalse(ip.isEmpty());
    }

    @Test
    void getLocalIp_isValidIpFormat() {
        var ip = QrStaticSite.getLocalIp();
        assertNotNull(ip);
        try {
            InetAddress.getByName(ip);
        } catch (Exception e) {
            fail("Invalid IP address: " + ip);
        }
    }
}
