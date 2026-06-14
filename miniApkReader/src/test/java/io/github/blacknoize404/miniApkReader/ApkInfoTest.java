package io.github.blacknoize404.miniApkReader;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApkInfoTest {

    @Test
    void record_createsInstanceWithAllFields() {
        var info = new ApkInfo("com.test", "1.0", 1, "21", "35",
            List.of("INTERNET"), List.of("camera"), "TestApp", "res/icon.png");
        assertEquals("com.test", info.packageName());
        assertEquals("1.0", info.versionName());
        assertEquals(1L, info.versionCode());
        assertEquals("21", info.minSdkVersion());
        assertEquals("35", info.targetSdkVersion());
        assertEquals(List.of("INTERNET"), info.permissions());
        assertEquals(List.of("camera"), info.features());
        assertEquals("TestApp", info.label());
        assertEquals("res/icon.png", info.icon());
    }

    @Test
    void record_equalsAndHashCode_sameValuesAreEqual() {
        var a = new ApkInfo("com.a", "1.0", 1, "21", "35",
            List.of("INTERNET"), List.of(), "A", null);
        var b = new ApkInfo("com.a", "1.0", 1, "21", "35",
            List.of("INTERNET"), List.of(), "A", null);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void record_equalsAndHashCode_differentValuesAreNotEqual() {
        var a = new ApkInfo("com.a", "1.0", 1, "21", "35",
            List.of("INTERNET"), List.of(), "A", null);
        var b = new ApkInfo("com.b", "1.0", 1, "21", "35",
            List.of("INTERNET"), List.of(), "A", null);
        assertNotEquals(a, b);
    }

    @Test
    void record_toString_containsPackageName() {
        var info = new ApkInfo("com.example", "2.0", 2, "26", "34",
            List.of(), List.of(), "Example", null);
        var str = info.toString();
        assertTrue(str.contains("com.example"));
        assertTrue(str.contains("2.0"));
    }

    @Test
    void record_acceptsNullLabel() {
        var info = new ApkInfo("com.null", "1.0", 1, null, null,
            List.of(), List.of(), null, null);
        assertNull(info.label());
        assertNull(info.minSdkVersion());
    }

    @Test
    void record_acceptsEmptyPermissionsAndFeatures() {
        var info = new ApkInfo("com.empty", "1.0", 1, "21", "35",
            List.of(), List.of(), "Empty", null);
        assertTrue(info.permissions().isEmpty());
        assertTrue(info.features().isEmpty());
    }

    @Test
    void record_immutablePermissionsList() {
        var perms = new java.util.ArrayList<>(List.of("INTERNET"));
        var info = new ApkInfo("com.test", "1.0", 1, "21", "35",
            perms, List.of(), "Test", null);
        perms.add("CAMERA");
        assertEquals(1, info.permissions().size());
        assertFalse(info.permissions().contains("CAMERA"));
    }

    @Test
    void record_immutableFeaturesList() {
        var feats = new java.util.ArrayList<>(List.of("camera"));
        var info = new ApkInfo("com.test", "1.0", 1, "21", "35",
            List.of(), feats, "Test", null);
        feats.add("nfc");
        assertEquals(1, info.features().size());
        assertFalse(info.features().contains("nfc"));
    }

    @Test
    void record_versionCodeSupportsLargeValues() {
        var info = new ApkInfo("com.large", "1.0", 9876543210L, "21", "35",
            List.of(), List.of(), "Large", null);
        assertEquals(9876543210L, info.versionCode());
    }

    @Test
    void record_handlesMultiplePermissions() {
        var perms = List.of("INTERNET", "CAMERA", "LOCATION", "STORAGE", "BLUETOOTH");
        var info = new ApkInfo("com.multi", "1.0", 1, "21", "35",
            perms, List.of(), "Multi", null);
        assertEquals(5, info.permissions().size());
        assertTrue(info.permissions().containsAll(perms));
    }

    @Test
    void record_handlesMultipleFeatures() {
        var feats = List.of("camera", "nfc", "bluetooth_le", "wifi", "microphone");
        var info = new ApkInfo("com.multi", "1.0", 1, "21", "35",
            List.of(), feats, "Multi", null);
        assertEquals(5, info.features().size());
        assertTrue(info.features().containsAll(feats));
    }

    @Test
    void record_identity_notEqualToNull() {
        var info = new ApkInfo("com.test", "1.0", 1, "21", "35",
            List.of(), List.of(), "Test", null);
        assertNotEquals(null, info);
    }

    @Test
    void record_identity_notEqualToDifferentType() {
        var info = new ApkInfo("com.test", "1.0", 1, "21", "35",
            List.of(), List.of(), "Test", null);
        assertNotEquals("someString", info);
    }
}
