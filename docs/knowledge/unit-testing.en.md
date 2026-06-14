# Unit Testing

## What is a Unit Test?

A **unit test** is an automated piece of code that verifies the behaviour of the smallest testable part of a program — a **unit** — in isolation. In object-oriented programming, a unit is typically a single class or method.

Unit tests are written and executed by developers, not by users or testers. They are the first level of testing in the **test pyramid** (unit → integration → end-to-end).

### Characteristics of a Good Unit Test

| Characteristic | Description |
|---------------|-------------|
| **Isolated** | Tests only one unit; external dependencies (databases, filesystem, network) are replaced with test doubles (mocks, stubs, fakes). |
| **Fast** | Executes in milliseconds. A project with thousands of unit tests should run in seconds. |
| **Deterministic** | Given the same input, always produces the same result. No flakiness. |
| **Self-verifying** | Passes or fails automatically; no human inspection of output. |
| **Readable** | The test name and body clearly express the behaviour being verified. |
| **Focused** | Each test verifies a single scenario or behaviour. |

### The AAA Pattern

Every unit test follows three phases:

1. **Arrange** — set up the test data and preconditions.
2. **Act** — invoke the unit under test.
3. **Assert** — verify the outcome matches expectations.

```java
@Test
void record_createsInstanceWithAllFields() {
    // Arrange
    var perms = List.of("INTERNET");

    // Act
    var info = new ApkInfo("com.test", "1.0", 1, "21", "35",
        perms, List.of(), "TestApp", "res/icon.png");

    // Assert
    assertEquals("com.test", info.packageName());
    assertEquals("1.0", info.versionName());
    assertEquals(1L, info.versionCode());
}
```

---

## Why Write Unit Tests?

1. **Regression prevention** — when you change code, tests catch what you accidentally broke.
2. **Documentation** — tests describe how the code is *supposed* to behave, serving as executable documentation.
3. **Design feedback** — code that is hard to test often has poor design (tight coupling, low cohesion). Writing tests encourages better architecture.
4. **Refactoring safety net** — you can restructure code with confidence that existing behaviour is preserved.
5. **Faster debugging** — a failing test pinpoints exactly which unit and scenario is broken.

---

## Test Doubles (Mocks, Stubs, Fakes)

When a unit depends on external systems, we replace them with **test doubles** to maintain isolation:

| Type | Purpose |
|------|---------|
| **Stub** | Returns pre-configured data without real logic. |
| **Mock** | Verifies that certain interactions (method calls) occurred. |
| **Fake** | A lightweight working implementation (e.g. an in-memory database). |
| **Spy** | Wraps a real object and records interactions. |

In MiniJWS, Mockito is used to create mocks and stubs for dependencies such as the `ApkFile` parser from the `apk-parser` library.

---

## Unit Tests in MiniJWS

MiniJWS uses **JUnit 5** (Jupiter) as the test framework and **Mockito** for test doubles, integrated via the **Maven Surefire Plugin**.

### Test Structure

```
module-name/
└── src/test/java/
    └── package/
        ├── module-info.java          # Test module descriptor (open module)
        ├── ClassNameTest.java        # Tests for ClassName
        └── ...
```

- Test classes are named `{ClassName}Test.java` by convention.
- Each test method follows the pattern `{behaviour}_{scenario}_{expectedOutcome}`.
- Tests are placed in the **same package** as the code they test, giving them access to package-private members.

### Example: Testing a Record

```java
@Test
void record_equalsAndHashCode_sameValuesAreEqual() {
    var a = new ApkInfo("com.a", "1.0", 1, ...);
    var b = new ApkInfo("com.a", "1.0", 1, ...);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
}
```

### Example: Testing an Error Path

```java
@Test
void read_throwsIOExceptionForNonExistentFile(@TempDir Path tempDir) {
    var nonExistent = tempDir.resolve("no-such.apk");
    assertThrows(IOException.class, () -> ApkReader.read(nonExistent));
}
```

### Example: Testing CLI Output

```java
@Test
void main_noArgs_printsUsage() {
    ApkInfoExtractor.main(new String[]{});
    var output = outContent.toString();
    assertTrue(output.contains("Usage"));
}
```

---

## Running Tests

```bash
# Run all tests in a module
mvn test -f module/pom.xml

# Run a single test class
mvn test -f module/pom.xml -Dtest=ApkInfoTest

# Run a specific test method
mvn test -f module/pom.xml -Dtest=ApkInfoTest#record_createsInstanceWithAllFields
```

To see detailed test output, add the `-X` flag for debug logging.

---

## Further Reading

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Reference](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Martin Fowler — Unit Test](https://martinfowler.com/bliki/UnitTest.html)
- [Test Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html)

[← Previous](http-protocol.en.md) · [Next →](index.en.md)  
[🇪🇸 Español](unit-testing.md) · [🇬🇧 English](unit-testing.en.md)
