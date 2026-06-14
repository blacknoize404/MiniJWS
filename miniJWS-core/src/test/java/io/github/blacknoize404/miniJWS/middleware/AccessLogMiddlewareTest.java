package io.github.blacknoize404.miniJWS.middleware;

import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import io.github.blacknoize404.miniJWS.primitives.MiddlewareChain;
import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AccessLogMiddlewareTest {

    private final MiddlewareChain okChain = req ->
        new HttpResponse.Builder().setStatusCode(200).setBody("OK").build();

    @Test
    void run_logsRequestToWriter() throws Exception {
        var sw = new StringWriter();
        var pw = new PrintWriter(sw, true);
        var mw = new AccessLogMiddleware(pw);
        var req = new HttpRequest.Builder()
            .setHttpMethod(HttpMethod.GET)
            .setUri(URI.create("/test"))
            .setProtocolVersion("HTTP/1.1")
            .build();
        var res = mw.run(req, okChain);
        assertEquals(200, res.getStatusCode());
        Thread.sleep(100);
        var log = sw.toString();
        assertTrue(log.contains("GET /test HTTP/1.1"));
        assertTrue(log.contains("200"));
    }

    @Test
    void run_logsRemoteAddrFromXForwardedFor() throws Exception {
        var sw = new StringWriter();
        var pw = new PrintWriter(sw, true);
        var mw = new AccessLogMiddleware(pw);
        var req = new HttpRequest.Builder()
            .setHttpMethod(HttpMethod.GET)
            .setUri(URI.create("/proxy"))
            .setProtocolVersion("HTTP/1.1")
            .setHeaders(Map.of("X-Forwarded-For", List.of("203.0.113.5")))
            .build();
        mw.run(req, okChain);
        Thread.sleep(100);
        var log = sw.toString();
        assertTrue(log.contains("203.0.113.5"));
    }

    @Test
    void run_logsResponseBodySize() throws Exception {
        var sw = new StringWriter();
        var pw = new PrintWriter(sw, true);
        var mw = new AccessLogMiddleware(pw);
        var req = new HttpRequest.Builder()
            .setHttpMethod(HttpMethod.GET)
            .setUri(URI.create("/data"))
            .setProtocolVersion("HTTP/1.1")
            .build();
        var res = mw.run(req, okChain);
        Thread.sleep(100);
        var log = sw.toString();
        int bodySize = res.getBody().map(b -> b.length).orElse(0);
        assertTrue(bodySize > 0);
        assertTrue(log.contains(String.valueOf(bodySize)));
    }
}
