package io.github.blacknoize404.miniApkReader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ApkInfoExtractorTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void main_noArgs_printsUsage() {
        ApkInfoExtractor.main(new String[]{});
        var output = outContent.toString();
        assertTrue(output.contains("Usage") || output.contains("ApkInfoExtractor"));
    }

    @Test
    void main_fileNotFound_printsError() {
        ApkInfoExtractor.main(new String[]{"non-existent.apk"});
        var error = errContent.toString();
        assertTrue(error.contains("not found") || error.contains("non-existent"));
    }

    @Test
    void main_fileNotFound_messageContainsPath() {
        ApkInfoExtractor.main(new String[]{"some/path.apk"});
        var error = errContent.toString();
        assertTrue(error.contains("some/path.apk"));
    }

    @Test
    void main_invalidApkFile_printsErrorMessage(@TempDir Path tempDir) throws Exception {
        var fakeApk = tempDir.resolve("fake.apk");
        java.nio.file.Files.writeString(fakeApk, "not-a-real-apk");
        ApkInfoExtractor.main(new String[]{fakeApk.toString()});
        var error = errContent.toString();
        assertTrue(error.contains("Error reading APK"));
    }

    @Test
    void main_handlesRelativePath() {
        ApkInfoExtractor.main(new String[]{"../pom.xml"});
        var error = errContent.toString();
        assertTrue(error.contains("not found") || error.contains("Error reading APK"));
    }

    @Test
    void main_handlesWindowsPath() {
        ApkInfoExtractor.main(new String[]{"C:\\nonexistent\\app.apk"});
        var error = errContent.toString();
        assertTrue(error.contains("not found") || error.contains("C:\\nonexistent\\app.apk"));
    }
}
