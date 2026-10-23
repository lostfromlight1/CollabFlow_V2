# CollabFlow

Multi-tenant collaboration and project management  
**Hierarchy:** Organization → Workspace → Project → Task.

**Status:** Slim **MVP complete** (Weeks 1–14) + Phase 2 on `development` / FE freeze on **`uat`**.  
**Frontend / UAT:** [docs/fe-contract.md](docs/fe-contract.md) (headers, org flow, ports, smoke).  
Run: [docs/running.md](docs/running.md) · Demo: [docs/e2e-demo.md](docs/e2e-demo.md) · Postman: [postman/](postman/) · MVP history: [docs/progress-tracker.md](docs/progress-tracker.md).

## Portfolio — what / why

CollabFlow is a multi-tenant project tool built as a **slim microservice** backend: three Spring Boot (Gradle) services for auth, tenancy/platform, and tasks; two Golang services for collaboration (Kafka consumers → notifications, invitations, comments, activity) and realtime (WebSocket + Redis pub/sub). Shared Postgres 17, Redis 8, and Kafka 3.9 + ZooKeeper (Confluent 7.9) keep local ops simple. Scope was cut from a 16-service plan so each boundary stays real and the system stays finishable for a junior portfolio—Java owns the sync domain; Go owns async side effects and live updates.

**What I practiced:** service boundaries, JWT at the gateway, Flyway vs GORM AutoMigrate, Kafka after DB commit, consumer idempotency, and Redis pub/sub for multi-instance-ready WS.

## Services

**MVP (6)** + Phase 2: user `:8084`, email `:9084` (document stubs `:9083`). Full table: [docs/running.md](docs/running.md#3-port-map-apps).

| Service | Stack | Port | Responsibility |
|---------|--------|------|----------------|
| api-gateway | Spring Cloud Gateway | 8080 | Edge routing, API key, JWT |
| auth-service | Java 21 / Boot 4.1 | 8081 | Users, JWT, master admin |
| platform-service | Java 21 / Boot 4.1 | 8082 | Org + workspace + project + RBAC |
| task-service | Java 21 / Boot 4.1 | 8083 | Workflows + tasks + sprints |
| collab-service | Go 1.27 / Gin | 9081 | Notifications, invitations, comments, activity |
| realtime-service | Go 1.27 / WS | 9082 | WebSocket rooms + Redis + Kafka |

Versions: [docs/tech-stack.md](docs/tech-stack.md). Boundaries / ApiIds: [docs/services-mvp.md](docs/services-mvp.md).

## Quick start

1. [docs/setup.md](docs/setup.md) — install JDK 21, Go 1.27, Docker  
2. [docs/running.md](docs/running.md) — `.env`, Compose (Postgres/Redis/Kafka/MailHog), start Java + Go  
3. [docs/e2e-demo.md](docs/e2e-demo.md) or Postman **Auth → 4. Phase 2 admin tenancy**

## OpenAPI (Java)

Springdoc Swagger UI (direct ports; still send `X-API-Key`):

| Service | Swagger UI |
|---------|------------|
| auth | http://localhost:8081/swagger-ui.html |
| platform | http://localhost:8082/swagger-ui.html |
| task | http://localhost:8083/swagger-ui.html |
| user | http://localhost:8084/swagger-ui.html |

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
| [docs/](docs/) | **Shared contracts** (ApiIds, schema, fe-contract, running) |
| [backend/README.md](backend/README.md) | How to run Java/Go + infra |
| [frontend/README.md](frontend/README.md) | Next.js app (when scaffolded) |
| [docs/fe-contract.md](docs/fe-contract.md) | **FE ↔ BE contract** |
| [docs/services-mvp.md](docs/services-mvp.md) | Ports + ApiId catalog |
| [docs/running.md](docs/running.md) | Infra + every service |
| [AGENTS.md](AGENTS.md) | Cursor agent guide |

## Repo layout

```
backend/
  java-services/   auth, platform, task, user, api-gateway, common
  go-services/     collab, realtime, email, document
  infrastructure/  docker-compose (Postgres, Redis, Kafka, MailHog)
frontend/          Next.js (App Router)
docs/              shared contracts + plans
postman/           Auth + Platform collections
```
