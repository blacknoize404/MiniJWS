# Pruebas Unitarias

## ¿Qué es una Prueba Unitaria?

Una **prueba unitaria** es un fragmento de código automatizado que verifica el comportamiento de la parte más pequeña comprobable de un programa — una **unidad** — de forma aislada. En programación orientada a objetos, una unidad es típicamente una clase o método individual.

Las pruebas unitarias son escritas y ejecutadas por desarrolladores, no por usuarios o testers. Son el primer nivel de prueba en la **pirámide de pruebas** (unitaria → integración → extremo a extremo).

### Características de una Buena Prueba Unitaria

| Característica | Descripción |
|----------------|-------------|
| **Aislada** | Prueba solo una unidad; las dependencias externas (bases de datos, sistema de archivos, red) se reemplazan con dobles de prueba (mocks, stubs, fakes). |
| **Rápida** | Se ejecuta en milisegundos. Un proyecto con miles de pruebas unitarias debería ejecutarse en segundos. |
| **Determinista** | Dada la misma entrada, siempre produce el mismo resultado. Sin incoherencias. |
| **Autoverificable** | Pasa o falla automáticamente; sin inspección humana de la salida. |
| **Legible** | El nombre y el cuerpo de la prueba expresan claramente el comportamiento que se verifica. |
| **Enfocada** | Cada prueba verifica un único escenario o comportamiento. |

### El Patrón AAA

Toda prueba unitaria sigue tres fases:

1. **Arrange (Preparar)** — establecer los datos de prueba y las precondiciones.
2. **Act (Actuar)** — invocar la unidad bajo prueba.
3. **Assert (Afirmar)** — verificar que el resultado coincide con las expectativas.

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

## ¿Por Qué Escribir Pruebas Unitarias?

1. **Prevención de regresiones** — cuando cambias código, las pruebas detectan lo que rompiste accidentalmente.
2. **Documentación** — las pruebas describen cómo se *supone* que el código debe comportarse, sirviendo como documentación ejecutable.
3. **Retroalimentación de diseño** — el código difícil de probar a menudo tiene mal diseño (acoplamiento fuerte, baja cohesión). Escribir pruebas fomenta una mejor arquitectura.
4. **Red de seguridad para refactorización** — puedes reestructurar código con la confianza de que el comportamiento existente se preserva.
5. **Depuración más rápida** — una prueba fallida señala exactamente qué unidad y escenario está roto.

---

## Dobles de Prueba (Mocks, Stubs, Fakes)

Cuando una unidad depende de sistemas externos, los reemplazamos con **dobles de prueba** para mantener el aislamiento:

| Tipo | Propósito |
|------|-----------|
| **Stub** | Devuelve datos preconfigurados sin lógica real. |
| **Mock** | Verifica que ciertas interacciones (llamadas a métodos) ocurrieron. |
| **Fake** | Una implementación funcional ligera (p.ej., una base de datos en memoria). |
| **Spy** | Envuelve un objeto real y registra interacciones. |

En MiniJWS, Mockito se usa para crear mocks y stubs para dependencias como el parser `ApkFile` de la librería `apk-parser`.

---

## Pruebas Unitarias en MiniJWS

MiniJWS usa **JUnit 5** (Jupiter) como framework de pruebas y **Mockito** para dobles de prueba, integrados mediante el **Maven Surefire Plugin**.

### Estructura de Pruebas

```
nombre-módulo/
└── src/test/java/
    └── paquete/
        ├── module-info.java          # Descriptor de módulo de prueba (módulo abierto)
        ├── NombreClaseTest.java      # Pruebas para NombreClase
        └── ...
```

- Las clases de prueba se nombran `{NombreClase}Test.java` por convención.
- Cada método de prueba sigue el patrón `{comportamiento}_{escenario}_{resultadoEsperado}`.
- Las pruebas se colocan en el **mismo paquete** que el código que prueban, dándoles acceso a miembros package-private.

### Ejemplo: Probando un Record

```java
@Test
void record_equalsAndHashCode_sameValuesAreEqual() {
    var a = new ApkInfo("com.a", "1.0", 1, ...);
    var b = new ApkInfo("com.a", "1.0", 1, ...);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
}
```

### Ejemplo: Probando una Ruta de Error

```java
@Test
void read_throwsIOExceptionForNonExistentFile(@TempDir Path tempDir) {
    var nonExistent = tempDir.resolve("no-such.apk");
    assertThrows(IOException.class, () -> ApkReader.read(nonExistent));
}
```

### Ejemplo: Probando Salida CLI

```java
@Test
void main_noArgs_printsUsage() {
    ApkInfoExtractor.main(new String[]{});
    var output = outContent.toString();
    assertTrue(output.contains("Usage"));
}
```

---

## Ejecutar Pruebas

```bash
# Ejecutar todas las pruebas en un módulo
mvn test -f module/pom.xml

# Ejecutar una clase de prueba individual
mvn test -f module/pom.xml -Dtest=ApkInfoTest

# Ejecutar un método de prueba específico
mvn test -f module/pom.xml -Dtest=ApkInfoTest#record_createsInstanceWithAllFields
```

Para ver la salida detallada de pruebas, añade el flag `-X` para logging de depuración.

---

## Lecturas Adicionales

- [Guía de Usuario de JUnit 5](https://junit.org/junit5/docs/current/user-guide/)
- [Referencia de Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Martin Fowler — Prueba Unitaria](https://martinfowler.com/bliki/UnitTest.html)
- [Pirámide de Pruebas](https://martinfowler.com/articles/practical-test-pyramid.html)

[← Anterior](http-protocol.md) · [Siguiente →](index.md)  
[🇪🇸 Español](unit-testing.md) · [🇬🇧 English](unit-testing.en.md)
