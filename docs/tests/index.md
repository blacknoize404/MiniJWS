# Test Documentation

This directory documents the unit tests for each module of the MiniJWS project.

## Modules

| Module | Test Classes | Tests | What is tested |
|--------|-------------|-------|----------------|
| [miniJWS-core](miniJWS-core.md) | 16 test classes | ~130+ | Server, HTTP parsing, middleware, headers, file serving, status codes |
| [miniQR](miniQR.md) | 1 test class | 12 | QR code generation (image + SVG) |
| [miniStaticServer](miniStaticServer.md) | 2 test classes | 15 | Static site serving, template injection, QR placeholders |
| [miniApkReader](miniApkReader.md) | 3 test classes | 40 | APK metadata record, APK reading/formatting, CLI |

## Running Tests

```bash
# Run all tests for a specific module
mvn test -f <module>/pom.xml
```

## Test Framework

- **JUnit 5** (Jupiter) — test execution and assertions
- **JUnit 5 Parameterized Tests** — data-driven testing
- **Mockito** — test doubles and behaviour verification
- **Maven Surefire Plugin** — test runner integration

All tests are located in `src/test/java/` under the corresponding module directory.
