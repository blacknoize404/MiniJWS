# miniApkReader

Biblioteca de extracción de metadatos de APK Android.

## API

```java
ApkInfo info = ApkReader.read(Path.of("app.apk"));
System.out.println("Paquete: " + info.packageName());
System.out.println(ApkReader.printInfo(info));
```

### Record ApkInfo

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `packageName` | `String` | p.ej. `com.example.app` |
| `versionName` | `String` | p.ej. `1.0.0` |
| `versionCode` | `long` | Código de versión interno |
| `minSdkVersion` | `String` | Nivel mínimo de SDK |
| `targetSdkVersion` | `String` | Nivel objetivo de SDK |
| `permissions` | `List<String>` | Permisos requeridos |
| `features` | `List<String>` | Características de hardware requeridas |
| `label` | `String` | Nombre visible de la app |
| `icon` | `String` | Ruta del recurso de icono |

### CLI

```bash
java -jar miniApkReader.jar ruta/a/app.apk
```

## Dependencias

- `net.dongliu:apk-parser:2.6.10`

## Compilación

```bash
mvn clean install
```
