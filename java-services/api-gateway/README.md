# api-gateway

**Port:** 8080 · Java 21 / Spring Cloud Gateway / Gradle  
**Role:** Single HTTP edge for `/api/v1/**` — route, `X-API-Key`, JWT on protected paths  
**MVP:** Complete · **Phase 2:** Waves 0–2 largely shipped (see checklist)

Upstream catalog: [services-mvp.md](../../docs/services-mvp.md).  
**Ordered waves:** [docs/phase-2.md](../../docs/phase-2.md).

### See also / Depends on

| Topic | Owner |
|-------|--------|
| Org login + verify/reset public paths | [auth-service](../auth-service/README.md) |
| Sprints **API** (handlers/table) | [task-service](../task-service/README.md) — gateway only fixes JWT |
| WS JWT check (keep downstream) | [realtime-service](../../go-services/realtime-service/README.md) |
| Request-id MDC in servlet services | [common](../common/README.md) — gateway emits `X-Request-Id` |
| `/users/**` route | Wave 3 **user-service** |

## What it does

CollabFlow’s front door. Clients (Postman, future admin portal, future app UI) talk to **`:8080`**. The gateway checks **`X-API-Key`**, validates **Bearer JWT** on protected routes (same `JWT_SECRET` as auth), and proxies to auth / platform / task / collab / realtime. Downstream services still enforce their own API key + JWT (defense in depth).

**Phase 2 direction:** align public paths with admin-provisioned auth, route **user-service**, forward **org context**, CORS for admin portal, and harden the edge (rate limit, timeouts, correlation ids).

## Features (MVP — shipped)

| Feature | What it does |
|---------|----------------|
| Path routing | `auth→8081`, `platform→8082`, `task→8083`, `user→8084`, `collab→9081`, `realtime→9082`, `documents→9083`, email not HTTP |
| `X-API-Key` | Required on all `/api/**`; WebSocket may use `?apiKey=` |
| JWT resource server | HS256; product paths need Bearer |
| Public auth | register/login/refresh/logout + verify/forgot/reset + healths |
| Invite peek | `GET /api/v1/invitations/token/**` permitAll (accept still JWT) |
| WebSocket | `/api/v1/realtime/ws` permitAll; token inside realtime-service |
| Sprints | Routed to task + **JWT required** (handlers in task-service) |
| CORS | `CORS_ALLOWED_ORIGINS` (e.g. `http://localhost:3000`) |
| Actuator | health/info public; other actuator locked outside `dev`/`local` |

### MVP learning notes

| Piece | What / why |
|-------|------------|
| **API key at edge** | Stops anonymous internet traffic; services still check the same key |
| **JWT at edge** | Reject bad/missing tokens before hitting Java/Go services |
| **WS permitAll** | Browsers struggle with custom headers on WS; `?token=` + `?apiKey=` used instead |

## Phase 2 — TODO

Do **not** invent routes in [services-mvp.md](../../docs/services-mvp.md) until implemented. Coordinate with auth / user-service / platform Phase 2.

### Security fixes / auth alignment

- [x] **Fix sprints security** — require JWT; stop open `permitAll`
- [x] **Public path set for Phase 2 auth** — permit verify/reset/resend; public register gated in auth
- [ ] **Document or enforce WS auth** — keep downstream-only `?token=` + `?apiKey=` (optional gateway later)

### Routing / tenancy / admin portal

- [x] **Route `document-service`** — `/api/v1/documents/**` → `:9083` (Wave 7 scaffold; **out of UAT** — stubs, JWT/ACL not on document-service yet)
- [x] **Route `user-service`** — `/api/v1/users/**` + `/api/v1/admin/users/**` → user-service:8084
- [x] **Org context at the edge** — forward `X-Organization-Id`; services validate membership
- [x] **Admin portal CORS** — env `CORS_ALLOWED_ORIGINS`
- [x] **Invite token peek** — `GET /invitations/token/**` permitAll for landing pages
- [ ] **Admin route protection** — master-admin claim at edge (services check `is_master_admin` today)

### Hardening

- [x] **Env-based upstream URIs** — `AUTH_SERVICE_URI`, `PLATFORM_SERVICE_URI`, etc.
- [x] **Lock down actuator** — health/info public; other actuator authenticated outside `dev`/`local`
- [ ] **Gateway Redis rate limit** — edge limits on login / verify (backup to auth); fail-closed outside `dev`
- [x] **Correlation ID filter** — generate/propagate `X-Request-Id`
- [ ] **Downstream timeouts + circuit breaker** — so one dead service does not hang the edge
- [x] **Unified 401/403 JSON** — match `:common` `ErrorResponse` for JWT denials (+ API-key filter)

### Stretch / later

- OpenAPI aggregation via gateway  
- Per-tenant / per-API-key quotas  
- Request size limits  
- mTLS between gateway and services  

## Security

| Layer | How |
|-------|-----|
| Client gate | Header `X-API-Key` = env `API_KEY` (repo-root `.env`); WS: `?apiKey=` |
| User auth | `Authorization: Bearer` (JWT from `JWT_SECRET`) |
| Public (MVP) | auth register/login/refresh/logout + healths + realtime WS |

## Run

Needs Docker infra if calling real services. Secrets from repo-root `.env` (`bootRun` loads them).

```powershell
cd D:\codes\CollabFlow_V2\java-services
# Typical: start auth (and others) then gateway
.\gradlew.bat :auth-service:bootRun
.\gradlew.bat :api-gateway:bootRun
```

## Try it

```powershell
$k = $env:API_KEY
curl http://localhost:8080/api/v1/auth/health -H "X-API-Key: $k"

# After login via :8080 — use accessToken
curl http://localhost:8080/api/v1/auth/me -H "X-API-Key: $k" -H "Authorization: Bearer <accessToken>"
```

Full happy path: [e2e-demo.md](../../docs/e2e-demo.md) · Postman under `postman/`.
