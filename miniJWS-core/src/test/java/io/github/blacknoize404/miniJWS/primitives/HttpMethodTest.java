package io.github.blacknoize404.miniJWS.primitives;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpMethodTest {

    @Test
    void enum_hasAllStandardMethods() {
        assertAll(
            () -> assertNotNull(HttpMethod.GET),
            () -> assertNotNull(HttpMethod.HEAD),
            () -> assertNotNull(HttpMethod.POST),
            () -> assertNotNull(HttpMethod.PUT),
            () -> assertNotNull(HttpMethod.DELETE),
            () -> assertNotNull(HttpMethod.CONNECT),
            () -> assertNotNull(HttpMethod.OPTIONS),
            () -> assertNotNull(HttpMethod.TRACE),
            () -> assertNotNull(HttpMethod.PATCH)
        );
    }

    @Test
    void valueOf_isCaseSensitive() {
        assertEquals(HttpMethod.GET, HttpMethod.valueOf("GET"));
        assertThrows(IllegalArgumentException.class, () -> HttpMethod.valueOf("get"));
    }
}
