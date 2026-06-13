# Module: miniApkReader

Android APK metadata extraction library.

## Dependencies

- `net.dongliu:apk-parser:2.6.10`

## API

### ApkReader

| Method | Description |
|--------|-------------|
| `read(Path apkPath)` | Parses an APK and returns an `ApkInfo` record |
| `printInfo(ApkInfo info)` | Formats APK info as a human-readable string |

### ApkInfo Record

| Field | Type | Description |
|-------|------|-------------|
| `packageName` | `String` | Android package name (e.g. `cu.xetid.apk.enzona`) |
| `versionName` | `String` | Human-readable version (e.g. `1.9.10.221002`) |
| `versionCode` | `long` | Internal version code |
| `minSdkVersion` | `String` | Minimum SDK version (e.g. `19`) |
| `targetSdkVersion` | `String` | Target SDK version (e.g. `33`) |
| `permissions` | `List<String>` | Required permissions |
| `features` | `List<String>` | Required hardware features |
| `label` | `String` | App display name |
| `icon` | `String` | Icon resource path |

### Usage

```java
ApkInfo info = ApkReader.read(Path.of("app.apk"));
System.out.println("Package: " + info.packageName());
System.out.println("Version: " + info.versionName());
```

### CLI (ApkInfoExtractor)

```bash
mvn compile exec:java -Dexec.mainClass="io.github.blacknoize404.miniApkReader.ApkInfoExtractor" -Dexec.args="path/to/app.apk"
```

Or via fat JAR:

```bash
java -cp miniApkReader/target/classes:$(find ~/.m2 -name 'apk-parser-2.6.10.jar') \
  io.github.blacknoize404.miniApkReader.ApkInfoExtractor app.apk

---

[← Previous](miniStaticServer.en.md) · [Home](../index.en.md)  
[🇪🇸 Español](miniApkReader.md) · [🇬🇧 English](miniApkReader.en.md)
```
