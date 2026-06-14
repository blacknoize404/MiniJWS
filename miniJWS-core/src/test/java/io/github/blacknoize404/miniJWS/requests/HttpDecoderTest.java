package io.github.blacknoize404.miniJWS.requests;

import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpDecoderTest {

    private static InputStream toStream(String data) {
        return new ByteArrayInputStream(data.getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void decode_simpleGetRequest() {
        var raw = "GET /index.html HTTP/1.1\r\nHost: localhost\r\n\r\n";
        var result = HttpDecoder.decode(toStream(raw));
        assertTrue(result.isPresent());
        var req = result.get();
        assertEquals(HttpMethod.GET, req.getHttpMethod());
        assertEquals("/index.html", req.getUri().getRawPath());
        assertEquals("HTTP/1.1", req.getProtocolVersion());
    }

    @Test
    void decode_withHeaders() {
        var raw = "GET / HTTP/1.1\r\nHost: example.com\r\nUser-Agent: curl/8.0\r\n\r\n";
        var result = HttpDecoder.decode(toStream(raw));
        assertTrue(result.isPresent());
        var req = result.get();
        assertEquals("example.com", req.getHeader("Host").orElse(""));
        assertEquals("curl/8.0", req.getHeader("User-Agent").orElse(""));
    }

    @Test
    void decode_withQueryParams() {
        var raw = "GET /search?q=hello&page=1 HTTP/1.1\r\nHost: local\r\n\r\n";
        var result = HttpDecoder.decode(toStream(raw));
        assertTrue(result.isPresent());
        var req = result.get();
        assertEquals("hello", req.getParameters().get("q"));
        assertEquals("1", req.getParameters().get("page"));
    }

    @Test
    void decode_withBody() {
        var raw = "POST /api/data HTTP/1.1\r\nHost: local\r\nContent-Length: 5\r\n\r\nHello";
        var result = HttpDecoder.decode(toStream(raw));
        assertTrue(result.isPresent());
        var req = result.get();
        assertEquals(HttpMethod.POST, req.getHttpMethod());
        assertTrue(req.getBody().isPresent());
        assertArrayEquals("Hello".getBytes(StandardCharsets.UTF_8), req.getBody().get());
    }

    @Test
    void decode_emptyInput_returnsEmpty() {
        var raw = "\r\n";
        var result = HttpDecoder.decode(toStream(raw));
        assertFalse(result.isPresent());
    }

    @Test
    void decode_nullInputStream_returnsEmpty() {
        var result = HttpDecoder.decode(new BufferedInputStream(new ByteArrayInputStream(new byte[0])));
        assertFalse(result.isPresent());
    }

    @Test
    void decode_invalidMethod_returnsEmpty() {
        var raw = "INVALID / HTTP/1.1\r\nHost: local\r\n\r\n";
        assertFalse(HttpDecoder.decode(toStream(raw)).isPresent());
    }

    @Test
    void decode_invalidRequestLine_returnsEmpty() {
        var raw = "GET\r\n\r\n";
        assertFalse(HttpDecoder.decode(toStream(raw)).isPresent());
    }

    @Test
    void decode_multipleContentLength_returnsEmpty() {
        var raw = "POST / HTTP/1.1\r\nHost: local\r\nContent-Length: 5\r\nContent-Length: 10\r\n\r\nHello";
        assertFalse(HttpDecoder.decode(toStream(raw)).isPresent());
    }

    @Test
    void decode_negativeContentLength_returnsEmpty() {
        var raw = "POST / HTTP/1.1\r\nHost: local\r\nContent-Length: -1\r\n\r\n";
        assertFalse(HttpDecoder.decode(toStream(raw)).isPresent());
    }

    @Test
    void decode_chunkedTransferEncoding() {
        var raw = "POST / HTTP/1.1\r\nHost: local\r\nTransfer-Encoding: chunked\r\n\r\n5\r\nHello\r\n0\r\n\r\n";
        var result = HttpDecoder.decode(toStream(raw));
        assertTrue(result.isPresent());
        var req = result.get();
        assertTrue(req.getBody().isPresent());
        assertEquals("Hello", new String(req.getBody().get(), StandardCharsets.UTF_8));
    }

    @Test
    void decode_chunkedWithExtensions() {
        var raw = "POST / HTTP/1.1\r\nHost: local\r\nTransfer-Encoding: chunked\r\n\r\n5;ext=1\r\nHello\r\n0\r\n\r\n";
        var result = HttpDecoder.decode(toStream(raw));
        assertTrue(result.isPresent());
        var req = result.get();
        assertTrue(req.getBody().isPresent());
        assertEquals("Hello", new String(req.getBody().get(), StandardCharsets.UTF_8));
    }

    @Test
    void decode_returnsUriWithoutQuery() {
        var raw = "GET /path?q=test HTTP/1.1\r\nHost: local\r\n\r\n";
        var result = HttpDecoder.decode(toStream(raw));
        assertTrue(result.isPresent());
        var req = result.get();
        assertEquals("/path", req.getUri().getRawPath());
    }

    @Test
    void decode_foldedHeaderValue() {
        var raw = "GET / HTTP/1.1\r\nHost: local\r\nX-Folded: line1\r\n line2\r\n\r\n";
        var result = HttpDecoder.decode(toStream(raw));
        assertTrue(result.isPresent());
        var req = result.get();
        assertTrue(req.getHeader("X-Folded").orElse("").contains("line1line2"));
    }

    @Test
    void decode_headerExceedsMaxLength_returnsEmpty() {
        var sb = new StringBuilder("GET / HTTP/1.1\r\nHost: local\r\nX-Long: ");
        sb.append("a".repeat(9000));
        sb.append("\r\n\r\n");
        assertFalse(HttpDecoder.decode(toStream(sb.toString())).isPresent());
    }
}
