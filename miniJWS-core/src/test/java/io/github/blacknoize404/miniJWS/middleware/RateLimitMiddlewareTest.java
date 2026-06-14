package io.github.blacknoize404.miniJWS.middleware;

import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import io.github.blacknoize404.miniJWS.primitives.MiddlewareChain;
import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitMiddlewareTest {

    private final MiddlewareChain okChain = req ->
        new HttpResponse.Builder().setStatusCode(200).setBody("OK").build();

    private HttpRequest requestFromIp(String ip) {
        return new HttpRequest.Builder()
            .setHttpMethod(HttpMethod.GET)
            .setUri(URI.create("/test"))
            .setProtocolVersion("HTTP/1.1")
            .setHeaders(ip != null ? Map.of("X-Forwarded-For", List.of(ip)) : Map.of())
            .build();
    }

    @Test
    void run_firstRequest_allows() {
        var mw = new RateLimitMiddleware(2, 60);
        var req = requestFromIp("192.168.1.1");
        var res = mw.run(req, okChain);
        assertEquals(200, res.getStatusCode());
    }

    @Test
    void run_withinLimit_allows() {
        var mw = new RateLimitMiddleware(3, 60);
        var req = requestFromIp("10.0.0.1");
        assertEquals(200, mw.run(req, okChain).getStatusCode());
        assertEquals(200, mw.run(req, okChain).getStatusCode());
        assertEquals(200, mw.run(req, okChain).getStatusCode());
    }

    @Test
    void run_exceedsLimit_returns429() {
        var mw = new RateLimitMiddleware(2, 60);
        var req = requestFromIp("10.0.0.2");
        assertEquals(200, mw.run(req, okChain).getStatusCode());
        assertEquals(200, mw.run(req, okChain).getStatusCode());
        var res = mw.run(req, okChain);
        assertEquals(429, res.getStatusCode());
    }

    @Test
    void run_exceedsLimit_hasRetryAfterHeader() {
        var mw = new RateLimitMiddleware(1, 30);
        var req = requestFromIp("10.0.0.3");
        mw.run(req, okChain);
        var res = mw.run(req, okChain);
        assertEquals("30", res.getHeaders().get("Retry-After").get(0));
    }

    @Test
    void run_differentIps_independentCounters() {
        var mw = new RateLimitMiddleware(1, 60);
        var reqA = requestFromIp("10.0.0.4");
        var reqB = requestFromIp("10.0.0.5");
        assertEquals(200, mw.run(reqA, okChain).getStatusCode());
        assertEquals(200, mw.run(reqB, okChain).getStatusCode());
    }

    @Test
    void run_usesXForwardedFor() {
        var mw = new RateLimitMiddleware(2, 60);
        var req = requestFromIp("10.0.0.6");
        assertEquals(200, mw.run(req, okChain).getStatusCode());
        assertEquals(200, mw.run(req, okChain).getStatusCode());
    }

    @Test
    void run_fallbackToLocalhost() {
        var mw = new RateLimitMiddleware(5, 60);
        var req = requestFromIp(null);
        assertEquals(200, mw.run(req, okChain).getStatusCode());
    }
}
