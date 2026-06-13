# miniApkReader

![Java 25+](https://img.shields.io/badge/Java-25+-orange?logo=openjdk&logoColor=white)
![Maven 3.8+](https://img.shields.io/badge/Maven-3.8+-C71A36?logo=apachemaven&logoColor=white)
![apk-parser 2.6.10](https://img.shields.io/badge/apk--parser-2.6.10-4B8BBE)

Android APK metadata extraction library.

## API

```java
ApkInfo info = ApkReader.read(Path.of("app.apk"));
System.out.println("Package: " + info.packageName());
System.out.println(ApkReader.printInfo(info));
```

### ApkInfo Record

| Field | Type | Description |
|-------|------|-------------|
| `packageName` | `String` | e.g. `com.example.app` |
| `versionName` | `String` | e.g. `1.0.0` |
| `versionCode` | `long` | Internal version code |
| `minSdkVersion` | `String` | Min SDK level |
| `targetSdkVersion` | `String` | Target SDK level |
| `permissions` | `List<String>` | Required permissions |
| `features` | `List<String>` | Required hardware features |
| `label` | `String` | App display name |
| `icon` | `String` | Icon resource path |

### CLI

```bash
java -jar miniApkReader.jar path/to/app.apk
```

## Dependencies

- `net.dongliu:apk-parser:2.6.10`

## Build

```bash
mvn clean install
```
