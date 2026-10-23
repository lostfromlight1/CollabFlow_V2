# realtime-service

**Port:** 9082 · Go **1.27** / Gin / gorilla/websocket / go-redis / kafka-go / GORM (read-only ACL)  
**Owns:** no Postgres tables (Kafka + Redis; **reads** shared `project_members` / `organization_members` for subscribe ACL)  
**MVP:** Complete · **Phase 2:** Wave 6 ACL checklist below  

Live updates for open boards/pages. Catalog: [services-mvp.md](../../../docs/services-mvp.md).  
Durable inbox is **collab-service**, not this service.  
**Ordered waves:** [docs/phase-2.md](../../../docs/phase-2.md).

### See also / Depends on

| Topic | Owner |
|-------|--------|
| WS JWT at edge (optional later) | [api-gateway](../../java-services/api-gateway/README.md) — MVP/Phase 2 keep **downstream** check |
| Org membership for subscribe ACL | [platform-service](../../java-services/platform-service/README.md) / shared DB |
| Toast rooms / notification push | [collab-service](../collab-service/README.md) |

## What it does

Consumes selected Kafka topics, publishes to **Redis pub/sub**, and fans out to local WebSocket clients subscribed to **rooms** (`project:…`, `task:…`, optional `org:…`). Redis keeps the design multi-instance-ready even when you run one process.

```text
Kafka (task.updated / comment.added)
  → Redis channel
  → each realtime instance
  → WebSocket clients in that room
```

## Features (MVP — shipped)

| Area | What it does |
|------|----------------|
| Health | `GET /api/v1/realtime/health` |
| WebSocket | `GET /api/v1/realtime/ws?token=<JWT>` |
| Rooms | Client JSON: `{"action":"subscribe","room":"project:<id>"}` / `task:<id>` |
| Kafka → Redis → WS | `task.updated` → project + task rooms; `comment.added` → entity room |
| Broadcast | `POST /api/v1/realtime/broadcast` — manual/dev publish only |
| Redis | Pattern subscribe for fan-out across instances |

### Protocol (quick)

```
ws://localhost:9082/api/v1/realtime/ws?token=<JWT>
# with org context (optional):
ws://localhost:9082/api/v1/realtime/ws?token=<JWT>&organizationId=<orgUuid>
# via gateway:
ws://localhost:8080/api/v1/realtime/ws?token=<JWT>&apiKey=<API_KEY>
```

Headers (optional): `X-Organization-Id`, `X-Request-Id`.

Subscribe rooms: `project:<uuid>`, `task:<uuid>`, `org:<uuid>`, `user:<self>`.  
Denied subscribe → WS message `{"type":"error","code":"FORBIDDEN",…}` (connection stays open).

Receive examples: `{"type":"task.updated",…}`, `{"type":"comment.added",…}`.

### MVP learning notes

| Piece | What / why |
|-------|------------|
| **Redis bus** | Kafka consumer may hit one pod; Redis reaches all realtime instances |
| **WS auth** | Gateway permits WS path; JWT checked here via `?token=` (browsers + custom headers) |
| **Subscribe ACL** | Shared Postgres EXISTS on `project_members` (or org member of project's org) — same DB as platform, no HTTP hop |
| **Requires Redis + Postgres** | Process fails startup if either is down |

## Phase 2 — TODO

Coordinate with gateway (WS JWT / API key), platform (membership ACL), collab (notification toasts).

### Security / tenancy

- [x] **Subscribe ACL** — must be project (or org) member before joining a room; reject otherwise
- [x] **Honor org context** — validate project belongs to selected org / optional `org:` rooms
- [ ] **Gateway JWT for WS** — validate at edge **or** keep downstream-only and document clearly
- [x] **Lock down `/broadcast`** — admin-only or disable outside `dev`

### Events / UX

- [ ] **More Kafka topics** — e.g. `task.assigned`, `task.status_changed` as distinct WS types (or richer payload)
- [ ] **User private room** — `user:{id}` for notification toasts (collab or dual-write) *(ACL for `user:{self}` already enforced)*
- [ ] **Presence / typing** — optional chat-like UX
- [ ] **Reconnect guidance** — client docs; optional simple Redis buffer (keep lean)

### Hardening

- [x] **Heartbeat / idle timeout** — code + docs (server ping 30s; idle read 90s)
- [ ] **Rate-limit** subscribe / broadcast
- [x] **Structured logs** — `requestId` / `userId` (parity with Java MDC)

### Stretch / later

- Load-test notes for multi-instance  
- Message schema versioning  

## Env

| Variable | Default |
|----------|---------|
| `REALTIME_HTTP_PORT` | `9082` |
| `POSTGRES_*` | same as other services (`localhost` / `5432` / `collabflow`) |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `KAFKA_GROUP_ID` | `realtime-service` |
| `JWT_SECRET` | same as auth |
| `GIN_MODE` / `APP_ENV` | broadcast allowed when `GIN_MODE=debug` (Gin default if unset) or `APP_ENV=dev` |

## Run

Redis + Kafka + Postgres up.

```powershell
cd D:\codes\CollabFlow_V2\backend\go-services\realtime-service
go run ./cmd/server
```

## Smoke

1. Open a WS client → subscribe `project:{{projectId}}` (must be member)  
2. Update a task name (or assign/status — dual-publishes `task.updated`) in Postman  
3. See `task.updated` on the socket  
4. Subscribe as a non-member → expect `type:error` / `FORBIDDEN`  
5. `POST /broadcast` with `GIN_MODE=release` and `APP_ENV` unset → `403`  

Full path: [e2e-demo.md](../../../docs/e2e-demo.md).

Conventions: `.cursor/rules/golang-services.mdc`. Learning: skill `teach-and-build`.
