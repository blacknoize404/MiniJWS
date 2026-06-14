package io.github.blacknoize404.miniJWS.middleware;

import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import io.github.blacknoize404.miniJWS.primitives.MiddlewareChain;
import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CorsMiddlewareTest {

    private HttpRequest requestWithOrigin(HttpMethod method, String origin) {
        var builder = new HttpRequest.Builder()
            .setHttpMethod(method)
            .setUri(URI.create("/test"))
            .setProtocolVersion("HTTP/1.1");
        if (origin != null) {
            builder.setHeaders(Map.of("Origin", List.of(origin)));
        }
        return builder.build();
    }

    private final MiddlewareChain okChain = req ->
        new HttpResponse.Builder().setStatusCode(200).setBody("OK").build();

    @Test
    void run_noOrigin_passesThrough() {
        var mw = new CorsMiddleware();
        var req = requestWithOrigin(HttpMethod.GET, null);
        var res = mw.run(req, okChain);
        assertEquals(200, res.getStatusCode());
    }

    @Test
    void run_withOrigin_addsCorsHeader() {
        var mw = new CorsMiddleware().allowOrigin("https://example.com");
        var req = requestWithOrigin(HttpMethod.GET, "https://example.com");
        var res = mw.run(req, okChain);
        assertEquals("https://example.com", res.getHeaders().get("Access-Control-Allow-Origin").get(0));
    }

    @Test
    void run_preflightRequest_returns204() {
        var mw = new CorsMiddleware().allowOrigin("*");
        var req = requestWithOrigin(HttpMethod.OPTIONS, "https://other.com");
        var res = mw.run(req, okChain);
        assertEquals(204, res.getStatusCode());
    }

    @Test
    void run_preflight_includesMethods() {
        var mw = new CorsMiddleware().allowOrigin("*");
        var req = requestWithOrigin(HttpMethod.OPTIONS, "https://example.com");
        var res = mw.run(req, okChain);
        assertTrue(res.getHeaders().containsKey("Access-Control-Allow-Methods"));
    }

    @Test
    void run_preflight_includesHeaders() {
        var mw = new CorsMiddleware().allowOrigin("*");
        var req = requestWithOrigin(HttpMethod.OPTIONS, "https://example.com");
        var res = mw.run(req, okChain);
        assertTrue(res.getHeaders().containsKey("Access-Control-Allow-Headers"));
    }

    @Test
    void allowCredentials_withWildcardOrigin_throws() {
        assertThrows(IllegalStateException.class, () ->
            new CorsMiddleware().allowOrigin("*").allowCredentials(true));
    }

    @Test
    void allowCredentials_withSpecificOrigin_setsHeader() {
        var mw = new CorsMiddleware()
            .allowOrigin("https://example.com")
            .allowCredentials(true);
        var req = requestWithOrigin(HttpMethod.GET, "https://example.com");
        var res = mw.run(req, okChain);
        assertEquals("true", res.getHeaders().get("Access-Control-Allow-Credentials").get(0));
    }

    @Test
    void maxAge_setsHeader() {
        var mw = new CorsMiddleware().allowOrigin("*").maxAge(3600);
        var req = requestWithOrigin(HttpMethod.OPTIONS, "https://example.com");
        var res = mw.run(req, okChain);
        assertEquals("3600", res.getHeaders().get("Access-Control-Max-Age").get(0));
    }

    @Test
    void allowMethods_overridesDefaults() {
        var mw = new CorsMiddleware().allowOrigin("*").allowMethods("GET", "POST");
        var req = requestWithOrigin(HttpMethod.OPTIONS, "https://example.com");
        var res = mw.run(req, okChain);
        assertEquals("GET, POST", res.getHeaders().get("Access-Control-Allow-Methods").get(0));
    }

    @Test
    void allowHeaders_overridesDefaults() {
        var mw = new CorsMiddleware().allowOrigin("*").allowHeaders("X-Custom");
        var req = requestWithOrigin(HttpMethod.OPTIONS, "https://example.com");
        var res = mw.run(req, okChain);
        assertEquals("X-Custom", res.getHeaders().get("Access-Control-Allow-Headers").get(0));
    }

    @Test
    void run_preservesOriginalStatusCodeAndBody() {
        var mw = new CorsMiddleware().allowOrigin("https://site.com");
        var req = requestWithOrigin(HttpMethod.GET, "https://site.com");
        var res = mw.run(req, okChain);
        assertEquals(200, res.getStatusCode());
        assertTrue(res.getBody().isPresent());
        assertEquals("OK", new String(res.getBody().get()));
    }
}
