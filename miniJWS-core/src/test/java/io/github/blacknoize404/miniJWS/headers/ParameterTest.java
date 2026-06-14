package io.github.blacknoize404.miniJWS.headers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ParameterTest {

    @Test
    void parse_simpleKeyValue() {
        var param = Parameter.parse("charset=utf-8");
        assertEquals("charset", param.getName());
        assertEquals("utf-8", param.getValue());
    }

    @Test
    void parse_trimsWhitespace() {
        var param = Parameter.parse("  key  =  value  ");
        assertEquals("key", param.getName());
        assertEquals("value", param.getValue());
    }

    @Test
    void parse_missingEqualsSign_throws() {
        assertThrows(IllegalArgumentException.class, () -> Parameter.parse("invalid"));
    }

    @Test
    void parse_emptyValueIsAllowed() {
        var param = Parameter.parse("key=");
        assertEquals("key", param.getName());
        assertEquals("", param.getValue());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void parse_nullOrEmpty_throws(String data) {
        assertThrows(Exception.class, () -> Parameter.parse(data));
    }

    @Test
    void toString_returnsNameEqualsValue() {
        var param = Parameter.parse("a=b");
        assertEquals("a=b", param.toString());
    }

    @Test
    void toString_withSpecialChars() {
        var param = Parameter.parse("name=John%20Doe");
        assertEquals("name=John%20Doe", param.toString());
    }
}
