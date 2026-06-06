# MiniJWS

A lightweight Java Web Server with modular architecture.

## Features

- HTTP/1.1 server implementation
- Modular design with Maven multi-module structure
- Built-in QR code generation (miniQR module)
- APK reading capabilities (miniApkReader module)
- Static file serving
- Request/Response handling with proper HTTP status codes

## Project Structure

```
MiniJWS/
├── pom.xml                 # Parent Maven POM
├── src/main/java/          # Main server source code
│   └── io/github/blacknoize404/miniJWS/
│       ├── HttpServer.java        # Main server entry point
│       ├── primitives/            # Core HTTP primitives
│       ├── requests/              # HTTP request handling
│       ├── responses/             # HTTP response handling
│       ├── headers/               # HTTP header parsing
│       ├── content/               # Content type handling
│       └── utilities/             # Utility classes
├── modules/
│   ├── miniQR/                  # QR code generation module
│   └── miniApkReader/           # APK reader module
├── data/                        # Static assets (HTML, CSS, images)
└── out/                         # Compiled output (ignored by git)
```

## Requirements

- Java 25+
- Maven 3.8+

## Building

```bash
# Build all modules
mvn clean install

# Build specific module
mvn clean install -pl modules/miniQR
mvn clean install -pl modules/miniApkReader
```

## Running

```bash
# Run the main server
java -cp out/production/MiniJWS io.github.blacknoize404.miniJWS.HttpServer

# Or using Maven exec plugin
mvn exec:java -Dexec.mainClass="io.github.blacknoize404.miniJWS.HttpServer"
```

## Modules

### miniQR
QR code generation library for creating QR codes programmatically.

### miniApkReader
Library for reading and parsing Android APK files.

## License

MIT License