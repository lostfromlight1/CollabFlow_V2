---
name: add-microservice
description: >-
  Scaffold a new CollabFlow microservice (Java Spring Boot or Golang Gin).
  Use when creating a new service under backend/java-services/ or backend/go-services/, adding
  a service from docs/microservice.md, or when the user asks to add/scaffold a microservice.
---

# Add Microservice

## Before you start

1. Confirm the service is on the **slim MVP list** in [docs/services-mvp.md](../../../docs/services-mvp.md).
2. Use [docs/microservice.md](../../../docs/microservice.md) only for endpoint/event detail under that merged owner.
3. Do not scaffold deferred services (document, search, export, etc.) unless Kaung explicitly expands scope.
4. Do not invent a new service without updating `services-mvp.md` and `AGENTS.md` first.

## Checklist

Copy and track:

```
Service scaffold:
- [ ] Language and folder chosen (backend/java-services/{name} or backend/go-services/{name})
- [ ] Port matches microservice.md gateway routes
- [ ] README with responsibility, endpoints, deps
- [ ] Config via env (DB, Redis, Kafka)
- [ ] Health endpoint
- [ ] Dockerfile stub (optional until Phase 1 infra)
- [ ] OpenAPI / endpoint list placeholder
- [ ] Unit test smoke placeholder
```

## Java (Spring Boot)

1. Create `backend/java-services/{service-name}/` with standard layout from `.cursor/rules/java-spring.mdc`.
2. Dependencies: Web, Security (if auth-facing), Data JPA, Redis, Kafka as required by microservice.md.
3. `application.yml` placeholders for Postgres, Redis, Kafka.
4. Flyway folder: `src/main/resources/db/migration/` (empty or first migration if this service owns new tables).
5. Controllers stubbed to documented `/api/v1/...` paths only.

## Golang (Gin)

1. Create `backend/go-services/{service-name}/` with layout from `.cursor/rules/golang-services.mdc`.
2. Module name consistent with repo (e.g. `github.com/.../go-services/{name}` — match existing when present).
3. Gin router, GORM dialector for Postgres, go-redis / Kafka clients as needed.
4. Handlers stubbed to documented paths only.

## Ports (slim MVP — services-mvp.md)

| Service | Port | Path prefixes |
|---------|------|----------------|
| api-gateway | 8080 | public entry |
| auth-service | 8081 | `/api/v1/auth` |
| platform-service | 8082 | `/api/v1/organizations`, `/workspaces`, `/projects` |
| task-service | 8083 | `/api/v1/tasks`, `/workflows`, `/sprints` |
| collab-service | 9081 | `/api/v1/notifications`, `/invitations`, `/comments`, `/activity` |
| realtime-service | 9082 | `/api/v1/realtime` |

## Done when

- Folder + minimal runnable skeleton exist
- Endpoints and Kafka topics match docs (stubs OK)
- README points to microservice.md section for this service
