package io.github.blacknoize404.miniJWS.responses;

import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HttpResponseTest {

    @Test
    void builder_defaultStatusCodeIs200() {
        var res = new HttpResponse.Builder().build();
        assertEquals(200, res.getStatusCode());
    }

    @Test
    void builder_setsStatusCode() {
        var res = new HttpResponse.Builder().setStatusCode(404).build();
        assertEquals(404, res.getStatusCode());
    }

    @Test
    void builder_setsContentTypeViaEnum() {
        var res = new HttpResponse.Builder()
            .setContentType(ContentType.JSON)
            .build();
        assertEquals(List.of("application/json;charset=utf-8"), res.getHeaders().get("Content-Type"));
    }

    @Test
    void builder_setsContentTypeViaString() {
        var res = new HttpResponse.Builder()
            .setContentType("application/custom")
            .build();
        assertEquals(List.of("application/custom"), res.getHeaders().get("Content-Type"));
    }

    @Test
    void builder_addsServerHeader() {
        var res = new HttpResponse.Builder().build();
        assertTrue(res.getHeaders().containsKey("Server"));
    }

    @Test
    void builder_addsDateHeader() {
        var res = new HttpResponse.Builder().build();
        assertTrue(res.getHeaders().containsKey("Date"));
    }

    @Test
    void builder_setBodyWithString() {
        var res = new HttpResponse.Builder()
            .setBody("Hello")
            .build();
        assertTrue(res.getBody().isPresent());
        assertArrayEquals("Hello".getBytes(StandardCharsets.UTF_8), res.getBody().get());
    }

    @Test
    void builder_setBodyWithNullString_removesBody() {
        var res = new HttpResponse.Builder()
            .setBody("Hello")
            .setBody((String) null)
            .build();
        assertTrue(res.getBody().isEmpty());
    }

    @Test
    void builder_setBodyWithBytes() {
        var data = new byte[]{1, 2, 3};
        var res = new HttpResponse.Builder().setBody(data).build();
        assertArrayEquals(data, res.getBody().orElseThrow());
    }

    @Test
    void builder_addsCustomHeader() {
        var res = new HttpResponse.Builder()
            .addHeader("X-Custom", "value")
            .build();
        assertEquals(List.of("value"), res.getHeaders().get("X-Custom"));
    }

    @Test
    void builder_addsHeaderWithList() {
        var res = new HttpResponse.Builder()
            .addHeader("X-Multi", List.of("a", "b"))
            .build();
        assertEquals(List.of("a", "b"), res.getHeaders().get("X-Multi"));
    }

    @Test
    void builder_setCookie_simple() {
        var res = new HttpResponse.Builder()
            .setCookie("session", "abc123")
            .build();
        assertEquals(List.of("session=abc123"), res.getHeaders().get("Set-Cookie"));
    }

    @Test
    void builder_setCookie_withOptions() {
        var res = new HttpResponse.Builder()
            .setCookie("token", "xyz", 3600, "/", true)
            .build();
        var cookie = res.getHeaders().get("Set-Cookie").get(0);
        assertTrue(cookie.contains("token=xyz"));
        assertTrue(cookie.contains("Max-Age=3600"));
        assertTrue(cookie.contains("Path=/"));
        assertTrue(cookie.contains("HttpOnly"));
    }

    @Test
    void builder_setsMethod() {
        var res = new HttpResponse.Builder()
            .setMethod(HttpMethod.POST)
            .build();
        assertEquals(HttpMethod.POST, res.getMethod());
    }

    @Test
    void builder_setsProtocolVersion() {
        var res = new HttpResponse.Builder()
            .setProtocolVersion("HTTP/2")
            .build();
        assertEquals("HTTP/2", res.getProtocolVersion());
    }

    @Test
    void redirect_defaultIs302() {
        var res = HttpResponse.redirect("/new-location");
        assertEquals(302, res.getStatusCode());
        assertEquals(List.of("/new-location"), res.getHeaders().get("Location"));
    }

    @Test
    void redirect_withCustomCode() {
        var res = HttpResponse.redirect("/permanent", 301);
        assertEquals(301, res.getStatusCode());
        assertEquals(List.of("/permanent"), res.getHeaders().get("Location"));
    }

    @Test
    void getBody_withNoBody_returnsEmpty() {
        var res = new HttpResponse.Builder().build();
        assertTrue(res.getBody().isEmpty());
    }

    @Test
    void builder_buildMultipleTimes_areIndependent() {
        var builder = new HttpResponse.Builder().setStatusCode(200).setBody("A");
        var a = builder.build();
        var b = builder.setStatusCode(404).setBody("B").build();
        assertEquals(200, a.getStatusCode());
        assertEquals("A", new String(a.getBody().orElseThrow(), StandardCharsets.UTF_8));
        assertEquals(404, b.getStatusCode());
        assertEquals("B", new String(b.getBody().orElseThrow(), StandardCharsets.UTF_8));
    }
}
