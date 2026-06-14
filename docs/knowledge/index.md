# Base de Conocimiento

Documentación detallada que cubre el diseño, la arquitectura, los patrones y los detalles de implementación del proyecto MiniJWS.

Este proyecto está pensado como un recurso para que estudiantes universitarios vean conceptos aprendidos en la carrera aplicados en un proyecto real: aquí se documentan no solo las clases de Java que se usan y por qué, sino también las decisiones de diseño que se tomaron y los patrones que se emplearon, con su justificación.

## Contenidos

| Archivo | Descripción |
|---------|-------------|
| [Patrones](patterns.md) | Patrones de diseño usados (Builder, Middleware/CoR, Strategy, etc.) |
| [Decisiones](decisions.md) | Decisiones clave de diseño y su justificación |
| [Clases del Núcleo](classes-core.md) | HttpServer, HttpRequest, HttpResponse, HttpDecoder, HttpEncoder, StaticFileHandler |
| [Clases de Middleware](classes-middleware.md) | AccessLogMiddleware, CorsMiddleware, GzipMiddleware, RateLimitMiddleware |
| [Clases de Soporte](classes-support.md) | ContentType, HttpMethod, HttpStatusCode, internos de HttpDecoder, otros módulos |
| [Fundamentos del Protocolo HTTP](http-protocol.md) | Explicación del protocolo HTTP desde cero, con referencias al código de MiniJWS |
| [Referencia de la API de Java](java-api/index.md) | Documentación completa del lenguaje Java y las API usadas en el proyecto |
| [Pruebas Unitarias](unit-testing.md) | Qué son las pruebas unitarias, el patrón AAA, dobles de prueba y cómo se estructuran las pruebas en MiniJWS |

---

[Siguiente →](http-protocol.md)  
[🇪🇸 Español](index.md) · [🇬🇧 English](index.en.md)
