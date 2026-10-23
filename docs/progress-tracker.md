# CollabFlow — Progress Tracker

**Status:** Slim MVP **complete** (Weeks 1–14).  
**Canonical plan:** [implementation-guide.md](implementation-guide.md).  
**What shipped:** [services-mvp.md](services-mvp.md) · [features.md](features.md)

**MVP history only** — Phase 2 waves / UAT frontend contract → [phase-2.md](phase-2.md) · [fe-contract.md](fe-contract.md).  
Use this file as a historical checklist. Deferred product → [future-plan.md](future-plan.md).

---

## Summary

| Phase | Status | Notes |
|-------|--------|-------|
| A Foundation (W1–2) | ✅ | Infra + gateway |
| B Java core (W3–8) | ✅ | Auth, platform, task |
| C Go + events (W9–12) | ✅ | Collab + realtime |
| D Integrate / portfolio (W13–14) | ✅ | E2E + OpenAPI + portfolio README |
| **MVP complete** | ✅ | Slim 6-service MVP |

---

## Services

| Service | Status | Notes |
|---------|--------|-------|
| infrastructure (Compose) | [x] | Postgres 17, Redis 8, ZK + Kafka CP 7.9 |
| api-gateway | [x] | Routes + `X-API-Key` + JWT |
| auth-service | [x] | JWT, refresh, Redis rate-limit, `user.created` |
| platform-service | [x] | Org + workspace + project + RBAC |
| task-service | [x] | Workflow seed, task CRUD/assign/status, stats |
| collab-service | [x] | Notif, invites, comments, activity, email stub |
| realtime-service | [x] | WS + Redis pub/sub + Kafka consume |

---

## Phase A — Foundation

### Week 1 — Infra
- [x] Repo folders created
- [x] Docker Compose: Postgres 17
- [x] Docker Compose: Redis 8
- [x] Docker Compose: ZooKeeper + Kafka (CP 7.9)
- [x] Smoke test DB / Redis / Kafka (`docker compose ps` healthy / up)
- [x] `.env.example` documented

### Week 2 — Shells + Gateway
- [x] Gradle skeletons for Java services
- [x] `api-gateway` health + routing
- [x] Java service health endpoints
- [x] `java-services/` conventions README
- [x] Smoke via gateway / Postman / [e2e-demo.md](e2e-demo.md)

---

## Phase B — Java core

### Week 3–4 — auth-service
- [x] Package layout + `:common` ApiResponse
- [x] Flyway: users, refresh_tokens
- [x] Register / login / JWT
- [x] Refresh + logout
- [x] X-API-Key + Swagger + logging
- [x] Postman collection (`postman/`)
- [x] Redis rate-limit (login/register)
- [x] Kafka: `user.created` (after commit)
- [x] Gateway JWT for `/me` + edge API key
- [x] Tests for auth happy path
- [x] Manual smoke (register → login → me via `:8080` / Postman)

### Week 5 — platform (org)
- [x] Flyway: orgs, members, roles, permissions + seed
- [x] Org CRUD + members
- [x] Permission checks
- [x] Kafka org events
- [x] Manual Postman smoke via gateway

### Week 6 — platform (workspace)
- [x] Flyway: workspaces, workspace_members
- [x] Workspace CRUD + invite join
- [x] Org membership enforced
- [x] Kafka workspace events

### Week 7 — platform (project)
- [x] Flyway: projects, project_members
- [x] Project CRUD + members
- [x] Gateway routes for platform APIs
- [x] Kafka project events

### Week 8 — task-service
- [x] Flyway: workflows + tasks
- [x] Default workflow seed
- [x] Task CRUD + status transitions
- [x] Task assign
- [x] Kafka task events
- [x] Sprints API — shipped in Phase 2 (task-service; see [services-mvp.md](services-mvp.md))

---

## Phase C — Golang

### Week 9 — collab (notifications)
- [x] Go module + Gin + GORM
- [x] Consume `task.assigned` → notification
- [x] Notification REST API
- [x] Gateway collab routes

### Week 10 — collab (invitations + comments)
- [x] Invitations API
- [x] Comments API + mentions
- [x] Kafka comment events / mention notifications

### Week 11 — collab (activity + email stub)
- [x] Activity feed from events
- [x] Activity REST API
- [x] Email stub (log only)

### Week 12 — realtime-service
- [x] WebSocket + rooms
- [x] Redis pub/sub
- [x] Consume task/comment events → broadcast
- [x] Gateway WS route

---

## Phase D — Integrate & portfolio

### Week 13
- [x] E2E demo script documented
- [x] Cross-service bug fix pass (assignerId + dual-publish `task.updated` for WS)
- [x] Basic analytics (`GET /api/v1/tasks/stats`)

### Week 14
- [x] OpenAPI / API examples
- [x] Portfolio write-up in README
- [x] MVP marked complete
- [x] Phase 2 pick — **document-service** next if expanding ([future-plan.md](future-plan.md))

---

## Features checklist (product)

| Feature | Service | Done |
|---------|---------|------|
| Auth | auth | [x] |
| Organization | platform | [x] |
| Workspace | platform | [x] |
| Project | platform | [x] |
| Workflow | task | [x] |
| Task management | task | [x] |
| Notifications | collab | [x] |
| Invitations | collab | [x] |
| Comments | collab | [x] |
| Activity feed | collab | [x] |
| Realtime | realtime | [x] |
| Basic analytics | task | [x] |
| Sprints | task | [x] Phase 2 |
| Documents | document | [ ] stub only — out of UAT |

---

## Learning log (optional)

| Week | Date | What I learned (3 bullets max) |
|------|------|--------------------------------|
| 1 | | |
| … | | |

---

**Last updated:** 2026-10-22
