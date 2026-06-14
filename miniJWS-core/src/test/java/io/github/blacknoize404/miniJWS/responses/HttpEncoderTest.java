package io.github.blacknoize404.miniJWS.responses;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HttpEncoderTest {

    @Test
    void sendResponse_writesStatusLine() {
        var res = new HttpResponse.Builder()
            .setStatusCode(200)
            .setProtocolVersion("HTTP/1.1")
            .build();
        var out = new ByteArrayOutputStream();
        HttpEncoder.sendResponse(res, out);
        var output = out.toString(StandardCharsets.US_ASCII);
        assertTrue(output.startsWith("HTTP/1.1 200 OK\r\n"));
    }

    @Test
    void sendResponse_writes404StatusLine() {
        var res = new HttpResponse.Builder()
            .setStatusCode(404)
            .build();
        var out = new ByteArrayOutputStream();
        HttpEncoder.sendResponse(res, out);
        var output = out.toString(StandardCharsets.US_ASCII);
        assertTrue(output.contains("404 NOT_FOUND"));
    }

    @Test
    void sendResponse_writesHeaders() {
        var res = new HttpResponse.Builder()
            .addHeader("X-Test", "value123")
            .build();
        var out = new ByteArrayOutputStream();
        HttpEncoder.sendResponse(res, out);
        var output = out.toString(StandardCharsets.US_ASCII);
        assertTrue(output.contains("X-Test: value123"));
    }

    @Test
    void sendResponse_writesBodyAndContentLength() {
        var body = "Hello, World!";
        var res = new HttpResponse.Builder()
            .setBody(body)
            .build();
        var out = new ByteArrayOutputStream();
        HttpEncoder.sendResponse(res, out);
        var output = out.toString(StandardCharsets.US_ASCII);
        assertTrue(output.contains("Content-Length: 13"));
        assertTrue(output.contains("Hello, World!"));
    }

    @Test
    void sendResponse_withNoBody_omitsContentLength() {
        var res = new HttpResponse.Builder()
            .setStatusCode(204)
            .build();
        var out = new ByteArrayOutputStream();
        HttpEncoder.sendResponse(res, out);
        var output = out.toString(StandardCharsets.US_ASCII);
        assertFalse(output.contains("Content-Length"));
    }

    @Test
    void sendResponse_endsWithCrLfBeforeBody() {
        var res = new HttpResponse.Builder()
            .setBody("data")
            .build();
        var out = new ByteArrayOutputStream();
        HttpEncoder.sendResponse(res, out);
        var output = out.toString(StandardCharsets.US_ASCII);
        assertTrue(output.contains("\r\n\r\n"));
    }

    @Test
    void sendResponse_writesMultipleHeaderValues() {
        var res = new HttpResponse.Builder()
            .addHeader("X-Multi", List.of("a", "b"))
            .build();
        var out = new ByteArrayOutputStream();
        HttpEncoder.sendResponse(res, out);
        var output = out.toString(StandardCharsets.US_ASCII);
        assertTrue(output.contains("X-Multi: a, b"));
    }
}
