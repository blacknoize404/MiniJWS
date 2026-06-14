package io.github.blacknoize404.miniJWS.content;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ContentTypesTest {

    @ParameterizedTest
    @CsvSource({
        "html, text/html;charset=utf-8",
        "css, text/css;charset=utf-8",
        "js, text/javascript;charset=utf-8",
        "json, application/json;charset=utf-8",
        "png, image/png",
        "jpg, image/jpeg",
        "jpeg, image/jpeg",
        "svg, image/svg+xml",
        "ico, image/x-icon",
        "pdf, application/pdf",
        "zip, application/zip",
        "apk, application/vnd.android.package-archive"
    })
    void forExtension_knownExtensions_returnsCorrectMime(String ext, String expectedMime) {
        assertEquals(expectedMime, ContentTypes.forExtension(ext));
    }

    @ParameterizedTest
    @ValueSource(strings = {"apk", "deb", "rpm", "exe", "dll"})
    void forExtension_customTypes_returnsSpecificMime(String ext) {
        String mime = ContentTypes.forExtension(ext);
        assertNotNull(mime);
        assertFalse(mime.isEmpty());
    }

    @Test
    void forExtension_nullReturnsOctetStream() {
        assertEquals("application/octet-stream", ContentTypes.forExtension(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "unknown_ext_xyz"})
    void forExtension_unknownReturnsDefault(String ext) {
        assertEquals("application/octet-stream", ContentTypes.forExtension(ext));
    }

    @Test
    void forExtension_isCaseInsensitive() {
        assertEquals(
            ContentTypes.forExtension("HTML"),
            ContentTypes.forExtension("html")
        );
    }

    @Test
    void extensionToMimeMap_containsAllEntries() {
        assertFalse(ContentTypes.EXTENSION_TO_MIME.isEmpty());
    }
}
