# collab-service

**Port:** 9081 · Go **1.27** / Gin / GORM / kafka-go  
**Owns:** `notifications`, `invitations`, `comments`, `comment_mentions`, `activity_feed`, `processed_events`  
**MVP:** Complete · **Phase 2:** TODO checklist below  

Async side effects + collaboration HTTP for CollabFlow. Catalog: [services-mvp.md](../../../docs/services-mvp.md).  
**Ordered waves:** [docs/phase-2.md](../../../docs/phase-2.md).

### See also / Depends on

| Topic | Owner |
|-------|--------|
| Produce `email.send` (verify/reset) | [auth-service](../../java-services/auth-service/README.md) |
| Produce `email.send` (invitation) | **this service** on invite create |
| Accept invite → membership API | [platform-service](../../java-services/platform-service/README.md) ORG-007 (+ WS/PRJ members) |
| Workspace **invite code** vs email **invitation** | See below |

### Invite code vs email invitation

| Mechanism | Owner | What it is |
|-----------|--------|------------|
| **Workspace invite code** | **platform-service** | Short join code on a workspace (`POST /workspaces/join`). Caller must **already** be an org member. |
| **Email invitation** | **collab-service** | Row in `invitations` + Kafka `email.send` (`template=invitation`). On **accept**, collab calls platform `POST /organizations/{id}/members` (and workspace/project members when needed). Collab does **not** write `organization_members`. |

## What it does

After Java services write to the DB and publish Kafka events, **collab** turns them into **notifications** and **activity** rows. It also owns sync APIs for **invitations** and **comments** (with `@UUID` mentions). Email is a **log stub** (`EMAIL_STUB`) — including Kafka `email.send` from auth **and** invitation create — not SMTP.

Mental model: **inbox + history** (durable). Live board updates are **realtime-service**, not this service.

## Features (MVP — shipped)

| Area | What it does | Notes |
|------|----------------|-------|
| Health | `GET /api/v1/collab/health` | NOTIF-000 |
| Notifications | Consume `task.assigned` → row; list / mark read / read-all / unread-count | + in-process `EMAIL_STUB` |
| Email | Consume `email.send` → structured `EMAIL_STUB` log | Idempotent; no SMTP yet |
| Mentions | On comment create → `COMMENT_MENTION` notification | In-process (not Kafka) |
| Invitations | Create (+ `email.send`), list mine, list by org, get/accept by token, decline | Accept → platform HTTP |
| Comments | CRUD + mention parse; produce `comment.added` | |
| Activity | Consume `task.assigned`, `task.created`, `comment.added` → `activity_feed`; list by org/entity/user | |
| Idempotency | `processed_events` for Kafka handlers | |
| Auth | JWT on most routes; public `GET …/invitations/token/:token` | |

### MVP learning notes

| Piece | What / why |
|-------|------------|
| **Kafka consume** | Side effects after Java commit — collab can be down briefly without blocking task write |
| **GORM AutoMigrate** | Collab-owned tables; Java Flyway owns auth/platform/task schema |
| **Start before assign** | Consumers use `LastOffset` — start collab before demo assign or re-assign |
| **Platform HTTP on accept** | Sync call (RestClient-style) so platform keeps ownership of membership rows |

## Phase 2 — TODO

Coordinate with auth (admin users), platform (membership on accept), gateway (org header), and future email-service.

### Admin / auth / email

- [x] **Consume `email.send` (stub)** — structured log (`to`, `subject`, `template`, `correlationId`); real SMTP / email-service = Wave 8
- [ ] **Real email (or email-service)** — replace stub for assignment + invitation delivery
- [x] **Invitation email on create** — publish `email.send` with `template=invitation` + token link
- [x] **Org-scoped lists** — honor `X-Organization-Id` on notifications / activity / invites
- [x] **Accept invite → platform membership** — HTTP `POST /organizations/{id}/members` (+ workspace/project when typed); forward Bearer JWT + `X-API-Key`

### Notifications / activity

- [ ] **More notification types** — invite received, status change, comment on assigned task (new consumers as needed)
- [x] **Unread count endpoint** — `GET /api/v1/notifications/unread-count` (NOTIF-004)
- [ ] **Notification preferences** — email on/off per type (table + API)
- [ ] **More platform events → activity** — e.g. `organization.member_added` / project events (optional)
- [x] **Pagination** on notifications / activity / invitations (`page` / `size`, PageResponse-shaped `data`)

### Comments / invites

- [ ] **Reactions / richer markdown** — if needed (design-only tables in database.md)
- [ ] **Invite expiry UX** — clear errors; resend invite
- [ ] **Extend idempotency** — `processed_events` for every new consumer

### Stretch / later

- Split into notification/comment microservices — **not** Phase 2  
- Standalone **email-service** ([future-plan.md](../../../docs/future-plan.md))  

## Security

| Layer | How |
|-------|-----|
| JWT | Same `JWT_SECRET` as auth/gateway |
| Public | Invite get-by-token only (MVP) |
| Platform call | `X-API-Key` + forwarded Bearer from accept |

Prefer calling via gateway `:8080` with `X-API-Key` + Bearer (+ optional `X-Organization-Id`).

## Env

| Variable | Default |
|----------|---------|
| `COLLAB_HTTP_PORT` | `9081` |
| `POSTGRES_*` | same as Java `.env` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `KAFKA_GROUP_ID` | `collab-service` |
| `JWT_SECRET` | same as auth/gateway |
| `PLATFORM_SERVICE_URI` | `http://localhost:8082` |
| `API_KEY` | same as Java `.env` (required for accept → platform) |
| `APP_PUBLIC_URL` | `http://localhost:3000` (invite link in email body) |

## Layout

```
cmd/server/main.go
internal/config/
internal/database/
internal/model/
internal/repository/
internal/service/
internal/handler/
internal/middleware/   # JWT ≈ Spring Security filter
internal/kafka/        # consumers ≈ @KafkaListener; producer for comment.added + email.send
internal/platform/     # HTTP client → platform membership APIs
internal/email/        # StubSend / StubSendMessage log only
```

Conventions: `.cursor/rules/golang-services.mdc`. Learning: skill `teach-and-build`.

## Run

Postgres + Kafka + platform up. Same DB `collabflow` as Java. Load `API_KEY` from repo-root `.env`.

```powershell
cd D:\codes\CollabFlow_V2\backend\go-services\collab-service
$env:API_KEY = (Get-Content ..\..\.env | Where-Object { $_ -match '^API_KEY=' }) -replace '^API_KEY=',''
go run ./cmd/server
```

## Smoke

1. Start collab **before** assign  
2. Assign a task → log: `notification created` + activity + `EMAIL_STUB`  
3. `GET /api/v1/notifications` and `GET /api/v1/notifications/unread-count` (optional `X-Organization-Id`)  
4. Create invitation → Kafka `email.send` → `EMAIL_STUB` with `template=invitation`  
5. Accept invite → platform adds org member (no collab write to `organization_members`)  

Full path: [e2e-demo.md](../../../docs/e2e-demo.md).
