# CollabFlow — Implementation Guide (slim MVP)

**Status:** Slim MVP **complete** (Weeks 1–14). This file is the **historical build plan** and learning path.  
For day-to-day API/schema truth use [services-mvp.md](services-mvp.md), [tech-stack.md](tech-stack.md), [database.md](database.md).  
Next expansion: [future-plan.md](future-plan.md). Tracker: [progress-tracker.md](progress-tracker.md).

Replaces the archived 16-service / 20-week plan in [archive/legacy-16-service-plan.md](archive/legacy-16-service-plan.md).

**Rule that got us here:** Java core first; Go with `teach-and-build`. Kafka **after** successful DB write.

---

## Feature → service map (what we built)

| Order | Feature | Service | Lang | Notes |
|------:|---------|---------|------|--------|
| 0 | Local infra | `infrastructure/` | Docker | Postgres, Redis, ZK, Kafka |
| 1 | API Gateway | `api-gateway` | Java | Routes + API key + JWT |
| 2 | Auth | `auth-service` | Java | Register/login/refresh |
| 3 | Org / workspace / project + RBAC | `platform-service` | Java | Hierarchy |
| 4 | Workflows + tasks (+ assign) | `task-service` | Java | Kafka **produce** + stats |
| 5 | Notifications, invitations, comments, activity | `collab-service` | Go | Kafka **consume** |
| 6 | Realtime WebSocket | `realtime-service` | Go | Redis pub/sub |
| 7 | Basic analytics | `task-service` | Java | `GET /tasks/stats` |
| 8 | E2E + portfolio | all | — | [e2e-demo.md](e2e-demo.md), root README |

---

## Week-by-week plan (all checked)

### Phase A — Foundation (Week 1–2)

#### Week 1 — Infrastructure

- [x] Folders: `infrastructure/`, `java-services/`, `go-services/`
- [x] Docker Compose: Postgres 17, Redis 8, ZooKeeper + Kafka (CP 7.9)
- [x] Verify infra healthy; `.env.example` documented

#### Week 2 — Shells + Gateway

- [x] Gradle Boot 4.1 multi-module + gateway routes + health
- [x] Conventions README; smoke via gateway / Postman / e2e doc

---

### Phase B — Java core (Week 3–8)

#### Week 3–4 — auth-service

- [x] `:common` envelopes; Flyway users/refresh_tokens
- [x] Register/login/JWT/refresh/logout; bcrypt; tests
- [x] Redis rate-limit; Kafka `user.created`; gateway JWT + `X-API-Key`

#### Week 5–7 — platform-service

- [x] Org + RBAC seed; workspace + invite join; project + members
- [x] Gateway routes; Kafka `organization.*` / `workspace.*` / `project.*`

#### Week 8 — task-service

- [x] Workflows + tasks Flyway; default workflow seed
- [x] Task CRUD, status transitions, assign; Kafka task events
- [ ] Optional sprints — **deferred** (not required for MVP complete)

---

### Phase C — Golang + events (Week 9–12)

#### Week 9–11 — collab-service

- [x] Gin + GORM; consume `task.assigned` → notifications
- [x] Invitations + comments + mentions; `comment.added`
- [x] Activity feed; email log stub; gateway collab routes

#### Week 12 — realtime-service

- [x] WebSocket rooms; Redis pub/sub; consume `task.updated` / `comment.added`
- [x] Gateway `/api/v1/realtime/**`

---

### Phase D — Integrate & portfolio (Week 13–14)

#### Week 13 — Integration

- [x] E2E script: [e2e-demo.md](e2e-demo.md)
- [x] RBAC / assignerId / dual-publish for WS
- [x] Task stats analytics

#### Week 14 — Polish

- [x] OpenAPI (Java springdoc); API examples in README
- [x] Portfolio case-study paragraph
- [x] MVP marked complete in tracker
- [x] Phase 2 pick: **document-service** when expanding

---

## After MVP

1. Do **not** resurrect the 16-service split without a clear need  
2. Expand from [future-plan.md](future-plan.md) (documents first)  
3. Keep [tech-stack.md](tech-stack.md) / [services-mvp.md](services-mvp.md) / [database.md](database.md) in sync with every schema or API change  

Out of scope until you open Phase 2: document-service, search, export, standalone email/analytics, K8s hardening.

---

## Suggested rhythm (for Phase 2+)

1. Read feature + [plan-mvp-feature](../.cursor/skills/plan-mvp-feature/SKILL.md) if unsure  
2. Small vertical slice  
3. Verify (Postman / e2e)  
4. Update tracker + docs in the same change  

---

**Last updated:** 2026-10-22
