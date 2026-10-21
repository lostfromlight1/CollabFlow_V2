# Java services (multi-module Gradle)

Spring Boot **4.1.1**, JDK **21**, Gradle **8.14.3** Wrapper.  
Versions: [docs/tech-stack.md](../docs/tech-stack.md).

## Modules

| Module | Port | Role |
|--------|------|------|
| `common` | — | Shared `ApiResponse` / `ErrorResponse` / exceptions |
| `api-gateway` | 8080 | Spring Cloud Gateway |
| `auth-service` | 8081 | Users, JWT |
| `platform-service` | 8082 | Org / workspace / project (Week 5+) |
| `task-service` | 8083 | Tasks / workflows (Week 8+) |

**Package layout (business services):** `controller`, `service`/`impl`, `dto`, `repository`, `model`, `config`, `exception` — see `.cursor/rules/java-spring.mdc`.

## Why one Gradle build?

All Java services share the same Boot/Java version. From `java-services/` you can build everything or run one module.

## Commands (from this folder)

```bash
# Build all
./gradlew build          # Linux/macOS
gradlew.bat build        # Windows

# Run one service
./gradlew :auth-service:bootRun
./gradlew :platform-service:bootRun
./gradlew :task-service:bootRun
./gradlew :api-gateway:bootRun
```

## Week 4 smoke (gateway + auth)

1. Start infra (`infrastructure/` — Postgres, Redis, Kafka).
2. Terminal A: `gradlew.bat :auth-service:bootRun`
3. Terminal B: `gradlew.bat :api-gateway:bootRun`
4. Use header `X-API-Key` (see `.env.example`). Prefer `:8080` (gateway).

```bash
curl http://localhost:8080/api/v1/auth/health -H "X-API-Key: $API_KEY"
# register → login → GET /api/v1/auth/me with Bearer (see auth-service/README.md)
```

## Conventions

- Packages: `com.collabflow.<service>`
- REST under `/api/v1/...`
- Actuator: `/actuator/health`
- See `.cursor/rules/java-spring.mdc`
