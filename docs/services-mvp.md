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
| 3 | `task-service` | Java | 8083 | workflows, statuses, transitions, task_types, tasks, sprints (+ basic stats) |
| 4 | `collab-service` | Go | 9081 | notifications, invitations, comments, activity (+ email log stub) |
| 5 | `realtime-service` | Go | 9082 | WebSocket rooms, Redis pub/sub, Kafka → broadcast |
| 6 | `api-gateway` | Java | 8080 | routing, `X-API-Key`, JWT validation on protected routes |

**Shared infra:** PostgreSQL **17**, Redis **8**, Kafka **3.9 + ZooKeeper** (Confluent **7.9.2**). Pins: [tech-stack.md](tech-stack.md).

**Not in MVP (Phase 3+):** search, export, analytics — [future-plan.md](future-plan.md).

### Phase 2 deployables

| # | Service | Lang | Port | Owns / role | Wave |
|---|---------|------|------|-------------|------|
| — | `user-service` | Java | 8084 | Profiles + admin user CRUD on shared `users` table | Wave 3 |
| 7 | `document-service` | Go | 9083 | Upload/list/download; files on disk; metadata in Postgres | Wave 7 |
| 8 | `email-service` | Go | 9084 | Consume `email.send` → real SMTP (MailHog locally) | Wave 8 |

Details: [phase-2.md](phase-2.md), [document-service/README](../backend/go-services/document-service/README.md), [email-service/README](../backend/go-services/email-service/README.md).

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
/api/v1/admin/users/*/deactivate|activate|sessions/revoke|reset-password|resend-verification
  /api/v1/admin/sessions/**                                  → auth-service:8081 (before the user wildcard)
/api/v1/users/** , /admin/users/**                           → user-service:8084
/api/v1/organizations/** , /roles/** , /workspaces/** ,
  /projects/** , /platform/** , /admin/organizations/** ,
  /admin/announcements/** , /admin/feature-flags/** ,
  /admin/audit/** , /admin/stats , /announcements/** ,
  /feature-flags/**                                          → platform-service:8082
/api/v1/tasks/** , /workflows/** , /task-types/** ,
  /sprints/**                                                → task-service:8083
/api/v1/notifications/** , /collab/** , /invitations/** ,
  /comments/** , /activity/**                                → collab-service:9081
/api/v1/realtime/**                                          → realtime-service:9082
/api/v1/documents/**                                         → document-service:9083   (Phase 2 Wave 7)
# email-service:9084 — Kafka consumer; health direct (no gateway route yet)
```

Endpoint catalog: `@ApiId` table below. Narrative reference: [microservice.md](microservice.md).

---

## Kafka (shipped)

**Producers**

| Service | Topics |
|---------|--------|
| auth | `user.created` (public register when enabled); `email.send` (verify/reset) |
| user-service | `user.created` (admin create), `user.updated` (profile) |
| platform | `organization.created\|updated\|deleted\|member_added\|member_removed`; `workspace.created\|updated\|deleted\|member_added\|member_removed`; `project.created\|updated\|deleted\|member_added\|member_removed` |
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
backend/
  java-services/
    common/            # ApiResponse, ErrorResponse, ApiKeyFilter, GlobalExceptionHandler
    auth-service/
    user-service/      # Phase 2 Wave 3 — profiles + admin users (:8084)
    platform-service/
    task-service/
    api-gateway/
  go-services/
    collab-service/
    realtime-service/
    document-service/  # Phase 2 Wave 7 scaffold (:9083)
    email-service/     # Phase 2 Wave 8 scaffold (:9084)
  infrastructure/      # docker-compose: postgres, redis, zookeeper, kafka
frontend/              # Next.js App Router
docs/
postman/
```

---

## Java `@ApiId` catalog (logs / MDC)

Controllers tag handlers with `@ApiId("AREA-NNN")` (see `.cursor/rules/java-spring.mdc`). Go ids below are **documentation ids** for support/logs (no Java annotation). Not the same as HTTP `ErrorCode`.

| Prefix | Area |
|--------|------|
| `AUTH-NNN` | auth-service |
| `USER-NNN` | user-service (profiles + admin users) |
| `PLAT-NNN` | platform health |
| `ORG-NNN` | organizations + members |
| `ADMIN-ORG-NNN` | master-admin org APIs (platform; reads `users.is_master_admin` from auth) |
| `ADMIN-STATS-NNN` | platform dashboard counts |
| `ADMIN-AUDIT-NNN` | platform audit log read |
| `ADMIN-ANN-NNN` | platform announcement admin |
| `ANN-NNN` | active announcement for any signed-in user |
| `ADMIN-FLAG-NNN` | platform feature-flag admin |
| `FLAG-NNN` | feature-flag read for any signed-in user |
| `ROLE-NNN` | roles list |
| `WS-NNN` | workspaces |
| `PRJ-NNN` | projects |
| `TASK-NNN` | tasks |
| `SPRINT-NNN` | sprints |
| `WF-NNN` | workflows / task-types |
| `NOTIF-NNN` | collab notifications |
| `INV-NNN` | collab invitations |
| `CMT-NNN` | collab comments |
| `ACT-NNN` | collab activity |
| `RT-NNN` | realtime |
| `DOC-NNN` | document-service (Phase 2) |
| `EMAIL-NNN` | email-service health (Phase 2; mostly Kafka) |

| Id | Method | Path |
|----|--------|------|
| AUTH-001 | GET | `/api/v1/auth/health` |
| AUTH-002 | POST | `/api/v1/auth/register` |
| AUTH-003 | POST | `/api/v1/auth/login` |
| AUTH-004 | POST | `/api/v1/auth/refresh` |
| AUTH-005 | POST | `/api/v1/auth/logout` |
| AUTH-006 | GET | `/api/v1/auth/me` |
| AUTH-007 | PUT | `/api/v1/auth/me` |
| AUTH-008 | POST | `/api/v1/auth/verify-email` |
| AUTH-009 | POST | `/api/v1/auth/resend-verification` |
| AUTH-010 | POST | `/api/v1/auth/forgot-password` |
| AUTH-011 | POST | `/api/v1/auth/reset-password` |
| AUTH-012 | POST | `/api/v1/auth/change-password` |
| AUTH-013 | GET | `/api/v1/auth/sessions` |
| AUTH-014 | POST | `/api/v1/auth/sessions/revoke-all` |
| AUTH-015 | POST | `/api/v1/auth/deactivate` |
| AUTH-016 | POST | `/api/v1/auth/select-organization` |
| AUTH-017 | GET | `/api/v1/auth/organizations` |
| AUTH-020 | POST | `/api/v1/admin/users` | **deprecated** — moved to user-service `USER-020` |
| AUTH-021 | POST | `/api/v1/admin/users/{id}/deactivate` |
| AUTH-022 | POST | `/api/v1/admin/users/{id}/activate` |
| AUTH-023 | POST | `/api/v1/admin/users/{id}/sessions/revoke` |
| AUTH-024 | POST | `/api/v1/admin/users/{id}/reset-password` |
| AUTH-025 | POST | `/api/v1/admin/users/{id}/resend-verification` |
| AUTH-026 | POST | `/api/v1/admin/sessions/impersonate` |
| AUTH-027 | POST | `/api/v1/admin/sessions/impersonate/end` |
| USER-000 | GET | `/api/v1/users/health` |
| USER-001 | GET | `/api/v1/users/me` |
| USER-002 | PUT | `/api/v1/users/me` |
| USER-020 | POST | `/api/v1/admin/users` |
| USER-021 | GET | `/api/v1/admin/users` |
| USER-022 | GET | `/api/v1/admin/users/{id}` |
| USER-023 | PUT | `/api/v1/admin/users/{id}/master-admin` |
| USER-024 | POST | `/api/v1/admin/users/{id}/delete` and `/restore` |
| USER-025 | GET | `/api/v1/admin/users/export` |
| PLAT-001 | GET | `/api/v1/platform/health` |
| ROLE-001 | GET | `/api/v1/roles` |
| ORG-001 | POST | `/api/v1/organizations` (gated when `collabflow.platform.self-serve-org-create=false`; master admin only) |
| ORG-002 | GET | `/api/v1/organizations` (includes `roleId` / `roleName` for org picker) |
| ORG-003 | GET | `/api/v1/organizations/{id}` |
| ORG-004 | PUT | `/api/v1/organizations/{id}` |
| ORG-005 | DELETE | `/api/v1/organizations/{id}` |
| ORG-006 | GET | `/api/v1/organizations/{id}/members` |
| ORG-007 | POST | `/api/v1/organizations/{id}/members` (X-API-Key + JWT; Wave 4 invite accept) |
| ORG-008 | PUT | `/api/v1/organizations/{id}/members/{memberId}` |
| ORG-009 | DELETE | `/api/v1/organizations/{id}/members/{memberId}` |
| ORG-010 | POST | `/api/v1/organizations/{id}/transfer-ownership` |
| ADMIN-ORG-001 | POST | `/api/v1/admin/organizations` |
| ADMIN-ORG-002 | POST | `/api/v1/admin/organizations/{id}/members` |
| ADMIN-ORG-003 | POST | `/api/v1/admin/organizations/{id}/transfer-ownership` |
| ADMIN-ORG-004 | GET | `/api/v1/admin/organizations` |
| ADMIN-ORG-005 | GET | `/api/v1/admin/organizations/{id}` |
| ADMIN-ORG-006 | POST | `/api/v1/admin/organizations/{id}/deactivate` |
| ADMIN-ORG-007 | POST | `/api/v1/admin/organizations/{id}/activate` |
| ADMIN-ORG-008 | GET | `/api/v1/admin/organizations/export` |
| ADMIN-ORG-009 | GET | `/api/v1/admin/organizations/by-user/{userId}` |
| ADMIN-ORG-010 | GET | `/api/v1/admin/organizations/{id}/members` |
| ADMIN-STATS-001 | GET | `/api/v1/admin/stats` |
| ADMIN-AUDIT-001 | GET | `/api/v1/admin/audit` |
| ADMIN-ANN-001 | POST | `/api/v1/admin/announcements` |
| ADMIN-ANN-002 | GET | `/api/v1/admin/announcements` |
| ADMIN-ANN-003 | PUT | `/api/v1/admin/announcements/{id}` |
| ADMIN-ANN-004 | DELETE | `/api/v1/admin/announcements/{id}` |
| ANN-001 | GET | `/api/v1/announcements/active` (any signed-in user) |
| ADMIN-FLAG-001 | GET | `/api/v1/admin/feature-flags` |
| ADMIN-FLAG-002 | PUT | `/api/v1/admin/feature-flags/{key}` |
| ADMIN-FLAG-003 | DELETE | `/api/v1/admin/feature-flags/{key}` |
| FLAG-001 | GET | `/api/v1/feature-flags` (any signed-in user; missing key means enabled) |
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
| TASK-002 | GET | `/api/v1/tasks?projectId=` (filters: `statusId`, `assigneeId`, `priority`, `sprintId`; page/size → `PageResponse`) |
| TASK-003 | GET | `/api/v1/tasks/{id}` |
| TASK-004 | PUT | `/api/v1/tasks/{id}` |
| TASK-005 | DELETE | `/api/v1/tasks/{id}` |
| TASK-006 | PUT | `/api/v1/tasks/{id}/status` |
| TASK-007 | PUT | `/api/v1/tasks/{id}/assign` |
| TASK-008 | GET | `/api/v1/tasks/stats?projectId=` |
| TASK-009 | GET | `/api/v1/tasks/{id}/subtasks` |
| TASK-010 | POST | `/api/v1/tasks/{id}/subtasks` |
| SPRINT-001 | POST | `/api/v1/sprints` |
| SPRINT-002 | GET | `/api/v1/sprints?projectId=` |
| SPRINT-003 | GET | `/api/v1/sprints/{id}` |
| SPRINT-004 | PUT | `/api/v1/sprints/{id}` |
| SPRINT-005 | DELETE | `/api/v1/sprints/{id}` |
| SPRINT-006 | POST | `/api/v1/sprints/{id}/start` |
| SPRINT-007 | POST | `/api/v1/sprints/{id}/complete` |
| SPRINT-008 | POST | `/api/v1/sprints/{id}/tasks` |
| SPRINT-009 | DELETE | `/api/v1/sprints/{id}/tasks/{taskId}` |
| WF-001 | GET | `/api/v1/workflows/default` |
| WF-002 | GET | `/api/v1/workflows/{id}` |
| WF-003 | GET | `/api/v1/workflows/{id}/statuses` |
| WF-004 | GET | `/api/v1/task-types` |
| NOTIF-000 | GET | `/api/v1/collab/health` |
| NOTIF-001 | GET | `/api/v1/notifications` |
| NOTIF-002 | PUT | `/api/v1/notifications/{id}/read` |
| NOTIF-003 | PUT | `/api/v1/notifications/read-all` |
| NOTIF-004 | GET | `/api/v1/notifications/unread-count` |
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
| DOC-000 | GET | `/api/v1/documents/health` |
| DOC-001 | POST | `/api/v1/documents/upload` (multipart; project member) |
| DOC-002 | GET | `/api/v1/documents?projectId=` (latest files) |
| DOC-003 | GET | `/api/v1/documents/{id}/content` (download) |
| EMAIL-000 | GET | `/api/v1/email/health` (direct `:9084`) |

**OpenAPI (Java):** Swagger UI on direct ports — auth `:8081`, platform `:8082`, task `:8083` → `/swagger-ui.html` (send `X-API-Key`). Gateway does not proxy Swagger.

**Logs (local `bootRun`):** `logs/auth/`, `logs/platform/`, `logs/task/` under repo root (`LOG_HOME` override). Go: under each `go-services/*` README.

---

## Portfolio narrative

> CollabFlow is a multi-tenant project tool with a **slim microservice** backend: three Spring Boot (Gradle) services for auth, tenancy/platform, and tasks; two Golang services for collaboration (Kafka consumers) and realtime (WebSocket + Redis); shared Postgres 17; Redis 8; Kafka 3.9 with ZooKeeper (Confluent 7.9). Scope was reduced from 16 services so boundaries stay real and the system stays finishable and explainable. Java owns the sync domain; Go owns async side effects and live updates.
