package io.github.blacknoize404.miniJWS.headers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FieldTest {

    @Test
    void parse_typeAndSubtype() {
        var field = Field.parse("text/html");
        assertEquals("text", field.getType());
        assertTrue(field.getSubtype().isPresent());
        assertEquals("html", field.getSubtype().get());
    }

    @Test
    void parse_typeOnly() {
        var field = Field.parse("text");
        assertEquals("text", field.getType());
        assertTrue(field.getSubtype().isEmpty());
    }

    @Test
    void parse_withParameters() {
        var field = Field.parse("text/html; charset=utf-8; q=0.9");
        assertEquals("text", field.getType());
        assertEquals("html", field.getSubtype().orElse(""));
        assertTrue(field.getParameters().isPresent());
        assertEquals(2, field.getParameters().get().size());
    }

    @Test
    void parse_emptySubtype() {
        var field = Field.parse("text/");
        assertEquals("text", field.getType());
        assertTrue(field.getSubtype().isPresent());
        assertTrue(field.getSubtype().get().isEmpty());
    }

    @Test
    void parse_parametersReturnedAsList() {
        var field = Field.parse("a/b; x=1; y=2");
        var params = field.getParameters().orElseThrow();
        assertEquals(2, params.size());
        assertEquals("x", params.get(0).getName());
        assertEquals("1", params.get(0).getValue());
    }

    @Test
    void toString_containsTypeAndSubtype() {
        var field = Field.parse("application/json");
        var str = field.toString();
        assertTrue(str.contains("application"));
        assertTrue(str.contains("json"));
    }
}
