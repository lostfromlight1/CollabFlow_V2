# task-service

**Port:** 8083 · Java 21 / Boot 4.1 / Gradle  
**Owns:** `status_workflows`, `workflow_statuses`, `workflow_transitions`, `task_types`, `tasks`, `sprints`  
**MVP:** Complete · **Phase 2:** Wave 5 sprints / sub-tasks / filters shipped (checklist below)  

Hierarchy: Organization → Workspace → Project → **Task** (this service).  
ApiIds: [services-mvp.md](../../../docs/services-mvp.md).  
**Ordered waves:** [docs/phase-2.md](../../../docs/phase-2.md).

### See also / Depends on

| Topic | Owner |
|-------|--------|
| Sprints **JWT** (permitAll fix) | [api-gateway](../api-gateway/README.md) — this service owns sprints **API** |
| Org header + membership | [platform-service](../platform-service/README.md) + [common](../common/README.md) `@CurrentOrganizationId` |
| Live board updates | [realtime-service](../../go-services/realtime-service/README.md) |

## What it does

Domain service for **workflows + tasks + sprints**. Seeds a default Kanban workflow and system task types; supports task CRUD, status transitions, assignment, sub-tasks, sprint lifecycle, and basic project stats. After successful DB writes it publishes Kafka events (`task.*`) so **collab** (notifications/activity) and **realtime** (WebSocket) can react. AuthZ uses shared-DB project membership + `task.*` / `sprint.*` permissions.

## Features (MVP — shipped)

| Area | What it does | ApiIds |
|------|----------------|--------|
| Health | Liveness | TASK-000 |
| Workflows (read) | Seeded default Kanban; get default / by id / statuses | WF-001…003 |
| Task types | List system types (Task, Bug, Story, Epic) | WF-004 |
| Tasks | CRUD; list by `projectId`; status change (transition rules); assign/unassign | TASK-001…007 |
| Stats | Counts by status + priority for a project | TASK-008 |
| AuthZ | `project_members` + `task.*` (Admin all; Member no delete; Viewer read) | — |
| Kafka | `task.created`, `task.updated`, `task.deleted`, `task.assigned` (incl. `assignerId`), `task.status_changed`; assign/status also dual-publish `task.updated` for WS | — |

## Features (Phase 2 Wave 5 — shipped)

| Area | What it does | ApiIds |
|------|----------------|--------|
| Sprints | CRUD; start/complete; attach/detach tasks via `sprint_id` | SPRINT-001…009 |
| Sub-tasks | List/create children via `parent_task_id` | TASK-009, TASK-010 |
| Task list | Filters (`statusId`, `assigneeId`, `priority`, `sprintId`) + `PageResponse` | TASK-002 |
| Schema | Flyway V2: `sprints` + FK on `tasks.sprint_id`; `sprint.*` permissions | — |

### MVP / Wave 5 learning notes

| Piece | What / why |
|-------|------------|
| **Transitions** | Status moves only along `workflow_transitions` — avoids illegal Kanban jumps |
| **Dual-publish `task.updated`** | Realtime listens to one topic for live board updates (also used on sprint attach/detach) |
| **One ACTIVE sprint** | App enforces at most one ACTIVE sprint per project |
| **Flyway** | Table `flyway_task_history` (V1 workflows + tasks; V2 sprints); needs auth + platform migrations first |

## Stable seed IDs

| Entity | UUID |
|--------|------|
| Default workflow | `c0000000-0000-4000-8000-000000000001` |
| Status To Do | `c0000000-0000-4000-8000-000000000012` |
| Status In Progress | `c0000000-0000-4000-8000-000000000013` |
| Status Done | `c0000000-0000-4000-8000-000000000015` |
| Type Task | `d0000000-0000-4000-8000-000000000001` |

Default board: Backlog → To Do → In Progress → In Review → Done (forward transitions).

## Phase 2 — TODO

Coordinate with gateway (sprints JWT), platform/org header, and common pagination.

### Admin / org tenancy

- [ ] **Honor `X-Organization-Id`** — project (and task) must belong to the selected org
- [x] **Gateway sprints path** — JWT required (Wave 0); task owns sprints API (Wave 5)

### Core product

- [x] **Sprints API** — Flyway `sprints` + CRUD; attach/detach tasks via `sprint_id`; start/complete
- [x] **Richer list filters** — status, assignee, priority, sprint (Postgres)
- [x] **Pagination** — common `PageResponse` on list tasks
- [x] **Sub-tasks API** — create/list children via `parent_task_id`
- [ ] **Task dependencies** — blocks/relates tables from [database.md](../../../docs/database.md) (not migrated) + APIs
- [ ] **Custom fields** — admin + values on tasks (design-only tables today)
- [ ] **Text search** on task list (Postgres `ILIKE` / later ES)

### Workflow / types

- [ ] **Workflow admin CRUD** — statuses/transitions beyond the seeded default (workspace-scoped)
- [ ] **Per-project workflow** — set/get project workflow when not fully exposed
- [ ] **Custom task types** — create beyond system seeds

### Hardening

- [ ] **Bulk update** — status/assignee for many ids (respect transitions + Kafka)
- [ ] **Watchers** — optional (design-only in database.md) or rely on collab notifications
- [ ] **Assignee checks under org login** — still project member; re-validate with org context
- [ ] **Idempotent assign/status** — no-op when unchanged (fewer duplicate Kafka/WS events)

### Stretch / later

- Hand off rich metrics to analytics-service (keep `stats` for MVP)  
- CSV export (export-service)  
- Elasticsearch task search  

## Security

| Layer | How |
|-------|-----|
| Client gate | Header `X-API-Key` = env `API_KEY` |
| User auth | `Authorization: Bearer`; `@CurrentUserId` |
| Authorization | Project membership + `task.*` / `sprint.*` permissions |

Prefer gateway `:8080`. Swagger: http://localhost:8083/swagger-ui.html

## Layout

`controller` / `service`/`impl` / `dto` / `repository` / `model` / `config` / `event`  
Conventions: `@CurrentUserId` + `@ApiId` — `.cursor/rules/java-spring.mdc`.

## Run

Needs Postgres (auth + platform first, including projects) + Kafka. Secrets from repo-root `.env`.

```powershell
cd D:\codes\CollabFlow_V2\backend\java-services
.\gradlew.bat :task-service:bootRun
```

Logs: `logs/task/task-service.yyyy-MM-dd.log` under repo root when using `bootRun`.

## Try it (via gateway)

```powershell
$k = $env:API_KEY
$base = "http://localhost:8080"
# After login + create project — Bearer required
curl "$base/api/v1/workflows/default" -H "X-API-Key: $k" -H "Authorization: Bearer <accessToken>"

curl "$base/api/v1/tasks?projectId=<projectId>&page=0&size=20" -H "X-API-Key: $k" -H "Authorization: Bearer <accessToken>"

curl "$base/api/v1/sprints?projectId=<projectId>" -H "X-API-Key: $k" -H "Authorization: Bearer <accessToken>"

curl "$base/api/v1/tasks/stats?projectId=<projectId>" -H "X-API-Key: $k" -H "Authorization: Bearer <accessToken>"
```

Full path: [e2e-demo.md](../../../docs/e2e-demo.md).
