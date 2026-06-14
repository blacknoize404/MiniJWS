package io.github.blacknoize404.miniApkReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApkReaderTest {

    @Test
    void printInfo_containsAllSections() {
        var info = new ApkInfo("com.test", "1.0", 1, "21", "35",
            List.of("INTERNET", "CAMERA"), List.of("camera", "nfc"),
            "TestApp", "res/icon.png");
        var output = ApkReader.printInfo(info);
        assertTrue(output.contains("=== APK Information ==="));
        assertTrue(output.contains("Package: com.test"));
        assertTrue(output.contains("Version: 1.0 (1)"));
        assertTrue(output.contains("SDK: min=21, target=35"));
        assertTrue(output.contains("Label: TestApp"));
        assertTrue(output.contains("Icon: res/icon.png"));
        assertTrue(output.contains("Permissions:"));
        assertTrue(output.contains("  - INTERNET"));
        assertTrue(output.contains("  - CAMERA"));
        assertTrue(output.contains("Features:"));
        assertTrue(output.contains("  - camera"));
        assertTrue(output.contains("  - nfc"));
    }

    @Test
    void printInfo_withNullLabelAndIcon() {
        var info = new ApkInfo("com.null", "1.0", 1, "21", "35",
            List.of(), List.of(), null, null);
        var output = ApkReader.printInfo(info);
        assertTrue(output.contains("Label: null"));
        assertTrue(output.contains("Icon: null"));
    }

    @Test
    void printInfo_withEmptyPermissionsAndFeatures_printsHeadersStill() {
        var info = new ApkInfo("com.empty", "1.0", 1, "21", "35",
            List.of(), List.of(), "Empty", null);
        var output = ApkReader.printInfo(info);
        assertTrue(output.contains("Permissions:"));
        assertTrue(output.contains("Features:"));
    }

    @Test
    void printInfo_usesNewlineSeparators() {
        var info = new ApkInfo("com.test", "1.0", 1, "21", "35",
            List.of("INTERNET"), List.of("camera"), "Test", null);
        var output = ApkReader.printInfo(info);
        var lines = output.split("\n");
        assertTrue(lines.length >= 9);
    }

    @Test
    void printInfo_withLargeVersionCode() {
        var info = new ApkInfo("com.large", "1.0", 9876543210L, "21", "35",
            List.of(), List.of(), "Large", null);
        var output = ApkReader.printInfo(info);
        assertTrue(output.contains("9876543210"));
    }

    @Test
    void printInfo_withManyPermissions_indentsEach() {
        var perms = List.of("A", "B", "C", "D", "E");
        var info = new ApkInfo("com.multi", "1.0", 1, "21", "35",
            perms, List.of(), "Multi", null);
        var output = ApkReader.printInfo(info);
        perms.forEach(p -> assertTrue(output.contains("  - " + p)));
    }

    @Test
    void printInfo_withManyFeatures_indentsEach() {
        var feats = List.of("a", "b", "c");
        var info = new ApkInfo("com.multi", "1.0", 1, "21", "35",
            List.of(), feats, "Multi", null);
        var output = ApkReader.printInfo(info);
        feats.forEach(f -> assertTrue(output.contains("  - " + f)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    void printInfo_handlesBlankPackageName(String pkg) {
        var info = new ApkInfo(pkg, "1.0", 1, "21", "35",
            List.of(), List.of(), "Blank", null);
        var output = ApkReader.printInfo(info);
        assertTrue(output.contains("Package: " + pkg));
    }

    @Test
    void read_throwsIOExceptionForNonExistentFile(@TempDir Path tempDir) {
        var nonExistent = tempDir.resolve("no-such.apk");
        var exception = assertThrows(IOException.class, () -> ApkReader.read(nonExistent));
        assertNotNull(exception.getMessage());
    }

    @Test
    void read_throwsIOExceptionForDirectory(@TempDir Path tempDir) {
        var exception = assertThrows(IOException.class, () -> ApkReader.read(tempDir));
        assertNotNull(exception.getMessage());
    }

    @Test
    void read_throwsIOExceptionForEmptyFile(@TempDir Path tempDir) throws IOException {
        var emptyFile = tempDir.resolve("empty.apk");
        java.nio.file.Files.createFile(emptyFile);
        var exception = assertThrows(IOException.class, () -> ApkReader.read(emptyFile));
        assertNotNull(exception.getMessage());
    }

    @Test
    void read_throwsIOExceptionForInvalidZip(@TempDir Path tempDir) throws IOException {
        var invalid = tempDir.resolve("invalid.apk");
        java.nio.file.Files.writeString(invalid, "not-a-zip-file");
        var exception = assertThrows(IOException.class, () -> ApkReader.read(invalid));
        assertNotNull(exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ".txt", "noextension"})
    void read_throwsIOExceptionForInvalidExtensionFiles(@TempDir Path tempDir, String name) throws IOException {
        var file = tempDir.resolve("test" + name);
        java.nio.file.Files.writeString(file, "some content");
        var exception = assertThrows(IOException.class, () -> ApkReader.read(file));
        assertNotNull(exception.getMessage());
    }
}
