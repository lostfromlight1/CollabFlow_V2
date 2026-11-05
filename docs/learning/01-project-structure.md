# 01 — Project structure

**Goal:** Know where each process lives, what it is built with, which port it listens on, and how to start it.

## Top-level layout

| Path | What lives there |
|------|------------------|
| `backend/java-services/` | Gradle multi-module: `common`, `api-gateway`, `auth-service`, `user-service`, `platform-service`, `task-service`. Modules listed in `backend/java-services/settings.gradle`. |
| `backend/go-services/` | `collab-service`, `realtime-service`, `document-service`, `email-service`. Each has `cmd/server/main.go` and `internal/`. |
| `backend/infrastructure/` | `docker-compose.yml` for Postgres, Redis, ZooKeeper, Kafka, MailHog. |
| `frontend/` | Next.js App Router. Calls the gateway. Not a UI style guide. |
| `docs/` | Contracts. This `learning/` folder explains them. |
| `scripts/` | `dev-backend.cmd` / `dev-backend.ps1` start infra and every app. |
| `postman/` | Auth and platform collections. See `postman/README.md`. |

Java package layout (from `.cursor/rules/java-spring.mdc`, matching the services): `config`, `controller`, `dto`, `service` plus `service/impl`, `repository`, `model`, `security`, and `event` where Kafka is used. Shared types live in `com.collabflow.common`.

Go layout: `cmd/server/main.go` wires config, database, handlers, and Kafka. Business code is under `internal/`.

## Versions

Pinned in [docs/tech-stack.md](../tech-stack.md) and checked in build files.

| Piece | Version | Where checked |
|-------|---------|----------------|
| JDK | 21 | `backend/java-services/build.gradle` toolchain |
| Spring Boot | 4.1.1 | same file, plugin version |
| Gradle | 8.14.3 | `backend/java-services/gradle/wrapper/gradle-wrapper.properties` |
| Spring Cloud | 2025.1.3 | [docs/tech-stack.md](../tech-stack.md); gateway module uses the BOM |
| Go | 1.27 | `backend/go-services/collab-service/go.mod` (`go 1.27`) |
| Gin | 1.12.0 | same `go.mod` |
| GORM | 1.31.0 | same `go.mod` |
| kafka-go | 0.4.47 | same `go.mod` |
| Next.js | 16.4.2 | `frontend/package.json` |
| React | 19.3.0 | `frontend/package.json` |

## Java services

Shared library `common` is not a process. `bootJar` is off. It provides `ApiResponse`, `PageResponse`, `ErrorResponse`, `GlobalExceptionHandler`, `ApiKeyFilter`, `@CurrentUserId`, `@CurrentOrganizationId`, `@ApiId`, and `DomainEvent`.

| Service | Main class | Port | Role |
|---------|------------|------|------|
| api-gateway | `com.collabflow.gateway.ApiGatewayApplication` | 8080 | Edge. No controllers. Filters and route map. |
| auth-service | `com.collabflow.auth.AuthApplication` | 8081 | Login, JWT, refresh, email tokens, sessions, master-admin seed |
| platform-service | `com.collabflow.platform.PlatformApplication` | 8082 | Tenancy, RBAC, admin console |
| task-service | `com.collabflow.task.TaskApplication` | 8083 | Workflows, tasks, sprints |
| user-service | `com.collabflow.user.UserApplication` | 8084 | Profile and admin user CRUD on shared `users` |

Ports are in each service `src/main/resources/application.yml`. Gateway upstream URLs default to `localhost` and those ports (`AUTH_SERVICE_URI`, `USER_SERVICE_URI`, `PLATFORM_SERVICE_URI`, `TASK_SERVICE_URI`, `COLLAB_SERVICE_URI`, `REALTIME_SERVICE_URI`, `DOCUMENT_SERVICE_URI`).

### What each Java service depends on

| Service | Notable starters |
|---------|------------------|
| api-gateway | Spring Cloud Gateway (WebFlux), Spring Security OAuth2 resource server. No JPA, Flyway, Kafka, or Redis. |
| auth-service | Web, JPA, Security, Flyway, Kafka, Redis, jjwt 0.12.6, springdoc 3.1.1, `:common` |
| user-service | Web, JPA, Security, Kafka, `:common`. No Flyway starter. `ddl-auto: validate`. |
| platform-service | Web, JPA, Security, Flyway, Kafka, `:common` |
| task-service | Web, JPA, Security, Flyway, Kafka, `:common` |

There is no `@PreAuthorize` on controllers. Authorization is path security plus service checks (`PermissionService`, `requireMembership`, `requireActiveMaster`).

### Java packages you will open often

| Service | Controllers |
|---------|-------------|
| auth | `AuthController` (`/api/v1/auth`), `AdminSessionController` (`/api/v1/admin`) |
| user | `UserController`, `AdminUserController`, `HealthController` |
| platform | `OrganizationController`, `WorkspaceController`, `ProjectController`, `RoleController`, plus admin controllers for orgs, announcements, flags, audit, stats |
| task | `TaskController`, `SprintController`, `WorkflowController`, `TaskTypeController`, `HealthController` |

## Go services

| Service | Module | Entry | Port env | Default port |
|---------|--------|-------|----------|--------------|
| collab-service | `github.com/collabflow/collab-service` | `cmd/server/main.go` | `COLLAB_HTTP_PORT` | 9081 |
| realtime-service | `github.com/collabflow/realtime-service` | `cmd/server/main.go` | `REALTIME_HTTP_PORT` | 9082 |
| document-service | `github.com/collabflow/document-service` | `cmd/server/main.go` | `DOCUMENT_HTTP_PORT` | 9083 |
| email-service | `github.com/collabflow/email-service` | `cmd/server/main.go` | `EMAIL_HTTP_PORT` | 9084 |

| Service | `internal/` packages | Extra libraries |
|---------|----------------------|-----------------|
| collab | config, database, model, repository, service, handler, middleware, kafka, platform, email | Gin, GORM, kafka-go, jwt/v5. No Redis. |
| realtime | acl, auth, config, database, handler, hub, kafka, redisx | Gin, GORM (read-only), kafka-go, go-redis/v9, gorilla/websocket |
| document | auth, config, database, handler, model, storage | Gin, GORM, jwt. No Kafka. |
| email | config, handler, kafka, service, smtp | Gin, kafka-go. No database. |

`backend/go-services/README.md` still describes document-service as stubs plus MinIO. The code stores files on local disk (`DOCUMENT_STORAGE_DIR` in `document-service/internal/config`). Treat the README line as stale.

## Frontend as an API caller

`frontend/package.json` scripts: `dev`, `build`, `start`, `lint`.

The HTTP client is `frontend/lib/api/client.ts`. It reads `NEXT_PUBLIC_API_BASE_URL` and `NEXT_PUBLIC_API_KEY`, sends `X-API-Key` on every call, adds `Authorization: Bearer` when the session has a token, and can add `X-Organization-Id`. A 401 can trigger one refresh against `POST /api/v1/auth/refresh`.

Domain modules next to the client:

| File | Calls |
|------|--------|
| `lib/api/auth.ts` | Login, register, refresh, sessions, org list and select, profile via `/users/me` |
| `lib/api/admin.ts` | Admin user and org directories, stats, audit, announcements, flags, impersonation |
| `lib/api/platform.ts` | Organizations, workspaces, projects, roles |
| `lib/api/tasks.ts` | Workflows, task types, tasks, sprints |
| `lib/api/collab.ts` | Invitations, comments, activity, notifications |
| `lib/api/documents.ts` | Upload, list, download |

App Router groups (URLs stay clean; groups are layouts): `(auth)` login and org pick, `(admin)` master-admin screens, `(app)` member product including workspaces, projects, tasks, and documents. The browser must not call ports 8081–8084 or 9081–9084. [docs/fe-contract.md](../fe-contract.md) sets the base URL to `http://localhost:8080`.

There is no `frontend/.env.example` in the tree. `frontend/README.md` tells you to copy an example into `frontend/.env.local`. Create that file locally with `NEXT_PUBLIC_API_BASE_URL` and `NEXT_PUBLIC_API_KEY`. Do not commit real keys.

## Infrastructure

`backend/infrastructure/docker-compose.yml` (project name `collabflow`):

| Compose service | Image | Host port |
|-----------------|-------|-----------|
| postgres | `postgres:17-alpine` | `POSTGRES_PORT` or 5432 |
| redis | `redis:8-alpine` | `REDIS_PORT` or 6379 |
| zookeeper | `confluentinc/cp-zookeeper:7.9.2` | `ZOOKEEPER_PORT` or 2181 |
| kafka | `confluentinc/cp-kafka:7.9.2` | `KAFKA_PORT` or 9092 |
| mailhog | `mailhog/mailhog:v1.0.1` | SMTP 1025, UI 8025 |

Named volumes: `collabflow_pg_data`, `collabflow_redis_data`, `collabflow_zk_data`, `collabflow_zk_log`. Kafka has a broker API healthcheck. Postgres uses `pg_isready`. Redis uses `redis-cli ping`.

## How to run

Full steps: [docs/running.md](../running.md). Short form:

1. Copy repo-root `.env.example` to `.env`. Fill secrets locally. Never commit them and never paste them into docs.
2. From `backend/infrastructure`: `docker compose --env-file ..\..\ .env up -d`.
3. One command from the repo root: `scripts\dev-backend.cmd start`. The script loads `.env`, starts Compose, then starts apps in this order: auth, platform, user, task, gateway, collab, realtime, email, document. Logs and pids go to `logs/backend/`. `stop` kills the app processes and leaves Docker running.
4. Or start by hand from `backend/java-services`: `.\gradlew.bat :auth-service:bootRun` (and the other modules). Gradle `bootRun` loads the repo-root `.env` (`build.gradle` `loadRepoDotEnv`). Go does not. Load `.env` into the shell before `go run ./cmd/server`.
5. Frontend: `npm run dev` inside `frontend/` (port 3000).

Suggested order so Flyway and Kafka consumers are ready: infra, then auth, platform, user, task, gateway, then collab, realtime, email. [docs/fe-contract.md](../fe-contract.md) notes that Go consumers start at `LastOffset`, so start them before you produce events you care about, or produce the event again.

### Health checks

Send `X-API-Key` on gateway calls. Go health routes in the services themselves do not check that header; the gateway does, for `/api/**`.

| Check | URL |
|-------|-----|
| Gateway to auth | `GET http://localhost:8080/api/v1/auth/health` |
| Platform | `GET http://localhost:8080/api/v1/platform/health` |
| User | `GET http://localhost:8080/api/v1/users/health` |
| Task | `GET http://localhost:8080/api/v1/tasks/health` |
| Collab direct | `GET http://localhost:9081/api/v1/collab/health` |
| Realtime direct | `GET http://localhost:9082/api/v1/realtime/health` |
| Document direct | `GET http://localhost:9083/api/v1/documents/health` |
| Email direct | `GET http://localhost:9084/api/v1/email/health` or `GET /health` |

Swagger UI is on the Java service ports, not through the gateway: `/swagger-ui.html` on 8081, 8082, 8083, and 8084. Still send `X-API-Key`.

## Environment variable names

Values stay in `.env`. Names only:

| Group | Names |
|-------|--------|
| Postgres | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`, `POSTGRES_HOST` (host is used by apps; it is not listed in `.env.example`) |
| Redis | `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` (realtime; often empty locally) |
| Kafka | `ZOOKEEPER_PORT`, `KAFKA_PORT`, `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_GROUP_ID`, `EMAIL_KAFKA_GROUP_ID` |
| Edge auth | `JWT_SECRET`, `API_KEY`, `JWT_ACCESS_MINUTES`, `JWT_REFRESH_DAYS` |
| CORS / public URL | `CORS_ALLOWED_ORIGINS`, `PUBLIC_APP_BASE_URL` |
| Mail | `SMTP_HOST`, `SMTP_PORT`, `SMTP_TLS`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM`, `EMAIL_SERVICE_ENABLED` |
| Bootstrap admin | `MASTER_ADMIN_EMAIL`, `MASTER_ADMIN_PASSWORD`, `MASTER_ADMIN_NAME` |
| Service URLs | `AUTH_SERVICE_URI`, `USER_SERVICE_URI`, `PLATFORM_SERVICE_URI`, `TASK_SERVICE_URI`, `COLLAB_SERVICE_URI`, `REALTIME_SERVICE_URI`, `DOCUMENT_SERVICE_URI`. Collab reads `PLATFORM_SERVICE_URI` for outbound membership calls. |
| Other | `SPRING_PROFILES_ACTIVE`, `LOG_HOME`, `AUTH_PUBLIC_REGISTER_ENABLED`, `PLATFORM_SELF_SERVE_ORG_CREATE`, `DOCUMENT_STORAGE_DIR`, `APP_PUBLIC_URL`, `GIN_MODE`, `APP_ENV` |
| Frontend | `NEXT_PUBLIC_API_BASE_URL`, `NEXT_PUBLIC_API_KEY` |

Go `Load()` functions contain fallbacks when `JWT_SECRET` or `POSTGRES_PASSWORD` is unset. Do not rely on those fallbacks. Set the variables. This note does not repeat the fallback strings.

## Glossary

| Term | Meaning |
|------|---------|
| bootRun | Gradle task that starts a Spring Boot service and injects repo-root `.env`. |
| Gateway | The only HTTP process the browser should call. |
| Phase 2 | user-service, document-service, and email-service added after the original six. |

## Unverified

`scripts/dev-backend.cmd` was not opened line by line. [docs/running.md](../running.md) says it delegates to `dev-backend.ps1`, and the PowerShell script’s start order was taken from the exploration of that script (auth through document). If the script and this chapter ever diverge, trust the script.
