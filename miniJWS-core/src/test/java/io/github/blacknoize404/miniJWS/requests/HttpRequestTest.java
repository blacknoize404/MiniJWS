package io.github.blacknoize404.miniJWS.requests;

import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpRequestTest {

    private HttpRequest.Builder baseBuilder() {
        return new HttpRequest.Builder()
            .setHttpMethod(HttpMethod.GET)
            .setUri(URI.create("/test"))
            .setProtocolVersion("HTTP/1.1");
    }

    @Test
    void builder_createsRequestWithAllFields() {
        var headers = Map.of("Host", List.of("example.com"));
        var params = Map.of("q", "hello");
        var body = "test-body".getBytes();
        var req = baseBuilder()
            .setHeaders(headers)
            .setParameters(params)
            .setBody(body)
            .build();

        assertEquals(HttpMethod.GET, req.getHttpMethod());
        assertEquals(URI.create("/test"), req.getUri());
        assertEquals("HTTP/1.1", req.getProtocolVersion());
        assertEquals(headers, req.getHeaders());
        assertEquals(params, req.getParameters());
        assertTrue(req.getBody().isPresent());
        assertArrayEquals(body, req.getBody().get());
    }

    @Test
    void builder_missingMethod_throwsNullPointer() {
        assertThrows(NullPointerException.class, () ->
            new HttpRequest.Builder()
                .setUri(URI.create("/test"))
                .build());
    }

    @Test
    void builder_missingUri_throwsNullPointer() {
        assertThrows(NullPointerException.class, () ->
            new HttpRequest.Builder()
                .setHttpMethod(HttpMethod.GET)
                .build());
    }

    @Test
    void getHeader_returnsJoinedValue() {
        var headers = Map.of("Accept", List.of("text/html", "application/json"));
        var req = baseBuilder().setHeaders(headers).build();
        assertEquals("text/html, application/json", req.getHeader("Accept").orElse(""));
    }

    @Test
    void getHeader_missingKey_returnsEmpty() {
        var req = baseBuilder().build();
        assertTrue(req.getHeader("X-Nonexistent").isEmpty());
    }

    @Test
    void getCookies_parsesFromCookieHeader() {
        var headers = Map.of("Cookie", List.of("session=abc123; theme=dark"));
        var req = baseBuilder().setHeaders(headers).build();
        assertEquals("abc123", req.getCookies().get("session"));
        assertEquals("dark", req.getCookies().get("theme"));
    }

    @Test
    void getCookies_withNoCookieHeader_returnsEmptyMap() {
        var req = baseBuilder().build();
        assertTrue(req.getCookies().isEmpty());
    }

    @Test
    void bodyAsString_returnsUtf8Content() {
        var req = baseBuilder().setBody("héllo wörld".getBytes(java.nio.charset.StandardCharsets.UTF_8)).build();
        assertEquals("héllo wörld", req.bodyAsString().orElse(""));
    }

    @Test
    void bodyAsString_withNoBody_returnsEmpty() {
        var req = baseBuilder().build();
        assertTrue(req.bodyAsString().isEmpty());
    }

    @Test
    void bodyAsForm_parsesUrlEncoded() {
        var req = baseBuilder()
            .setBody("name=John+Doe&age=25".getBytes())
            .build();
        var form = req.bodyAsForm().orElse(Map.of());
        assertEquals("John Doe", form.get("name"));
        assertEquals("25", form.get("age"));
    }

    @Test
    void bodyAsForm_withNoBody_returnsEmpty() {
        var req = baseBuilder().build();
        assertTrue(req.bodyAsForm().isEmpty());
    }

    @Test
    void bodyAsJson_parsesFlatJson() {
        var req = baseBuilder()
            .setBody("{\"name\":\"Alice\",\"age\":\"30\"}".getBytes())
            .build();
        var json = req.bodyAsJson().orElse(Map.of());
        assertEquals("Alice", json.get("name"));
        assertEquals("30", json.get("age"));
    }

    @Test
    void bodyAsJson_handlesSingleQuotes() {
        var req = baseBuilder()
            .setBody("{'key':'val'}".getBytes())
            .build();
        var json = req.bodyAsJson().orElse(Map.of());
        assertEquals("val", json.get("key"));
    }

    @Test
    void bodyAsJson_withNoBody_returnsEmpty() {
        var req = baseBuilder().build();
        assertTrue(req.bodyAsJson().isEmpty());
    }

    @Test
    void toString_containsMethodAndUri() {
        var req = baseBuilder().build();
        var str = req.toString();
        assertTrue(str.contains("GET"));
        assertTrue(str.contains("/test"));
    }

    @Test
    void addHeader_replacesExistingKey() {
        var req = baseBuilder()
            .addHeader("X-Custom", List.of("value1"))
            .addHeader("X-Custom", List.of("value2"))
            .build();
        assertEquals(List.of("value2"), req.getHeaders().get("X-Custom"));
    }

    @Test
    void addParameter_addsToExistingParams() {
        var req = baseBuilder()
            .addParameter("a", "1")
            .addParameter("b", "2")
            .build();
        assertEquals("1", req.getParameters().get("a"));
        assertEquals("2", req.getParameters().get("b"));
    }

    @Test
    void getBody_withNullBody_returnsEmpty() {
        var req = baseBuilder().setBody((byte[]) null).build();
        assertTrue(req.getBody().isEmpty());
    }

    @Test
    void bodyAsForm_handlesEmptyString() {
        var req = baseBuilder().setBody("".getBytes()).build();
        assertTrue(req.bodyAsForm().orElse(Map.of()).isEmpty());
    }

    @Test
    void bodyAsJson_handlesEmptyObject() {
        var req = baseBuilder().setBody("{}".getBytes()).build();
        assertTrue(req.bodyAsJson().orElse(Map.of()).isEmpty());
    }
}
