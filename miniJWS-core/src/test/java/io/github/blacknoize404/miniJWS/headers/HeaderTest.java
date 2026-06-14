package io.github.blacknoize404.miniJWS.headers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeaderTest {

    @Test
    void parse_simpleHeader() {
        var header = Header.parse("Content-Type: text/html");
        assertEquals("Content-Type", header.getName());
        assertEquals(1, header.getFields().size());
        assertEquals("text", header.getFields().get(0).getType());
    }

    @Test
    void parse_headerWithMultipleValues() {
        var header = Header.parse("Accept: text/html, application/json");
        assertEquals("Accept", header.getName());
        assertEquals(2, header.getFields().size());
    }

    @Test
    void parse_headerWithParameters() {
        var header = Header.parse("Content-Type: text/html; charset=utf-8");
        assertEquals("Content-Type", header.getName());
        assertEquals(1, header.getFields().size());
        var field = header.getFields().get(0);
        assertTrue(field.getParameters().isPresent());
        assertEquals("utf-8", field.getParameters().get().get(0).getValue());
    }

    @Test
    void parse_headerMissingColon_throws() {
        assertThrows(IllegalArgumentException.class, () -> Header.parse("Invalid-Header"));
    }

    @Test
    void toString_returnsFormattedHeader() {
        var header = Header.parse("X-Test: value");
        var str = header.toString();
        assertTrue(str.contains("X-Test"));
        assertTrue(str.contains("value"));
    }
}
