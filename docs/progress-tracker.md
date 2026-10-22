# CollabFlow — Progress Tracker

**How to use:** Change `[ ]` → `[x]` when a item is done.  
**Canonical plan:** [implementation-guide.md](implementation-guide.md).

**Legend:** ⬜ not started · 🟨 in progress · ✅ done (use checkboxes below; update the summary table by hand).

---

## Summary

| Phase | Status | Notes |
|-------|--------|-------|
| A Foundation (W1–2) | 🟨 | Week 1 done; Week 2 skeletons compiled |
| B Java core (W3–8) | 🟨 | Week 8 task-service done; next Phase C Go (Week 9) |
| C Go + events (W9–12) | 🟨 | Week 9 collab notifications (consume task.assigned) |
| D Integrate / portfolio (W13–14) | ⬜ | |
| **MVP complete** | ⬜ | All phases green |

---

## Services

| Service | Status | First usable when |
|---------|--------|-------------------|
| infrastructure (Compose) | [x] | Week 1 — Postgres/Redis healthy; Kafka up |
| api-gateway | [x] skeleton | Week 2 — routes stubbed |
| auth-service | [x] usable | Week 4 — JWT + Kafka + Redis rate limit |
| platform-service | [x] org+workspace+project usable | Week 7 — projects |
| task-service | [x] usable | Week 8 — workflow seed + task CRUD |
| collab-service | [x] notifications usable | Week 9 — task.assigned → notify |
| realtime-service | [ ] | Week 12 |

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
- [x] `api-gateway` health + stub routing
- [x] Java service health endpoints
- [x] `java-services/` conventions README
- [ ] Smoke: `bootRun` auth + gateway, curl both URLs

---

## Phase B — Java core

### Week 3–4 — auth-service
- [x] Package layout (controller/service/dto/model/…) + `:common` ApiResponse
- [x] Flyway: users, refresh_tokens
- [x] Register / login / JWT
- [x] Refresh + logout
- [x] X-API-Key + Swagger + logging (shared standards)
- [x] Postman collection (`postman/`)
- [x] Redis rate-limit (login/register)
- [x] Kafka: `user.created` (after commit)
- [x] Gateway JWT for `/me` + edge API key
- [x] Tests for auth happy path
- [ ] Manual curl smoke (register → login → me via `:8080`)

### Week 5 — platform (org)
- [x] Flyway: orgs, members, roles, permissions + seed
- [x] Org CRUD + members
- [x] Permission checks
- [x] Kafka org events (`organization.created`, `organization.member_added`)
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
- [x] Flyway: workflows + tasks (+ related)
- [x] Default workflow seed
- [x] Task CRUD + status transitions
- [x] Task assign
- [x] Kafka task events
- [ ] (Optional) sprints basics

---

## Phase C — Golang

### Week 9 — collab (notifications)
- [x] Go module + Gin + GORM
- [x] Consume `task.assigned` → notification
- [x] Notification REST API
- [x] Gateway collab routes (partial)

### Week 10 — collab (invitations + comments)
- [x] Invitations API
- [x] Comments API + mentions
- [x] Kafka comment events / mention notifications

### Week 11 — collab (activity + email stub)
- [x] Activity feed from events
- [x] Activity REST API
- [x] Email stub (log only OK)

### Week 12 — realtime-service
- [ ] WebSocket + rooms
- [ ] Redis pub/sub
- [ ] Consume task/comment events → broadcast
- [ ] Gateway WS route

---

## Phase D — Integrate & portfolio

### Week 13
- [ ] E2E demo script documented
- [ ] Cross-service bugfix pass
- [ ] Basic analytics endpoints

### Week 14
- [ ] OpenAPI / API examples
- [ ] Portfolio write-up in README
- [ ] MVP marked complete in summary table
- [ ] Phase 2 pick from future-plan (optional)

---

## Features checklist (product)

| Feature | Service | Done |
|---------|---------|------|
| Auth | auth | [ ] |
| Organization | platform | [ ] |
| Workspace | platform | [ ] |
| Project | platform | [ ] |
| Workflow | task | [ ] |
| Task management | task | [ ] |
| Notifications | collab | [ ] |
| Invitations | collab | [ ] |
| Comments | collab | [ ] |
| Activity feed | collab | [ ] |
| Realtime | realtime | [ ] |
| Basic analytics | task/platform | [ ] |

---

## Learning log (optional)

Add a line after each week:

| Week | Date | What I learned (3 bullets max) |
|------|------|--------------------------------|
| 1 | | |
| 2 | | |
| … | | |

---

**Last updated:** 2026-10-21
