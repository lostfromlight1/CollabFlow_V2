# CollabFlow Features

**Version:** 1.1.0 (slim MVP finalized)  
**Last updated:** 2026-10-22  
**MVP status:** Complete (Weeks 1–14) — [progress-tracker.md](progress-tracker.md)

**Service mapping (canonical):** [services-mvp.md](services-mvp.md)

| Product area | Deployable |
|--------------|------------|
| Auth | `auth-service` |
| Org / workspace / project / RBAC | `platform-service` |
| Workflows + tasks + basic stats | `task-service` |
| Notifications, invitations, comments, activity | `collab-service` |
| Realtime WebSocket | `realtime-service` |
| Edge | `api-gateway` |

---

## MVP features (shipped)

### 1. User authentication — `auth-service`

- Register / login with bcrypt passwords  
- JWT access tokens + refresh token rotation / logout  
- Redis rate-limit on login/register (fail-open if Redis down)  
- Kafka: `user.created` after register  
- Gateway: `X-API-Key` + Bearer for `/me`

**Not in MVP:** email verification, password reset, MFA.

---

### 2. Organization management — `platform-service`

- Create org (slug), list mine, get/update/soft-delete  
- Members: add / update role / remove  
- System roles + permission checks (`organization.*`)  
- Kafka: `organization.created`, `organization.updated`, `organization.deleted`, `organization.member_added`, `organization.member_removed`

**Not in MVP:** billing/subscription plans, org analytics dashboards.

---

### 3. Workspace management — `platform-service`

- CRUD workspaces under an org  
- Members + invite-code join  
- Org membership enforced before workspace access  
- Kafka: `workspace.*`

---

### 4. Project management — `platform-service`

- CRUD projects under a workspace  
- Members + roles  
- Soft archive/delete as implemented  
- Kafka: `project.*`

---

### 5. Status workflows — `task-service`

- Seeded default workflow + statuses + transitions  
- Read APIs: default workflow, by id, statuses, task types  
- Status change on tasks enforces transitions

**Not in MVP:** admin UI/API to create arbitrary workflows, workflow cloning.

---

### 6. Task management — `task-service`

- Task CRUD, list by project  
- Assign user, change status  
- Kafka: `task.created|updated|deleted|assigned|status_changed`  
- Dual-publish `task.updated` on assign/status for realtime WS

**Not in MVP:** sprints API, custom fields admin, dependencies API, bulk ops, cloning.

---

### 7. Notifications — `collab-service`

- Consume `task.assigned` → in-app notification  
- Mention notifications from comments (in-process)  
- List / mark read / mark all read  
- Email: **log stub** only

---

### 8. Invitations — `collab-service`

- Create invitation, list, org list  
- Accept by token; delete  
- Gateway routes under `/api/v1/invitations/**`

---

### 9. Comments — `collab-service`

- CRUD comments on entities  
- Basic @mention parsing → notify  
- Kafka: `comment.added`

**Not in MVP:** reactions, rich attachments, nested threads beyond basic model.

---

### 10. Activity feed — `collab-service`

- Consume key events (`task.created`, `comment.added`, …) → `activity_feed`  
- List by org / entity / user

---

### 11. Realtime updates — `realtime-service`

- WebSocket endpoint + room subscribe (`project:…`, `task:…`)  
- Consume `task.updated` / `comment.added` → Redis pub/sub → broadcast  

**Not in MVP:** presence, typing indicators.

---

### 12. Basic analytics — `task-service`

- `GET /api/v1/tasks/stats?projectId=` — counts by status/priority  

**Not in MVP:** dedicated analytics-service, burndown, velocity, workspace-wide dashboards.

---

### 13. API access — all + gateway

- REST `/api/v1/...` via gateway `:8080`  
- OpenAPI/Swagger on Java services (springdoc 3.1.1)  
- `X-API-Key` + JWT  

Demo: [e2e-demo.md](e2e-demo.md) · Postman: `postman/`.

---

## Phase 2+ (deferred)

See [future-plan.md](future-plan.md). Do **not** invent endpoints/tables for these until expanding scope.

| Feature | Suggested home | Notes |
|---------|----------------|--------|
| Document management | `document-service` (Go) | MinIO/S3; schema sketched in database.md |
| Standalone email | `email-service` | Replace collab log stub |
| Tags & labels | platform / documents | |
| Watchers | task or collab | |
| Sprint management | `task-service` | Gateway path reserved; no handlers yet |
| Custom fields admin | `task-service` | |
| Search | `search-service` | Elasticsearch |
| Export | `export-service` | CSV/Excel jobs |
| Advanced analytics | `analytics-service` | Beyond task stats |
| Webhooks | optional new service | |

---

## Priority matrix (final)

### Phase 1 — MVP (done)

1. Auth  
2. Organization  
3. Workspace  
4. Project  
5. Workflow (seeded) + tasks  
6. Notifications  
7. Invitations  
8. Comments  
9. Activity feed  
10. Realtime  
11. Basic task stats  
12. Gateway + OpenAPI  

**Timeline (actual plan):** ~12–14 weeks learning pace — [implementation-guide.md](implementation-guide.md).

### Phase 2 — Collaboration & media

Documents, richer email, tags, watchers.

### Phase 3 — Insights & data out

Sprints (in task-service), custom fields, search, export, advanced analytics.

### Phase 4 — Enterprise polish

Webhooks, hardening, optional split of merged services.

---

## Cross-service flows (shipped)

**Task assignment**

1. `task-service` updates assignee + produces `task.assigned` (+ `task.updated`)  
2. `collab-service` creates notification  
3. `realtime-service` broadcasts to project/task rooms  

**Comment with mention**

1. `collab-service` saves comment + produces `comment.added`  
2. Mention → in-app notification  
3. Activity row + realtime broadcast  

**Hierarchy create**

1. auth → JWT  
2. platform: org → workspace → project (RBAC)  
3. task: create/assign under project  

---

## Feature × service summary

| Feature | Service | Phase | Done |
|---------|---------|-------|------|
| Authentication | auth-service | MVP | Yes |
| Organization | platform-service | MVP | Yes |
| Workspace | platform-service | MVP | Yes |
| Project | platform-service | MVP | Yes |
| Workflow (seeded) | task-service | MVP | Yes |
| Tasks | task-service | MVP | Yes |
| Task stats | task-service | MVP | Yes |
| Notifications | collab-service | MVP | Yes |
| Invitations | collab-service | MVP | Yes |
| Comments | collab-service | MVP | Yes |
| Activity feed | collab-service | MVP | Yes |
| Realtime | realtime-service | MVP | Yes |
| API gateway / OpenAPI | gateway + Java | MVP | Yes |
| Sprints | task-service | Phase 3 | No |
| Documents | document-service | Phase 2 | No |
| Search / export / analytics-service | future | Phase 3 | No |

---

**Document version:** 1.1.0  
**Last updated:** 2026-10-22
