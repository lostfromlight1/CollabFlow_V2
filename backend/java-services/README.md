# Java services (multi-module Gradle)

Spring Boot **4.1.1**, JDK **21**, Gradle **8.14.3** Wrapper.  
Versions: [docs/tech-stack.md](../docs/tech-stack.md).

## Modules

| Module | Port | Role |
|--------|------|------|
| `common` | — | Shared `ApiResponse` / `ErrorResponse` / exceptions |
| `api-gateway` | 8080 | Spring Cloud Gateway |
| `auth-service` | 8081 | Users, JWT, master admin |
| `platform-service` | 8082 | Org / workspace / project |
| `task-service` | 8083 | Tasks / workflows / sprints |
| `user-service` | 8084 | Profiles + admin user CRUD (Phase 2) |

**How to run:** [docs/running.md](../docs/running.md).

**Package layout (business services):** `controller`, `service`/`impl`, `dto`, `repository`, `model`, `config`, `exception` — see `.cursor/rules/java-spring.mdc`.

## Why one Gradle build?

All Java services share the same Boot/Java version. From `java-services/` you can build everything or run one module.

## Secrets (do not put in YAML)

`API_KEY` and `JWT_SECRET` live only in the **repo-root** `.env` (gitignored). Copy from `.env.example` once:

```powershell
# from repo root
Copy-Item .env.example .env
```

`application.yml` uses `${API_KEY}` / `${JWT_SECRET}` with **no** defaults.  
`gradlew …:bootRun` loads that `.env` into the process (OS env vars already set win).  
IDE run configs that are not Gradle `bootRun` must set the same two variables themselves.

## Commands (from this folder)

```bash
# Build all
./gradlew build          # Linux/macOS
gradlew.bat build        # Windows

# Run one service (reads repo-root .env)
./gradlew :auth-service:bootRun
./gradlew :platform-service:bootRun
./gradlew :user-service:bootRun
./gradlew :task-service:bootRun
./gradlew :api-gateway:bootRun
```

## OpenAPI / Swagger

Each business service exposes springdoc (direct port; gateway does not proxy Swagger UI):

| Module | Swagger UI | OpenAPI JSON |
|--------|------------|--------------|
| auth-service | http://localhost:8081/swagger-ui.html | `/v3/api-docs` |
| platform-service | http://localhost:8082/swagger-ui.html | `/v3/api-docs` |
| task-service | http://localhost:8083/swagger-ui.html | `/v3/api-docs` |
| user-service | http://localhost:8084/swagger-ui.html | `/v3/api-docs` |

Authorize with `X-API-Key` + Bearer where annotated. Route catalog: [docs/services-mvp.md](../docs/services-mvp.md).

## Smoke

Full start order + health: [docs/running.md](../docs/running.md).  
Demo: [docs/e2e-demo.md](../docs/e2e-demo.md).

## Conventions

- Packages: `com.collabflow.<service>`
- REST under `/api/v1/...`
- Controllers: `@CurrentUserId` + `@ApiId`
- Actuator: `/actuator/health`
- See `.cursor/rules/java-spring.mdc`
