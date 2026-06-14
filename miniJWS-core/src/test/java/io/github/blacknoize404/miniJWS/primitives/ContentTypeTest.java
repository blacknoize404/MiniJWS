package io.github.blacknoize404.miniJWS.primitives;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ContentTypeTest {

    @ParameterizedTest
    @CsvSource({
        "html, HTML",
        "css, CSS",
        "js, JS",
        "json, JSON",
        "xml, XML",
        "svg, SVG",
        "ico, ICO",
        "png, PNG",
        "jpg, JPEG",
        "jpeg, JPEG",
        "gif, GIF",
        "webp, WEBP",
        "mp4, MP4",
        "webm, WEBM",
        "woff2, WOFF2",
        "ttf, TTF",
        "pdf, PDF",
        "zip, ZIP"
    })
    void fromExtension_knownExtensions_returnsCorrectType(String ext, String expectedName) {
        var result = ContentType.fromExtension(ext);
        assertTrue(result.isPresent());
        assertEquals(ContentType.valueOf(expectedName), result.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"apk", "deb", "rpm", "exe", "dll"})
    void fromExtension_unknownExtensions_returnsEmpty(String ext) {
        assertTrue(ContentType.fromExtension(ext).isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    void fromExtension_blankExtensions_returnsEmpty(String ext) {
        assertTrue(ContentType.fromExtension(ext).isEmpty());
    }

    @Test
    void fromExtension_isCaseInsensitive() {
        var html = ContentType.fromExtension("HTML");
        var Html = ContentType.fromExtension("Html");
        assertTrue(html.isPresent());
        assertEquals(html.get(), Html.get());
    }

    @ParameterizedTest
    @CsvSource({
        "application/octet-stream, FILE",
        "application/json;charset=utf-8, JSON",
        "text/html;charset=utf-8, HTML",
        "text/plain;charset=utf-8, TEXT",
        "image/png, PNG",
        "image/svg+xml, SVG",
        "font/woff2, WOFF2"
    })
    void fromMime_knownMimes_returnsCorrectType(String mime, String expectedName) {
        assertEquals(ContentType.valueOf(expectedName), ContentType.fromMime(mime));
    }

    @Test
    void fromMime_unknownMime_throws() {
        assertThrows(IllegalArgumentException.class, () -> ContentType.fromMime("application/unknown"));
    }

    @Test
    void mime_returnsCorrectString() {
        assertEquals("text/html;charset=utf-8", ContentType.HTML.mime());
        assertEquals("application/json;charset=utf-8", ContentType.JSON.mime());
        assertEquals("application/octet-stream", ContentType.FILE.mime());
    }

    @Test
    void fromExtension_withLeadingDot_returnsEmpty() {
        assertTrue(ContentType.fromExtension(".html").isEmpty());
    }
}
