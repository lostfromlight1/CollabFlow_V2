# CollabFlow

Multi-tenant collaboration and project management  
**Hierarchy:** Organization → Workspace → Project → Task.

**Status:** Slim **MVP complete** (Weeks 1–14). Tracker: [docs/progress-tracker.md](docs/progress-tracker.md).  
Demo script: [docs/e2e-demo.md](docs/e2e-demo.md) · Postman: [postman/](postman/).

## Portfolio — what / why

CollabFlow is a multi-tenant project tool built as a **slim microservice** backend: three Spring Boot (Gradle) services for auth, tenancy/platform, and tasks; two Golang services for collaboration (Kafka consumers → notifications, invitations, comments, activity) and realtime (WebSocket + Redis pub/sub). Shared Postgres 17, Redis 8, and Kafka 3.9 + ZooKeeper (Confluent 7.9) keep local ops simple. Scope was cut from a 16-service plan so each boundary stays real and the system stays finishable for a junior portfolio—Java owns the sync domain; Go owns async side effects and live updates.

**What I practiced:** service boundaries, JWT at the gateway, Flyway vs GORM AutoMigrate, Kafka after DB commit, consumer idempotency, and Redis pub/sub for multi-instance-ready WS.

## Services (6)

| Service | Stack | Port | Responsibility |
|---------|--------|------|----------------|
| api-gateway | Spring Cloud Gateway | 8080 | Edge routing, API key, JWT |
| auth-service | Java 21 / Boot 4.1 | 8081 | Users, JWT |
| platform-service | Java 21 / Boot 4.1 | 8082 | Org + workspace + project + RBAC |
| task-service | Java 21 / Boot 4.1 | 8083 | Workflows + tasks + stats |
| collab-service | Go 1.27 / Gin | 9081 | Notifications, invitations, comments, activity |
| realtime-service | Go 1.27 / WS | 9082 | WebSocket rooms + Redis + Kafka |

Versions: [docs/tech-stack.md](docs/tech-stack.md). Boundaries / ApiIds: [docs/services-mvp.md](docs/services-mvp.md).

## Quick start

1. [docs/setup.md](docs/setup.md) — JDK 21, Go 1.27, Docker  
2. Copy `.env.example` → `.env`  
3. Infra:

```powershell
cd infrastructure
docker compose --env-file ../.env up -d
```

4. Run services (gateway last or in parallel), then Postman **CollabFlow Local** or [e2e-demo.md](docs/e2e-demo.md).

## OpenAPI (Java)

Springdoc Swagger UI (direct ports; still send `X-API-Key`):

| Service | Swagger UI |
|---------|------------|
| auth | http://localhost:8081/swagger-ui.html |
| platform | http://localhost:8082/swagger-ui.html |
| task | http://localhost:8083/swagger-ui.html |

OpenAPI JSON: `/v3/api-docs` on each service. Full route catalog: [docs/services-mvp.md](docs/services-mvp.md).

## API examples (via gateway `:8080`)

```bash
# Health
curl -s http://localhost:8080/api/v1/auth/health -H "X-API-Key: $API_KEY"

# Register → login (save accessToken)
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" -H "X-API-Key: $API_KEY" \
  -d '{"email":"you@example.com","password":"password123","name":"You"}'

# Create org (Bearer from login)
curl -s -X POST http://localhost:8080/api/v1/organizations \
  -H "Content-Type: application/json" -H "X-API-Key: $API_KEY" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Acme","slug":"acme"}'

# Task stats (analytics)
curl -s "http://localhost:8080/api/v1/tasks/stats?projectId=$PROJECT_ID" \
  -H "X-API-Key: $API_KEY" -H "Authorization: Bearer $TOKEN"
```

Go services: health under `/api/v1/collab/health`, `/api/v1/realtime/health`.  
Realtime WS: `ws://localhost:9082/api/v1/realtime/ws?token=$TOKEN` then  
`{"action":"subscribe","room":"project:<projectId>"}`.

## Docs map

| Doc | Contents |
|-----|----------|
| [docs/implementation-guide.md](docs/implementation-guide.md) | Week-by-week plan |
| [docs/progress-tracker.md](docs/progress-tracker.md) | Checkboxes |
| [docs/e2e-demo.md](docs/e2e-demo.md) | End-to-end demo |
| [docs/services-mvp.md](docs/services-mvp.md) | Ports + ApiId catalog |
| [docs/future-plan.md](docs/future-plan.md) | Deferred / Phase 2 |
| [AGENTS.md](AGENTS.md) | Cursor agent guide |

## Phase 2 (optional next)

MVP stops here. First Phase 2 pick from [future-plan.md](docs/future-plan.md): **document-service** (uploads / folders) when you want to expand—not required for the MVP checklist.

## Repo layout

```
java-services/     auth, platform, task, api-gateway, common
go-services/       collab, realtime
infrastructure/    docker-compose
docs/              guides + schema
postman/           Auth + Platform collections
```
