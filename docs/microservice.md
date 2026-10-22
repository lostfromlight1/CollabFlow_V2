# CollabFlow — Microservice reference (slim MVP)

**Status:** Slim MVP **complete** (Weeks 1–14).  
**Canonical deployables / ApiId catalog:** [services-mvp.md](services-mvp.md) (wins on conflicts).  
**Versions:** [tech-stack.md](tech-stack.md) · **Schema:** [database.md](database.md) · **Deferred:** [future-plan.md](future-plan.md)

This file is the **endpoint + Kafka + Redis narrative** for the **6 shipped services**. The old 16-service split is summarized only under [Deferred backlog](#deferred-backlog-old-16-service-split).

Java patterns (`@CurrentUserId`, `@ApiId`): `.cursor/rules/java-spring.mdc`.

---

## Architecture overview

```
                    ┌─────────────────┐
                    │   API Gateway   │
                    │  Spring Cloud   │
                    │   Gateway:8080  │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
   ┌─────▼─────┐       ┌─────▼─────┐       ┌─────▼─────┐
   │   auth    │       │ platform  │       │   task    │
   │   :8081   │       │   :8082   │       │   :8083   │
   └─────┬─────┘       └─────┬─────┘       └─────┬─────┘
         │                   │                   │
         │            ┌──────▼──────┐            │
         │            │    Kafka    │◄───────────┘
         │            └──────┬──────┘
         │       ┌───────────┴───────────┐
   ┌─────▼─────┐ │                       │
   │ Postgres  │┌▼──────────┐     ┌──────▼──────┐
   │ + Redis   ││  collab   │     │  realtime   │
   │           ││   :9081   │     │   :9082     │
   └───────────┘└───────────┘     └─────────────┘
```

| Layer | Services |
|-------|----------|
| Edge | `api-gateway` (Java) |
| Sync domain | `auth-service`, `platform-service`, `task-service` (Java) |
| Async / live | `collab-service`, `realtime-service` (Go) |
| Data | Shared PostgreSQL 17, Redis 8, Kafka 3.9 + ZooKeeper (CP 7.9) |

---

## Communication patterns

| Pattern | When | Example |
|---------|------|---------|
| Sync HTTP | Caller needs a response | Create org, assign task |
| Kafka after commit | Side effects | `task.assigned` → collab notification |
| Redis pub/sub | Fan-out for WS | realtime broadcasts to rooms |

Gateway: every `/api/**` needs `X-API-Key`. Protected routes need `Authorization: Bearer <accessJWT>`.

---

## 1. api-gateway (Java · 8080)

**Responsibility:** Route `/api/v1/**` to upstreams; validate `X-API-Key`; JWT for protected paths.

**Routes:** see [services-mvp.md](services-mvp.md) route map.  
**Note:** `/api/v1/sprints/**` is reserved on the gateway; **no sprint handlers** in MVP.

---

## 2. auth-service (Java · 8081)

**Owns:** `users`, `refresh_tokens`  
**Kafka produce:** `user.created`  
**Redis:** login/register rate-limit  

| Method | Path | Notes |
|--------|------|--------|
| GET | `/api/v1/auth/health` | Public |
| POST | `/api/v1/auth/register` | Publishes `user.created` |
| POST | `/api/v1/auth/login` | Access + refresh tokens |
| POST | `/api/v1/auth/refresh` | Refresh rotation |
| POST | `/api/v1/auth/logout` | Revoke refresh |
| GET | `/api/v1/auth/me` | JWT required |

**Not in MVP:** email verify, forgot/reset password, MFA, `user.updated` topic.

---

## 3. platform-service (Java · 8082)

**Owns:** `organizations`, `organization_members`, `workspaces`, `workspace_members`, `projects`, `project_members`, `roles`, `permissions`, `role_permissions`  
**Kafka produce:** `organization.*` (created, member_added), `workspace.*`, `project.*`  
**RBAC:** `PermissionService` checks role permissions per org/workspace/project scope.

### Organizations

| Method | Path |
|--------|------|
| POST | `/api/v1/organizations` |
| GET | `/api/v1/organizations` |
| GET | `/api/v1/organizations/{id}` |
| PUT | `/api/v1/organizations/{id}` |
| DELETE | `/api/v1/organizations/{id}` |
| GET/POST | `/api/v1/organizations/{id}/members` |
| PUT/DELETE | `/api/v1/organizations/{id}/members/{memberId}` |
| GET | `/api/v1/roles` |

### Workspaces

| Method | Path |
|--------|------|
| POST | `/api/v1/workspaces` |
| GET | `/api/v1/organizations/{orgId}/workspaces` |
| GET/PUT/DELETE | `/api/v1/workspaces/{id}` |
| GET/POST | `/api/v1/workspaces/{id}/members` |
| PUT/DELETE | `/api/v1/workspaces/{id}/members/{memberId}` |
| POST | `/api/v1/workspaces/{id}/join` |

### Projects

| Method | Path |
|--------|------|
| POST | `/api/v1/projects` |
| GET | `/api/v1/workspaces/{workspaceId}/projects` |
| GET/PUT/DELETE | `/api/v1/projects/{id}` |
| GET/POST | `/api/v1/projects/{id}/members` |
| PUT/DELETE | `/api/v1/projects/{id}/members/{memberId}` |
| GET | `/api/v1/platform/health` |

---

## 4. task-service (Java · 8083)

**Owns:** `status_workflows`, `workflow_statuses`, `workflow_transitions`, `task_types`, `tasks`  
**Kafka produce:** `task.created`, `task.updated`, `task.deleted`, `task.assigned`, `task.status_changed`  
**Analytics (MVP):** `GET /api/v1/tasks/stats?projectId=` — counts by status/priority.

| Method | Path |
|--------|------|
| GET | `/api/v1/tasks/health` |
| POST | `/api/v1/tasks` |
| GET | `/api/v1/tasks?projectId=` |
| GET/PUT/DELETE | `/api/v1/tasks/{id}` |
| PUT | `/api/v1/tasks/{id}/status` |
| PUT | `/api/v1/tasks/{id}/assign` |
| GET | `/api/v1/tasks/stats?projectId=` |
| GET | `/api/v1/workflows/default` |
| GET | `/api/v1/workflows/{id}` |
| GET | `/api/v1/workflows/{id}/statuses` |
| GET | `/api/v1/task-types` |

**Not in MVP:** sprint CRUD, custom-field admin API, bulk update, task dependencies API, workflow create/update (seeded default workflow only).

---

## 5. collab-service (Go · 9081)

**Owns:** `notifications`, `invitations`, `comments`, `activity_feed` (+ related mention rows as implemented)  
**Kafka consume:** `task.assigned`, `task.created`, `comment.added`  
**Kafka produce:** `comment.added`  
**Email:** log stub only (no separate email-service)

| Method | Path |
|--------|------|
| GET | `/api/v1/collab/health` |
| GET | `/api/v1/notifications` |
| PUT | `/api/v1/notifications/{id}/read` |
| PUT | `/api/v1/notifications/read-all` |
| POST | `/api/v1/invitations` |
| GET | `/api/v1/invitations` |
| GET | `/api/v1/invitations/org?organizationId=` |
| GET | `/api/v1/invitations/token/{token}` |
| POST | `/api/v1/invitations/token/{token}/accept` |
| DELETE | `/api/v1/invitations/{id}` |
| POST | `/api/v1/comments` |
| GET | `/api/v1/comments?entityType=&entityId=` |
| GET/PUT/DELETE | `/api/v1/comments/{id}` |
| GET | `/api/v1/activity?organizationId=` / `?entityType=&entityId=` |
| GET | `/api/v1/activity/entity/{type}/{id}` |
| GET | `/api/v1/activity/user/{id}` |

---

## 6. realtime-service (Go · 9082)

**Owns:** no primary tables (in-memory rooms + Redis bus)  
**Kafka consume:** `task.updated`, `comment.added` → publish to Redis rooms → WS clients  
**Redis:** pub/sub for multi-instance readiness  

| Method | Path |
|--------|------|
| GET | `/api/v1/realtime/health` |
| GET (WS) | `/api/v1/realtime/ws` |
| POST | `/api/v1/realtime/broadcast` |

Client flow: connect with JWT query/header as documented in service README; `{"action":"subscribe","room":"project:<uuid>"}` / `task:<uuid>`.

**Not in MVP:** presence, typing indicators.

---

## Kafka event catalog (shipped)

Envelope: shared JSON domain event (`eventId`, `eventType`, `occurredAt`, `payload`) — produce **after** successful DB commit.

| Topic | Producer | Primary consumer(s) |
|-------|----------|---------------------|
| `user.created` | auth | (none in MVP — available for Phase 2) |
| `organization.created` | platform | (none in MVP) |
| `organization.member_added` | platform | (none in MVP) |
| `workspace.*` | platform | (none in MVP) |
| `project.*` | platform | (none in MVP) |
| `task.created` | task | collab → activity |
| `task.updated` | task | realtime → WS |
| `task.deleted` | task | (none dedicated) |
| `task.assigned` | task | collab → notification |
| `task.status_changed` | task | (+ dual `task.updated` for WS) |
| `comment.added` | collab | collab activity + realtime WS |

---

## Redis usage (shipped)

| Key / pattern | Service | Purpose |
|---------------|---------|---------|
| Rate-limit keys (login/register) | auth | Throttle by IP |
| Pub/sub channels + room fan-out | realtime | Multi-instance WS |

---

## Data management

- **Shared Postgres MVP** — one DB `collabflow`; ownership by table still matters (Flyway per Java service).  
- **No DB-per-service** until Phase 2 scale needs force it.  
- Go services do **not** own Flyway; they map existing tables via GORM.

---

## Deferred backlog (old 16-service split)

Previously planned as separate deployables. **Do not resurrect** unless expanding past slim MVP ([future-plan.md](future-plan.md)).

| Old name | Now / later |
|----------|-------------|
| organization / workspace / project services | Merged → `platform-service` |
| workflow-service | Merged → `task-service` (read seeded workflows) |
| notification / invitation / comment / activity / email | Merged → `collab-service` (email = stub) |
| document-service | Phase 2 |
| search-service | Phase 3 |
| export-service | Phase 3 |
| analytics-service | Phase 3 (MVP has task stats only) |
| sprint APIs | Optional stretch inside `task-service` |
| Kong gateway | Replaced by Spring Cloud Gateway |

Full archived week plan: [archive/legacy-16-service-plan.md](archive/legacy-16-service-plan.md).

---

**Last updated:** 2026-10-22
