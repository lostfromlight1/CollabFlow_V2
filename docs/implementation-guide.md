# CollabFlow — Implementation Guide (slim MVP)

**Canonical build plan.** Replaces the archived 16-service / 20-week plan in [archive/legacy-16-service-plan.md](archive/legacy-16-service-plan.md) for day-to-day work.

| | |
|--|--|
| Services | [services-mvp.md](services-mvp.md) |
| Versions | [tech-stack.md](tech-stack.md) |
| Schema | [database.md](database.md) |
| Progress | [progress-tracker.md](progress-tracker.md) |
| Later | [future-plan.md](future-plan.md) |

**Total MVP target:** ~12–14 weeks part-time / learning pace (adjust freely).  
**Rule:** Java core first; Go with `teach-and-build`. Check off items in the tracker when done.

---

## Feature → service map (what first)

Build in this order. Do not skip ahead to documents/search/export.

| Order | Feature | Service | Lang | Notes |
|------:|---------|---------|------|--------|
| 0 | Local infra | `infrastructure/` | Docker | Postgres, Redis, ZK, Kafka |
| 1 | API Gateway skeleton | `api-gateway` | Java | Routes + JWT validation (expand as services appear) |
| 2 | Auth (register/login/refresh) | `auth-service` | Java | Your strength — solid base |
| 3 | Org + workspace + project + RBAC | `platform-service` | Java | Hierarchy + roles |
| 4 | Workflows + tasks (+ assign) | `task-service` | Java | Kafka **produce** |
| 5 | Notifications, invitations, comments, activity | `collab-service` | Go | Kafka **consume** — learn Go here |
| 6 | Realtime WebSocket | `realtime-service` | Go | Redis pub/sub |
| 7 | Basic analytics (counts) | `task-service` / `platform-service` | Java | No new service |
| 8 | E2E polish + portfolio README | all | — | Demo script |

---

## Week-by-week plan

### Phase A — Foundation (Week 1–2)

**Goal:** Run infra locally; empty service shells; gateway can hit health stubs.

#### Week 1 — Infrastructure

**Prereq:** PC tools installed — [setup.md](setup.md) (Docker, JDK 21, Go 1.27, Git).

- [x] Create folders: `infrastructure/`, `java-services/`, `go-services/`
- [x] Docker Compose: Postgres 17, Redis 8, ZooKeeper + Kafka (CP 7.9) — [tech-stack.md](tech-stack.md)
- [x] Verify: DB connect, Redis PING, Kafka up (`docker compose ps`)
- [x] Document env vars (`.env.example`)

**Learning focus:** Docker networking, ZK role vs Kafka brokers.

#### Week 2 — Shells + Gateway

- [x] Gradle Boot 4.1 skeletons: `auth-service`, `platform-service`, `task-service`, `api-gateway`
- [x] Shared conventions — README in `java-services/`
- [x] Gateway routes for `/api/v1/auth/**` (and platform/task path stubs)
- [x] Health endpoints on each Java stub
- [ ] You verify: `bootRun` auth + gateway, curl both URLs (see `java-services/README.md`)

**Done when:** gateway returns auth stub JSON via `http://localhost:8080/api/v1/auth/health`.

---

### Phase B — Java core (Week 3–8)

#### Week 3 — auth-service (part 1)

- [x] Package layout + shared `ApiResponse` / `ErrorResponse` (`:common`)
- [x] Flyway: `users`, `refresh_tokens`
- [x] Register + login + JWT access token
- [x] Password hashing (bcrypt)
- [x] Unit tests for auth happy path
- [ ] You verify with curl (see `auth-service/README.md`)

**Learning focus:** Spring Security + JWT on Boot 4; unified API envelopes.

#### Week 4 — auth-service (part 2)

- [x] Refresh token rotation + logout/revoke (implemented with register/login)
- [x] Redis rate-limit basics (login/register by IP; fail-open if Redis down)
- [x] Publish `user.created` to Kafka after commit
- [x] Wire gateway JWT validation for protected routes (`/me`) + edge `X-API-Key`

**Done when:** Register → login → refresh → `/me` through gateway with Bearer token.


#### Week 5 — platform-service (org)

- [x] Flyway: `organizations`, `organization_members`, `roles`, `permissions`, `role_permissions` + seeds
- [x] Org CRUD + add/remove member + roles
- [x] Permission check helper
- [x] Events: `organization.created`, `organization.member_added`

#### Week 6 — platform-service (workspace)

- [ ] Flyway: `workspaces`, `workspace_members`
- [ ] Workspace CRUD, invite code join
- [ ] Enforce: user must be org member
- [ ] Events: `workspace.*`

#### Week 7 — platform-service (project)

- [ ] Flyway: `projects`, `project_members`
- [ ] Project CRUD + members
- [ ] Gateway routes for org/workspace/project
- [ ] Events: `project.*`

**Done when:** User can create org → workspace → project with RBAC.

#### Week 8 — task-service (workflow + task CRUD)

- [ ] Flyway: workflows, statuses, transitions, task_types, tasks (+ custom fields stubs if time)
- [ ] Seed default workflow
- [ ] Task CRUD + list/filter by project
- [ ] Status change with transition rules
- [ ] Events: `task.created`, `task.updated`, `task.status_changed`, `task.assigned`

**Done when:** Create task, assign user, move status; events visible on Kafka topic.

*(Optional stretch in Week 8–9: sprints table + basic sprint endpoints.)*

---

### Phase C — Golang + events (Week 9–12)

Use skill **`teach-and-build`** every week here.

#### Week 9 — collab-service setup + notifications

- [ ] Go 1.27 module, Gin, GORM, config via env
- [ ] Flyway already applied from Java; GORM models for `notifications`
- [ ] Kafka consumer: `task.assigned` → create notification
- [ ] REST: list / mark read

**Learning focus:** Go packages, GORM, kafka-go consumer, idempotency.

#### Week 10 — invitations + comments

- [ ] Invitations CRUD + accept/decline (token)
- [ ] Comments CRUD + mentions parsing (basic)
- [ ] Events: `comment.added`; notify on mention
- [ ] Gateway routes for collab APIs

#### Week 11 — activity feed + email stub

- [ ] Consume key events → `activity_feed` rows
- [ ] Activity list by entity / user
- [ ] Email: log stub or simple SMTP later (no separate email-service)

**Done when:** Assign task → notification + activity appear via collab API.

#### Week 12 — realtime-service

- [ ] WebSocket endpoint + room subscribe (e.g. project/task)
- [ ] Redis pub/sub for multi-instance readiness (even if single instance)
- [ ] Consume `task.updated` / `comment.added` → broadcast
- [ ] Gateway WS route

**Learning focus:** Go concurrency, WS, Redis pub/sub.

---

### Phase D — Integrate & portfolio (Week 13–14)

#### Week 13 — Integration

- [ ] End-to-end script: register → org → workspace → project → task → comment → see notification + WS update
- [ ] Fix auth/RBAC gaps across services
- [ ] Basic analytics: task counts by status/project (on `task-service`)

#### Week 14 — Polish

- [ ] OpenAPI for Java services; short API examples in README
- [ ] Portfolio case-study paragraph (architecture + what you learned)
- [ ] Mark MVP complete in [progress-tracker.md](progress-tracker.md)
- [ ] Decide Phase 2 from [future-plan.md](future-plan.md) (documents, etc.)

---

## Suggested weekly rhythm (learning)

1. Read the week’s goal + related doc section  
2. Plan mode / `plan-mvp-feature` if unsure  
3. Implement small slice  
4. Verify yourself (curl / Docker logs)  
5. Check off tracker; write 3 bullets “what I learned”

---

## Out of scope until MVP tracker is green

- document-service, search, export, standalone email/analytics services  
- Splitting platform/collab back into many microservices  
- Kubernetes / production hardening (optional stretch after Week 14)

See [future-plan.md](future-plan.md).
