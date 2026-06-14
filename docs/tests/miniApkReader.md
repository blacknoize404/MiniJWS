# miniApkReader — Unit Tests

## Test Classes

| Test Class | Source Under Test | Lines of Test |
|------------|-------------------|---------------|
| `ApkInfoTest.java` | `ApkInfo.java` (record) | 19 tests |
| `ApkReaderTest.java` | `ApkReader.java` | 15 tests |
| `ApkInfoExtractorTest.java` | `AppInfoExtractor.java` (CLI) | 6 tests |

---

## ApkInfoTest

Evaluates the `ApkInfo` record, a Java 25+ `record` that holds all extracted APK metadata.

| Test Method | What it evaluates |
|-------------|-------------------|
| `record_createsInstanceWithAllFields` | All 9 record components (`packageName`, `versionName`, `versionCode`, `minSdkVersion`, `targetSdkVersion`, `permissions`, `features`, `label`, `icon`) are correctly stored and returned via accessor methods. |
| `record_equalsAndHashCode_sameValuesAreEqual` | Two `ApkInfo` instances with identical component values are equal (`equals()`) and produce the same hash code (`hashCode()`). |
| `record_equalsAndHashCode_differentValuesAreNotEqual` | Two `ApkInfo` instances differing in one field (e.g. `packageName`) are not equal. |
| `record_toString_containsPackageName` | The auto-generated `toString()` includes the package name and version name. |
| `record_acceptsNullLabel` | `null` is accepted for `label`, `minSdkVersion`, `targetSdkVersion`, and `icon` without throwing `NullPointerException`. |
| `record_acceptsEmptyPermissionsAndFeatures` | Empty lists for `permissions` and `features` are valid. |
| `record_immutablePermissionsList` | The list passed to the constructor is defensively copied (changes to the original list do not affect the record). |
| `record_immutableFeaturesList` | Same defensive copy guarantee for the `features` list. |
| `record_versionCodeSupportsLargeValues` | `versionCode` (a `long`) supports values larger than `Integer.MAX_VALUE`. |
| `record_handlesMultiplePermissions` | A list with 5 permissions is correctly stored and all are retrievable. |
| `record_handlesMultipleFeatures` | A list with 5 features is correctly stored and all are retrievable. |
| `record_identity_notEqualToNull` | `equals(null)` returns `false` (no `NullPointerException`). |
| `record_identity_notEqualToDifferentType` | `equals()` on a non-`ApkInfo` object returns `false`. |

---

## ApkReaderTest

Evaluates the `ApkReader` utility class — specifically the `printInfo()` formatting method and the `read()` error paths.

| Test Method | What it evaluates |
|-------------|-------------------|
| `printInfo_containsAllSections` | The output string contains all expected sections: header, package, version, SDK, label, icon, permissions (with indentation), features (with indentation). |
| `printInfo_withNullLabelAndIcon` | `null` label and icon are printed as the literal string `"null"` without crashing. |
| `printInfo_withEmptyPermissionsAndFeatures_printsHeadersStill` | The "Permissions:" and "Features:" section headers appear even when the lists are empty. |
| `printInfo_usesNewlineSeparators` | The output is multi-line (at least 9 lines) when there is one permission and one feature. |
| `printInfo_withLargeVersionCode` | Large `versionCode` values (e.g. 9876543210) are formatted correctly. |
| `printInfo_withManyPermissions_indentsEach` | Each permission in a multi-element list is indented with `"  - "`. |
| `printInfo_withManyFeatures_indentsEach` | Each feature in a multi-element list is indented with `"  - "`. |
| `printInfo_handlesBlankPackageName` | Edge-case package names (`null`, empty, whitespace) are handled without exception. |
| `read_throwsIOExceptionForNonExistentFile` | A non-existent file path throws `IOException`, not a generic runtime exception. |
| `read_throwsIOExceptionForDirectory` | A directory path (not a file) throws `IOException`. |
| `read_throwsIOExceptionForEmptyFile` | An empty file (0 bytes) throws `IOException` because it is not a valid APK/ZIP. |
| `read_throwsIOExceptionForInvalidZip` | A file containing plain text (not a valid ZIP) throws `IOException`. |
| `read_throwsIOExceptionForInvalidExtensionFiles` | Files with atypical names (empty name, `.txt` extension, no extension) throw `IOException`. |

Note: `read()` is tested through its error contract since it delegates to the third-party library `net.dongliu:apk-parser`. A valid APK integration test would require a real `.apk` file as a test resource.

---

## ApkInfoExtractorTest

Evaluates the CLI entry point `ApkInfoExtractor.main()`.

| Test Method | What it evaluates |
|-------------|-------------------|
| `main_noArgs_printsUsage` | When invoked with no arguments, the usage message is printed to stdout. |
| `main_fileNotFound_printsError` | When the argument is a non-existent file, an error is printed to stderr containing the words "not found". |
| `main_fileNotFound_messageContainsPath` | The error message includes the path that was passed as argument. |
| `main_invalidApkFile_printsErrorMessage` | When a real file that is not a valid APK is passed, the error "Error reading APK" appears on stderr. |
| `main_handlesRelativePath` | Relative paths (e.g. `../pom.xml`) are handled without crashing. |
| `main_handlesWindowsPath` | Windows-style paths (e.g. `C:\...`) are handled without crashing. |

---

## Running the Tests

```bash
cd miniApkReader
mvn test
```

Expected output: `BUILD SUCCESS` with 40 tests run (19 + 15 + 6), all passing.
