# Módulo: miniApkReader

Biblioteca de extracción de metadatos de APK Android.

## Dependencias

- `net.dongliu:apk-parser:2.6.10`

## API

### ApkReader

| Método | Descripción |
|--------|-------------|
| `read(Path apkPath)` | Parsea un APK y devuelve un record `ApkInfo` |
| `printInfo(ApkInfo info)` | Formatea la información del APK como cadena legible |

### Record ApkInfo

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `packageName` | `String` | Nombre del paquete Android (p.ej. `cu.xetid.apk.enzona`) |
| `versionName` | `String` | Versión legible (p.ej. `1.9.10.221002`) |
| `versionCode` | `long` | Código de versión interno |
| `minSdkVersion` | `String` | Versión mínima de SDK (p.ej. `19`) |
| `targetSdkVersion` | `String` | Versión objetivo de SDK (p.ej. `33`) |
| `permissions` | `List<String>` | Permisos requeridos |
| `features` | `List<String>` | Características de hardware requeridas |
| `label` | `String` | Nombre visible de la app |
| `icon` | `String` | Ruta del recurso de icono |

### Uso

```java
ApkInfo info = ApkReader.read(Path.of("app.apk"));
System.out.println("Paquete: " + info.packageName());
System.out.println("Versión: " + info.versionName());
```

### CLI (ApkInfoExtractor)

```bash
mvn compile exec:java -Dexec.mainClass="io.github.blacknoize404.miniApkReader.ApkInfoExtractor" -Dexec.args="ruta/a/app.apk"
```

O mediante fat JAR:

```bash
java -cp miniApkReader/target/classes:$(find ~/.m2 -name 'apk-parser-2.6.10.jar') \
  io.github.blacknoize404.miniApkReader.ApkInfoExtractor app.apk

---

[← Anterior](miniStaticServer.md) · [Inicio](../index.md)  
[🇪🇸 Español](miniApkReader.md) · [🇬🇧 English](miniApkReader.en.md)
```
