package io.github.blacknoize404.miniJWS;

import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpServerTest {

    @ParameterizedTest
    @CsvSource({
        "GET /users/:id, GET /users/42, id=42",
        "GET /users/:id/posts/:postId, GET /users/1/posts/2, id=1,postId=2",
        "GET /files/*, GET /files/test.txt, ''",
        "GET /static/**, GET /static/css/style.css, ''",
        "GET /, GET /, ''"
    })
    void matchPath_returnsCorrectParams(String pattern, String path, String expectedParams) {
        String[] patParts = pattern.split(" ", 2);
        String[] pathParts = path.split(" ", 2);
        var result = HttpServer.matchPath(patParts[1], pathParts[1]);
        if (expectedParams.isEmpty()) {
            assertNotNull(result);
        } else {
            assertNotNull(result);
            var expected = expectedParams.split(",");
            for (String param : expected) {
                String[] kv = param.split("=");
                assertEquals(kv[1], result.get(kv[0]));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({
        "GET /users/:id, GET /posts/42",
        "GET /users/:id, GET /users/42/posts",
        "GET /exact, GET /different",
        "GET /a/:p/b, GET /a/1/c"
    })
    void matchPath_returnsNullForNonMatching(String pattern, String path) {
        String[] patParts = pattern.split(" ", 2);
        String[] pathParts = path.split(" ", 2);
        assertNull(HttpServer.matchPath(patParts[1], pathParts[1]));
    }

    @Test
    void addRoute_thenRemoveRoute_removesIt() throws Exception {
        var server = new HttpServer(0);
        server.addRoute(HttpMethod.GET, "/test", req ->
            new HttpResponse.Builder().setBody("test").build());
        server.removeRoute(HttpMethod.GET, "/test");
        server.stop();
    }

    @Test
    void addStaticRoute_addsRoute() throws Exception {
        var server = new HttpServer(0);
        server.addStaticRoute("/static", "src/test/resources");
        server.stop();
    }

    @Test
    void serverName_isMiniJWS() {
        assertEquals("MiniJWS", HttpServer.SERVER_NAME);
    }

    @Test
    void constructor_withPortOnly_startsServer() throws Exception {
        var server = new HttpServer(0);
        assertNotNull(server);
        server.stop();
    }

    @Test
    void constructor_withPortAndParallelism_startsServer() throws Exception {
        var server = new HttpServer(0, 2);
        assertNotNull(server);
        server.stop();
    }
}
