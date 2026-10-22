# common

**Type:** Shared Java **library** (JAR) — **not** a deployable service (no port; `bootJar` disabled)  
**Package:** `com.collabflow.common`  
**MVP:** Complete · **Phase 2:** TODO checklist below

Consumed by servlet MVC services: **auth**, **platform**, **task** (and future **user-service**).  
**api-gateway** is WebFlux and keeps its own API-key / JWT filters; it does not rely on this module’s servlet stack.  
**Ordered waves:** [docs/phase-2.md](../../docs/phase-2.md).

### See also / Depends on

| Topic | Owner |
|-------|--------|
| Emit / forward `X-Request-Id` | [api-gateway](../api-gateway/README.md) Wave 0 |
| Adopt `@CurrentOrganizationId` | platform + task (Wave 2) |
| Unified 401/403 JSON at edge | gateway-local (WebFlux) — not this servlet handler |

## What it does

Cross-cutting building blocks so every Java API service returns the same envelopes, maps exceptions the same way, checks `X-API-Key`, injects the JWT user id, and tags logs with `@ApiId`. Prefer extending **common** over copy-pasting filters/handlers into each service.

## Features (MVP — shipped)

| Area | What it provides |
|------|------------------|
| API envelopes | `ApiResponse<T>`, `ErrorResponse` (+ field errors) |
| Exceptions | `BusinessException`, `ConflictException`, `UnauthorizedException`, `ForbiddenException`, `ResourceNotFoundException`, `ErrorCode` |
| Global handler | `GlobalExceptionHandler` — business errors; Bean Validation; **bad JSON/UUID → 400**; path/query type mismatch → 400; unexpected → logged 500 |
| API key | `ApiKeyFilter`, `ApiKeyProperties`, `CommonSecurityConfiguration` — require `X-API-Key` on `/api/**` |
| Current user | `@CurrentUserId` — JWT principal as `UUID` |
| ApiId / MDC | `@ApiId`, `ApiIdMdcInterceptor`, `MdcKeys`, `CommonWebMvcConfiguration` |
| Kafka envelope | `DomainEvent` factory (event id, type, timestamp, payload) |
| OpenAPI | `OpenApiSupport` — shared API-key + Bearer security scheme names |

### MVP learning notes

| Piece | What / why |
|-------|------------|
| **One error shape** | Clients and Postman always see `success` / `errorCode` / `message` / `path` |
| **Defense in depth** | Gateway checks API key + JWT; each service still checks API key via this filter |
| **compileOnly deps** | Common compiles against web/security/json; each Boot app brings them at runtime |

## Phase 2 — TODO

Library improvements used by all Java services. Implement here first, then adopt in auth/platform/task/user.

### Must / high value

- [ ] **Shared JWT helper** — HS256 validate + `sub` → UUID (reduce duplicated `JwtService` in auth/platform/task); secrets stay in each service’s config
- [x] **Correlation / request ID** — filter + MDC `requestId`; align header name with gateway `X-Request-Id`
- [x] **Expand `GlobalExceptionHandler`** — e.g. `DataIntegrityViolationException` → 409/400; `ConstraintViolationException`; keep field-error shape stable
- [x] **Pagination DTOs** — shared `PageResponse<T>` + page/size/sort params for list APIs
- [x] **More tests** — UUID/type-mismatch; `PageResponse`; illegal-arg mapping

### Align with admin / org Phase 2

- [x] **`@CurrentOrganizationId`** — resolve `X-Organization-Id` for platform/task when gateway forwards org context
- [x] **Document ErrorResponse contract** — servlet services use common handler; **WebFlux gateway** has its own 401/403 JSON

### Hardening / portfolio

- [ ] **Single envelope source of truth** — stick to `ErrorResponse` (skip RFC 7807 unless product requires it); document the choice
- [ ] **Kafka publish helper** — thin “after commit + DomainEvent” helper **without** hard-depending on spring-kafka in common (optional interface / docs pattern)
- [ ] **MDC key set** — document `apiId`, `userId`, `requestId`, `organizationId` for Java (+ Go parity later)

### Stretch / later

- `common-webflux` module for gateway (or keep gateway-local)  
- Shared Redis rate-limiter component (auth has its own today)  
- OpenTelemetry starter wiring  

## How consuming services use it

1. Depend on `project(':common')` in Gradle.  
2. Scan the package:

```java
@SpringBootApplication(scanBasePackages = {
    "com.collabflow.<service>",
    "com.collabflow.common"
})
```

3. Controllers: `@CurrentUserId` + `@ApiId("AREA-NNN")` — see `.cursor/rules/java-spring.mdc` and [services-mvp.md](../../docs/services-mvp.md).

## Config

Secrets from repo-root `.env` — **never** literals in YAML:

```yaml
collabflow:
  security:
    api-key: ${API_KEY}
```

`JWT_SECRET` remains **per-service** until a shared JWT helper is introduced (Phase 2 TODO above).
