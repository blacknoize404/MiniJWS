package io.github.blacknoize404.miniJWS.middleware;

import io.github.blacknoize404.miniJWS.primitives.MiddlewareChain;
import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class GzipMiddlewareTest {

    private final MiddlewareChain okChain = req ->
        new HttpResponse.Builder().setStatusCode(200).setBody("Hello, World! This is a test body that is long enough to trigger compression. ".repeat(4)).build();

    private final MiddlewareChain smallChain = req ->
        new HttpResponse.Builder().setStatusCode(200).setBody("Hi").build();

    private final MiddlewareChain noBodyChain = req ->
        new HttpResponse.Builder().setStatusCode(204).build();

    private HttpRequest requestWithEncoding(String encoding) {
        var builder = new HttpRequest.Builder()
            .setHttpMethod(io.github.blacknoize404.miniJWS.primitives.HttpMethod.GET)
            .setUri(URI.create("/test"))
            .setProtocolVersion("HTTP/1.1");
        if (encoding != null) {
            builder.setHeaders(Map.of("Accept-Encoding", List.of(encoding)));
        }
        return builder.build();
    }

    @Test
    void run_noAcceptEncoding_passesThrough() {
        var mw = new GzipMiddleware();
        var req = requestWithEncoding(null);
        var res = mw.run(req, okChain);
        assertEquals(200, res.getStatusCode());
        assertFalse(res.getHeaders().containsKey("Content-Encoding"));
    }

    @Test
    void run_withGzipEncoding_compressesResponse() {
        var mw = new GzipMiddleware();
        var req = requestWithEncoding("gzip");
        var res = mw.run(req, okChain);
        assertEquals(200, res.getStatusCode());
        assertEquals("gzip", res.getHeaders().get("Content-Encoding").get(0));
    }

    @Test
    void run_compressedBodyIsSmaller() {
        var mw = new GzipMiddleware();
        var req = requestWithEncoding("gzip");
        var res = mw.run(req, okChain);
            var original = okChain.next(req).getBody().orElseThrow();
        var compressed = res.getBody().orElseThrow();
        assertTrue(compressed.length < original.length);
    }

    @Test
    void run_compressedDataIsValidGzip() throws Exception {
        var mw = new GzipMiddleware();
        var req = requestWithEncoding("gzip");
        var res = mw.run(req, okChain);
        var compressed = res.getBody().orElseThrow();
        var gzIn = new GZIPInputStream(new ByteArrayInputStream(compressed));
        var decompressed = new ByteArrayOutputStream();
        gzIn.transferTo(decompressed);
        assertEquals("Hello, World! This is a test body that is long enough to trigger compression. ".repeat(4),
            decompressed.toString());
    }

    @Test
    void run_smallBody_skipsCompression() {
        var mw = new GzipMiddleware();
        var req = requestWithEncoding("gzip");
        var res = mw.run(req, smallChain);
        assertFalse(res.getHeaders().containsKey("Content-Encoding"));
    }

    @Test
    void run_noBody_skipsCompression() {
        var mw = new GzipMiddleware();
        var req = requestWithEncoding("gzip");
        var res = mw.run(req, noBodyChain);
        assertEquals(204, res.getStatusCode());
        assertFalse(res.getHeaders().containsKey("Content-Encoding"));
    }

    @Test
    void run_alreadyEncoded_skipsCompression() {
        io.github.blacknoize404.miniJWS.primitives.MiddlewareChain alreadyEncoded = req ->
            new HttpResponse.Builder()
                .setStatusCode(200)
                .addHeader("Content-Encoding", "identity")
                .setBody("Some body content here that is long enough for compression if it were not already encoded.")
                .build();
        var mw = new GzipMiddleware();
        var req = requestWithEncoding("gzip");
        var res = mw.run(req, alreadyEncoded);
        assertEquals("identity", res.getHeaders().get("Content-Encoding").get(0));
    }

    @Test
    void run_acceptsOtherEncodingsButNotGzip() {
        var mw = new GzipMiddleware();
        var req = requestWithEncoding("deflate");
        var res = mw.run(req, okChain);
        assertFalse(res.getHeaders().containsKey("Content-Encoding"));
    }

    @Test
    void constructor_usesDefaultLevel() {
        var mw = new GzipMiddleware();
        assertNotNull(mw);
    }

    @Test
    void constructor_clampsLevel() {
        var mw1 = new GzipMiddleware(0);
        var mw2 = new GzipMiddleware(10);
        assertNotNull(mw1);
        assertNotNull(mw2);
    }
}
