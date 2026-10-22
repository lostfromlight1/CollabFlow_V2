# common

Shared Java library for all CollabFlow Spring services.

- `ApiResponse<T>` / `ErrorResponse` — envelopes
- `BusinessException` + `ErrorCode` + `GlobalExceptionHandler`
- `ApiKeyFilter` + `ApiKeyProperties` — `X-API-Key` on `/api/**`
- `OpenApiSupport` — shared Swagger security schemes

Each Boot app must scan this package, e.g.:

```java
@SpringBootApplication(scanBasePackages = {
    "com.collabflow.auth",
    "com.collabflow.common"
})
```

Config (every service) — secrets from env / repo-root `.env`, never literals in YAML:

```yaml
collabflow:
  security:
    api-key: ${API_KEY}
  jwt:
    secret: ${JWT_SECRET}
```
