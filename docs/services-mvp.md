# CollabFlow — Slim MVP Services (canonical)

**Status:** Slim MVP **complete** (Weeks 1–14). This file is the **source of truth** for deployables, routes, Kafka ownership, and the Java `@ApiId` catalog.

Older 16-service layout: historical notes live in [microservice.md](microservice.md) (deferred backlog) and [archive/legacy-16-service-plan.md](archive/legacy-16-service-plan.md). When owners conflict, **this file wins**.

**Tracker:** [progress-tracker.md](progress-tracker.md) · **Plan:** [implementation-guide.md](implementation-guide.md) · **Versions:** [tech-stack.md](tech-stack.md)

---

## Target: 6 services (shipped)

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

| # | Service | Lang | Port | Owns |
|---|---------|------|------|------|
| 1 | `auth-service` | Java | 8081 | users, refresh_tokens, JWT, Redis rate-limit |
| 2 | `platform-service` | Java | 8082 | organizations, workspaces, projects, members, RBAC roles/permissions |
| 3 | `task-service` | Java | 8083 | workflows, statuses, transitions, task_types, tasks (+ basic stats) |
| 4 | `collab-service` | Go | 9081 | notifications, invitations, comments, activity (+ email log stub) |
| 5 | `realtime-service` | Go | 9082 | WebSocket rooms, Redis pub/sub, Kafka → broadcast |
| 6 | `api-gateway` | Java | 8080 | routing, `X-API-Key`, JWT validation on protected routes |

**Shared infra:** PostgreSQL **17**, Redis **8**, Kafka **3.9 + ZooKeeper** (Confluent **7.9.2**). Pins: [tech-stack.md](tech-stack.md).

**Not in MVP:** sprints API (gateway path reserved only), documents, search, export, standalone email/analytics services — [future-plan.md](future-plan.md).

---

## What each service teaches

| Service | Practice |
|---------|----------|
| auth | Spring Security, JWT, Redis rate-limit |
| platform | Multi-tenant hierarchy, RBAC |
| task | Domain workflows, Kafka **produce** after writes, basic analytics |
| collab | **Golang**, GORM, Kafka **consume**, notifications/comments/activity |
| realtime | Go concurrency, WebSockets, Redis pub/sub |
| gateway | Edge routing — how microservices are exposed |

---

## API route map (gateway)

```
/api/v1/auth/**                                              → auth-service:8081
/api/v1/organizations/** , /roles/** , /workspaces/** ,
  /projects/** , /platform/**                                → platform-service:8082
/api/v1/tasks/** , /workflows/** , /task-types/** ,
  /sprints/** (reserved — not implemented)                   → task-service:8083
/api/v1/notifications/** , /collab/** , /invitations/** ,
  /comments/** , /activity/**                                → collab-service:9081
/api/v1/realtime/**                                          → realtime-service:9082
```

Endpoint catalog: `@ApiId` table below. Narrative reference: [microservice.md](microservice.md).

---

## Kafka (shipped)

**Producers**

| Service | Topics |
|---------|--------|
| auth | `user.created` |
| platform | `organization.created`, `organization.member_added`; `workspace.created\|updated\|deleted\|member_added\|member_removed`; `project.created\|updated\|deleted\|member_added\|member_removed` |
| task | `task.created`, `task.updated`, `task.deleted`, `task.assigned`, `task.status_changed` (status/assign also dual-publish `task.updated` for WS) |
| collab | `comment.added` |

**Consumers**

| Service | Topics → effect |
|---------|-----------------|
| collab | `task.assigned` → notification (+ email stub log); `task.created` → activity; `comment.added` → activity; mentions → in-process `COMMENT_MENTION` notification |
| realtime | `task.updated`, `comment.added` → Redis rooms → WebSocket broadcast |

Platform org/workspace/project topics are produced for portfolio/event story; Go does not consume them in MVP.

---

## Redis (shipped)

| Service | Use |
|---------|-----|
| auth | Login/register rate-limit (fail-open if Redis down) |
| realtime | Pub/sub bus for multi-instance-ready WS |
| collab | None in MVP |

---

## Repo layout

```
java-services/
  common/            # ApiResponse, ErrorResponse, ApiKeyFilter, GlobalExceptionHandler
  auth-service/
  platform-service/
  task-service/
  api-gateway/
go-services/
  collab-service/
  realtime-service/
infrastructure/      # docker-compose: postgres, redis, zookeeper, kafka
docs/
postman/
```

---

## Java `@ApiId` catalog (logs / MDC)

Controllers tag handlers with `@ApiId("AREA-NNN")` (see `.cursor/rules/java-spring.mdc`). Go ids below are **documentation ids** for support/logs (no Java annotation). Not the same as HTTP `ErrorCode`.

| Prefix | Area |
|--------|------|
| `AUTH-NNN` | auth-service |
| `PLAT-NNN` | platform health |
| `ORG-NNN` | organizations + members |
| `ROLE-NNN` | roles list |
| `WS-NNN` | workspaces |
| `PRJ-NNN` | projects |
| `TASK-NNN` | tasks |
| `WF-NNN` | workflows / task-types |
| `NOTIF-NNN` | collab notifications |
| `INV-NNN` | collab invitations |
| `CMT-NNN` | collab comments |
| `ACT-NNN` | collab activity |
| `RT-NNN` | realtime |

| Id | Method | Path |
|----|--------|------|
| AUTH-001 | GET | `/api/v1/auth/health` |
| AUTH-002 | POST | `/api/v1/auth/register` |
| AUTH-003 | POST | `/api/v1/auth/login` |
| AUTH-004 | POST | `/api/v1/auth/refresh` |
| AUTH-005 | POST | `/api/v1/auth/logout` |
| AUTH-006 | GET | `/api/v1/auth/me` |
| PLAT-001 | GET | `/api/v1/platform/health` |
| ROLE-001 | GET | `/api/v1/roles` |
| ORG-001 | POST | `/api/v1/organizations` |
| ORG-002 | GET | `/api/v1/organizations` |
| ORG-003 | GET | `/api/v1/organizations/{id}` |
| ORG-004 | PUT | `/api/v1/organizations/{id}` |
| ORG-005 | DELETE | `/api/v1/organizations/{id}` |
| ORG-006 | GET | `/api/v1/organizations/{id}/members` |
| ORG-007 | POST | `/api/v1/organizations/{id}/members` |
| ORG-008 | PUT | `/api/v1/organizations/{id}/members/{memberId}` |
| ORG-009 | DELETE | `/api/v1/organizations/{id}/members/{memberId}` |
| WS-001 | POST | `/api/v1/workspaces` |
| WS-002 | GET | `/api/v1/organizations/{orgId}/workspaces` |
| WS-003 | GET | `/api/v1/workspaces/{id}` |
| WS-004 | PUT | `/api/v1/workspaces/{id}` |
| WS-005 | DELETE | `/api/v1/workspaces/{id}` |
| WS-006 | GET | `/api/v1/workspaces/{id}/members` |
| WS-007 | POST | `/api/v1/workspaces/{id}/members` |
| WS-008 | PUT | `/api/v1/workspaces/{id}/members/{memberId}` |
| WS-009 | DELETE | `/api/v1/workspaces/{id}/members/{memberId}` |
| WS-010 | POST | `/api/v1/workspaces/{id}/join` |
| PRJ-001 | POST | `/api/v1/projects` |
| PRJ-002 | GET | `/api/v1/workspaces/{workspaceId}/projects` |
| PRJ-003 | GET | `/api/v1/projects/{id}` |
| PRJ-004 | PUT | `/api/v1/projects/{id}` |
| PRJ-005 | DELETE | `/api/v1/projects/{id}` |
| PRJ-006 | GET | `/api/v1/projects/{id}/members` |
| PRJ-007 | POST | `/api/v1/projects/{id}/members` |
| PRJ-008 | PUT | `/api/v1/projects/{id}/members/{memberId}` |
| PRJ-009 | DELETE | `/api/v1/projects/{id}/members/{memberId}` |
| TASK-000 | GET | `/api/v1/tasks/health` |
| TASK-001 | POST | `/api/v1/tasks` |
| TASK-002 | GET | `/api/v1/tasks?projectId=` |
| TASK-003 | GET | `/api/v1/tasks/{id}` |
| TASK-004 | PUT | `/api/v1/tasks/{id}` |
| TASK-005 | DELETE | `/api/v1/tasks/{id}` |
| TASK-006 | PUT | `/api/v1/tasks/{id}/status` |
| TASK-007 | PUT | `/api/v1/tasks/{id}/assign` |
| TASK-008 | GET | `/api/v1/tasks/stats?projectId=` |
| WF-001 | GET | `/api/v1/workflows/default` |
| WF-002 | GET | `/api/v1/workflows/{id}` |
| WF-003 | GET | `/api/v1/workflows/{id}/statuses` |
| WF-004 | GET | `/api/v1/task-types` |
| NOTIF-000 | GET | `/api/v1/collab/health` |
| NOTIF-001 | GET | `/api/v1/notifications` |
| NOTIF-002 | PUT | `/api/v1/notifications/{id}/read` |
| NOTIF-003 | PUT | `/api/v1/notifications/read-all` |
| INV-001 | POST | `/api/v1/invitations` |
| INV-002 | GET | `/api/v1/invitations/token/{token}` |
| INV-003 | GET | `/api/v1/invitations` |
| INV-004 | GET | `/api/v1/invitations/org?organizationId=` |
| INV-005 | POST | `/api/v1/invitations/token/{token}/accept` |
| INV-006 | DELETE | `/api/v1/invitations/{id}` |
| CMT-001 | POST | `/api/v1/comments` |
| CMT-002 | GET | `/api/v1/comments?entityType=&entityId=` |
| CMT-003 | GET | `/api/v1/comments/{id}` |
| CMT-004 | PUT | `/api/v1/comments/{id}` |
| CMT-005 | DELETE | `/api/v1/comments/{id}` |
| ACT-001 | GET | `/api/v1/activity?organizationId=` |
| ACT-002 | GET | `/api/v1/activity?entityType=&entityId=` |
| ACT-003 | GET | `/api/v1/activity/entity/{type}/{id}` |
| ACT-004 | GET | `/api/v1/activity/user/{id}` |
| RT-000 | GET | `/api/v1/realtime/health` |
| RT-001 | GET (WS) | `/api/v1/realtime/ws` |
| RT-002 | POST | `/api/v1/realtime/broadcast` |

**OpenAPI (Java):** Swagger UI on direct ports — auth `:8081`, platform `:8082`, task `:8083` → `/swagger-ui.html` (send `X-API-Key`). Gateway does not proxy Swagger.

**Logs (local `bootRun`):** `logs/auth/`, `logs/platform/`, `logs/task/` under repo root (`LOG_HOME` override). Go: under each `go-services/*` README.

---

## Portfolio narrative

> CollabFlow is a multi-tenant project tool with a **slim microservice** backend: three Spring Boot (Gradle) services for auth, tenancy/platform, and tasks; two Golang services for collaboration (Kafka consumers) and realtime (WebSocket + Redis); shared Postgres 17; Redis 8; Kafka 3.9 with ZooKeeper (Confluent 7.9). Scope was reduced from 16 services so boundaries stay real and the system stays finishable and explainable. Java owns the sync domain; Go owns async side effects and live updates.
