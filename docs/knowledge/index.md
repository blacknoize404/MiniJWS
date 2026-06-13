# Knowledge Base

Deep-dive documentation covering the design, architecture, patterns, and implementation details of the MiniJWS project.

Este proyecto está pensado como un recurso para que estudiantes universitarios vean conceptos aprendidos en la carrera aplicados en un proyecto real: aquí se documentan no solo las clases de Java que se usan y por qué, sino también las decisiones de diseño que se tomaron y los patrones que se emplearon, con su justificación.

## Contents

| File | Description |
|------|-------------|
| [Patterns](patterns.md) | Design patterns used (Builder, Middleware/CoR, Strategy, etc.) |
| [Decisions](decisions.md) | Key design decisions and their rationale |
| [Core Classes](classes-core.md) | HttpServer, HttpRequest, HttpResponse, HttpDecoder, HttpEncoder, StaticFileHandler |
| [Middleware Classes](classes-middleware.md) | AccessLogMiddleware, CorsMiddleware, GzipMiddleware, RateLimitMiddleware |
| [Support Classes](classes-support.md) | ContentType, HttpMethod, HttpStatusCode, HttpDecoder internals, other modules |
| [HTTP Protocol Fundamentals](http-protocol.md) | Explicación del protocolo HTTP desde cero, con referencias al código de MiniJWS |
| [Java API Reference](java-api/index.md) | Comprehensive Java language and API docs used in the project |

---

[Siguiente →](http-protocol.md)  
[🇪🇸 Español](index.md) · [🇬🇧 English](index.md)
