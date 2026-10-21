# CollabFlow — Slim MVP Services (canonical)

**Status:** This is the **source of truth** for what we build.  
The older 16-service layout in [microservice.md](microservice.md) is **reference / future backlog** only.

**Why slim:** Keep Java + Go + Kafka + Redis + real microservice boundaries, without 16 deployables. Still challenging for a junior portfolio; easier to understand and finish.

---

## Target: 6 services

```
                 API Gateway (Spring Cloud Gateway — Java)
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
   ┌────▼────┐          ┌─────▼─────┐         ┌─────▼─────┐
   │  Java   │          │   Java    │         │   Java    │
   │  auth   │          │ platform  │         │   task    │
   └────┬────┘          └─────┬─────┘         └─────┬─────┘
        │                     │                     │
        │              ┌──────▼──────┐              │
        │              │    Kafka    │◄─────────────┘
        │              └──────┬──────┘
        │         ┌───────────┴───────────┐
   ┌────▼────┐    │                       │
   │ Postgres│  ┌─▼────────┐        ┌─────▼─────┐
   │ (shared)│  │   Go     │        │    Go     │
   │ + Redis │  │  collab  │        │ realtime  │
   └─────────┘  └──────────┘        └───────────┘
```

| # | Service | Lang | Port | Owns (from old split) |
|---|---------|------|------|------------------------|
| 1 | `auth-service` | Java | 8081 | auth-service |
| 2 | `platform-service` | Java | 8082 | organization + workspace + project (+ members/roles usage) |
| 3 | `task-service` | Java | 8083 | task + workflow (+ sprints when we reach that feature) |
| 4 | `collab-service` | Go | 9081 | notification + invitation + comment + activity (+ simple email send) |
| 5 | `realtime-service` | Go | 9082 | realtime-service (WebSocket + Redis pub/sub) |
| 6 | API Gateway | Java | 8080 | routing, JWT validation, rate limit |

**Shared infra (not “business services”):** PostgreSQL **17**, Redis **8**, Kafka **3.9 + ZooKeeper** (Confluent Platform **7.9.x**). Docker Compose for local.  
**Exact pins:** [tech-stack.md](tech-stack.md).

---

## What each service teaches you

| Service | You practice |
|---------|----------------|
| auth | Spring Security, JWT, Redis sessions (you already know Java — solid base) |
| platform | Multi-tenant hierarchy, RBAC, modular monolith *inside* one service |
| task | Complex domain, workflows, Kafka **produce** after writes |
| collab | **Golang**, GORM, Kafka **consume**, notifications/comments |
| realtime | Go concurrency, WebSockets, Redis pub/sub |
| gateway | Edge routing — how microservices are exposed |

---

## API route map (gateway)

```
/api/v1/auth/**           → auth-service:8081
/api/v1/organizations/**  → platform-service:8082
/api/v1/workspaces/**     → platform-service:8082
/api/v1/projects/**       → platform-service:8082
/api/v1/tasks/**          → task-service:8083
/api/v1/workflows/**      → task-service:8083
/api/v1/sprints/**        → task-service:8083
/api/v1/notifications/**  → collab-service:9081
/api/v1/invitations/**    → collab-service:9081
/api/v1/comments/**       → collab-service:9081
/api/v1/activity/**       → collab-service:9081
/api/v1/realtime/**       → realtime-service:9082
```

Endpoint details can still be copied from [microservice.md](microservice.md); only the **process boundary** changed (merged owners).

---

## Kafka (keep, but fewer producers/consumers)

**Producers (Java):**

- auth → `user.created`, `user.updated`
- platform → `organization.*`, `workspace.*`, `project.*`
- task → `task.created`, `task.updated`, `task.assigned`, `task.status_changed`

**Consumers (Go):**

- collab → creates notifications, activity rows; handles invitation side effects
- realtime → broadcasts task/comment updates to WebSocket rooms

Same event envelope as microservice.md. Fewer services = clearer event story for your portfolio.

---

## Deferred (Phase 2+ — not MVP)

Full backlog of **merged + deferred** services (what we reduced and when to bring them back): **[future-plan.md](future-plan.md)**.

Do **not** build those as separate deployables until this slim MVP works end-to-end.

---

## MVP feature order (unchanged product, fewer deployables)

1. Auth (`auth-service`)
2. Org / workspace / project (`platform-service`)
3. Task + workflow (`task-service`)
4. Notifications, invitations, comments, activity (`collab-service`)
5. Realtime updates (`realtime-service`)
6. Basic analytics = read APIs on Java services (no new service)

---

## Repo folders (when coding starts)

```
java-services/
  auth-service/
  platform-service/
  task-service/
  api-gateway/
go-services/
  collab-service/
  realtime-service/
infrastructure/          # docker-compose: postgres, redis, zookeeper, kafka
docs/
```

**Build plan:** [implementation-guide.md](implementation-guide.md) · **Tracker:** [progress-tracker.md](progress-tracker.md)

---

## Portfolio narrative (one paragraph)

> CollabFlow is a multi-tenant project tool with a **slim microservice** backend: three Spring Boot (Gradle) services for auth, tenancy/platform, and tasks; two Golang services for collaboration (Kafka consumers) and realtime (WebSocket + Redis); shared Postgres 17; Redis 8; Kafka 3.9 with ZooKeeper (Confluent 7.9). Scope was reduced from 16 services so boundaries stay real and the system stays finishable and explainable.
