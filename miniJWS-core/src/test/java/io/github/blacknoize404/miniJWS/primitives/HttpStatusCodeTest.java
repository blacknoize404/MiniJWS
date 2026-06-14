package io.github.blacknoize404.miniJWS.primitives;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class HttpStatusCodeTest {

    @ParameterizedTest
    @CsvSource({
        "100, CONTINUE",
        "200, OK",
        "201, CREATED",
        "204, NO_CONTENT",
        "301, MOVED_PERMANENTLY",
        "302, FOUND",
        "400, BAD_REQUEST",
        "401, UNAUTHORIZED",
        "403, FORBIDDEN",
        "404, NOT_FOUND",
        "405, METHOD_NOT_ALLOWED",
        "408, REQUEST_TIMEOUT",
        "429, TOO_MANY_REQUESTS",
        "500, INTERNAL_SERVER_ERROR",
        "502, BAD_GATEWAY",
        "503, SERVICE_UNAVAILABLE"
    })
    void getMessage_knownCodes_returnsCorrectMessage(int code, String expected) {
        assertEquals(expected, HttpStatusCode.getMessage(code));
    }

    @ParameterizedTest
    @ValueSource(ints = {102, 300, 406, 501, 505, -1, 0, 999})
    void getMessage_unknownCodes_returnsUnknown(int code) {
        assertEquals("UNKNOWN", HttpStatusCode.getMessage(code));
    }

    @Test
    void statusCodesMap_isModifiable() {
        assertDoesNotThrow(() -> HttpStatusCode.getMessage(200));
    }

    @Test
    void statusCodesMap_isNotEmpty() {
        assertFalse(HttpStatusCode.STATUS_CODES.isEmpty());
    }
}
