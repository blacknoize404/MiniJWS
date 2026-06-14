# Knowledge Base

Deep-dive documentation covering the design, architecture, patterns, and implementation details of the MiniJWS project.

This project is intended as a resource for university students to see concepts learned in their degree applied in a real project: here we document not only which Java classes are used and why, but also the design decisions made and the patterns employed, with their rationale.

## Contents

| File | Description |
|------|-------------|
| [Patterns](patterns.en.md) | Design patterns used (Builder, Middleware/CoR, Strategy, etc.) |
| [Decisions](decisions.en.md) | Key design decisions and their rationale |
| [Core Classes](classes-core.en.md) | HttpServer, HttpRequest, HttpResponse, HttpDecoder, HttpEncoder, StaticFileHandler |
| [Middleware Classes](classes-middleware.en.md) | AccessLogMiddleware, CorsMiddleware, GzipMiddleware, RateLimitMiddleware |
| [Support Classes](classes-support.en.md) | ContentType, HttpMethod, HttpStatusCode, HttpDecoder internals, other modules |
| [HTTP Protocol Fundamentals](http-protocol.en.md) | HTTP protocol explained from scratch, with references to MiniJWS code |
| [Java API Reference](java-api/index.en.md) | Comprehensive Java language and API docs used in the project |
| [Unit Testing](unit-testing.en.md) | What unit tests are, the AAA pattern, test doubles, and how tests are structured in MiniJWS |

---

[Next →](http-protocol.en.md)  
[🇪🇸 Español](index.md) · [🇬🇧 English](index.en.md)
