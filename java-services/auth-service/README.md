# auth-service

**Port:** 8081 · Java 21 / Boot 4.1 / Gradle  
**Owns:** `users`, `refresh_tokens`  
**MVP:** Complete · **Phase 2:** TODO checklist below

Shared: `:common` → envelopes, `ApiKeyFilter`, `DomainEvent`, `@CurrentUserId`, `@ApiId`  
(see `.cursor/rules/java-spring.mdc` + [services-mvp.md](../../docs/services-mvp.md) ApiId catalog).

## What it does

Identity and tokens for CollabFlow. Today (MVP): open register/login with email + password, JWT + refresh rotation, Redis rate limits, Kafka `user.created` after commit. Platform/task/collab trust the JWT `sub` (user id); this service does **not** own org/workspace RBAC.

**Phase 2 direction:** stop self-serve “friends” onboarding. A **master admin** portal creates orgs and users; only org members use the app; login picks an org; email verification is required.

## Features (MVP — shipped)

| Feature | What it does | ApiId |
|---------|----------------|-------|
| Health | Liveness | AUTH-001 |
| Register | Create user (bcrypt), issue access + refresh JWT, Kafka `user.created` after commit | AUTH-002 |
| Login | Email + password; Redis rate-limit by client IP | AUTH-003 |
| Refresh | Rotate refresh token, issue new access JWT | AUTH-004 |
| Logout | Revoke **one** refresh token | AUTH-005 |
| Me | Current user profile (`emailVerified` exposed; **not** gated on verify yet) | AUTH-006 |
| Redis rate limit | `INCR` + TTL on login/register; **fail-open** if Redis is down (dev-friendly) | — |
| OpenAPI | springdoc Swagger UI | — |

### MVP learning notes

| Piece | What / why |
|-------|------------|
| **Kafka `user.created`** | After DB commit only — never publish if the transaction rolled back. |
| **Redis rate limit** | Shared across auth instances; fail-open today so local work without Redis still logs in. |
| **Gateway JWT** | Prefer `:8080`; gateway verifies Bearer before proxying `/me`. |

## Phase 2 — TODO

Track enrichments here. Do **not** invent ApiIds in [services-mvp.md](../../docs/services-mvp.md) until implemented. Real SMTP is **not** owned by auth alone — use email stub / Kafka `email.send` / future email-service.

### Identity / email

- [ ] **Email verification** — token + verify endpoint; **require verified email before full login** (or after first login step); use `users.is_email_verified`
- [ ] **Resend verification** — rate-limited
- [ ] **Forgot / reset password** — for users who forgot the admin-issued password
- [ ] **Change password** — authenticated; current + new (admin-set password can be changed later)

### Profile / account

- [ ] **Update profile** — `PUT /api/v1/auth/me` (name, `avatar_url` at minimum)
- [ ] **Soft-deactivate account** — user or admin sets `is_active=false`
- [ ] **Publish `user.updated`** — after profile / password / verify / deactivate (Kafka after commit)

### Sessions / rate limits

- [ ] **List sessions** — active refresh tokens (id, created, expires; **never** return raw token)
- [ ] **Revoke all sessions** / logout-all — revoke every refresh token for the user (optional revoke-one by id)
- [ ] **Rate-limit refresh** — same Redis pattern as login
- [ ] **Fail-closed rate limit outside `dev`** — if Redis is down in `prod`/`staging`, reject; keep fail-open only for `dev`

### Admin-provisioned users + org-scoped login

- [ ] **Disable or gate public self-register** for normal app users (MVP `POST /register` becomes admin-only or removed from public UX)
- [ ] **Admin creates user** — email + temporary password (or invite email with set-password link); user must belong to ≥1 org
- [ ] **Org-scoped login** — after credentials (+ email verified):
  - Load memberships; **reject** if user has **0 orgs**
  - **1 org** → auto-select (or still show picker for consistency)
  - **N orgs** → user **chooses** which org to enter
  - Login response includes selected `organizationId` (+ `organizations[]` for picker)
  - JWT may stay user-scoped with org in response/header later; platform still enforces membership

### Split: auth-service vs user-service

Today one deployable owns both credentials and profile. Phase 2 goal: **split into two Java services**.

| Service | Owns (intended) | Examples |
|---------|-----------------|----------|
| **auth-service** | Credentials, sessions, tokens | login, refresh, logout, email verify, password reset/change, rate limits, JWT issue |
| **user-service** (new) | User profile / lifecycle | admin create user, `GET/PUT` profile, soft-deactivate, list users for admin portal; table `users` (or profile projection) |

- [x] **Extract `user-service`** — `java-services/user-service` (:8084); shared `users` table (auth Flyway); profile + admin create on user-service — [database.md](../../docs/database.md)
- [x] **Gateway routes** — `/api/v1/auth/**` → auth; `/api/v1/users/**` + `/admin/users/**` → user-service
- [x] **Admin create user** goes through **user-service** (full row + temp password hash; password *changes* stay on auth)
- [x] **Kafka** — `user.created` / `user.updated` from **user-service** (auth still may publish `user.created` on public register)
- [ ] **ApiId catalog** — new `USER-NNN` prefix in services-mvp when implemented

### Depends on platform + admin portal (not auth alone)

- [ ] **Master admin account** — bootstrap / seed for the admin portal (auth or platform; decide when implementing)
- [ ] **Admin portal (frontend)** — create **organization**, create **user**, attach user to org (membership + role)
- [ ] **Platform APIs for admin** — create org + create/link user as member (calls **user-service** + membership)
- [ ] **Only org members use the app** — no org ⇒ no product access (align gateway/platform checks)

**Later (not Phase 2 priority):** MFA, OAuth / social login.

## Target flow (Phase 2 design)

Hierarchy stays **Org → Workspace → Project → Task**. Access is **admin-provisioned**, not self-serve personal/friends signup.

```text
Master admin (admin portal)
  → create Organization
  → create User (email + temp password) and add to org
User
  → login (email + password)
  → email verify (if not yet verified)
  → choose organization (if more than one)
  → use app inside that org
  → can change password later
```

| Rule | Detail |
|------|--------|
| Who creates orgs/users | Master admin via **admin portal** (frontend) |
| Who may use the app | Users that **have at least one org membership** |
| Credentials | Email + password set by admin (user can change password later) |
| Email verify | Required in the login/onboarding path |
| Org on login | User **selects** which org to work in |

**Ordered waves:** [docs/phase-2.md](../../docs/phase-2.md). Deferred catalog: [future-plan.md](../../docs/future-plan.md).

### See also / Depends on

| Topic | Owner |
|-------|--------|
| Admin org create + attach member | [platform-service](../platform-service/README.md) |
| `X-Organization-Id` forward + public verify/reset paths | [api-gateway](../api-gateway/README.md) |
| `PageResponse` / `@CurrentOrganizationId` / request-id | [common](../common/README.md) |
| Consume `email.send` (stub → SMTP) | [collab-service](../../go-services/collab-service/README.md) |
| Admin create user | **Moved** to [user-service](../user-service/README.md) (`USER-020`) |
| Email | **Auth produces** `email.send`; collab consumes |

## Security

| Layer | How |
|-------|-----|
| Client gate | Header `X-API-Key` = env `API_KEY` (repo-root `.env`) |
| User auth | `Authorization: Bearer` (JWT from `JWT_SECRET`) |

Swagger: http://localhost:8081/swagger-ui.html  

## Logs

Console + dated files: repo `logs/auth/auth-service.yyyy-MM-dd.log` (`logback-spring.xml`).

## Layout

`controller` / `service` / `dto` / `repository` / `model` / `config` / `exception` / `event`

## Run

Needs Docker: Postgres, Redis, Kafka (`infrastructure/`). Secrets from repo-root `.env` (`bootRun` loads them).

```powershell
cd D:\codes\CollabFlow_V2\java-services
.\gradlew.bat :auth-service:bootRun
```

## Try it (direct or via gateway `:8080`)

```powershell
# From repo root .env (or set in the shell)
$k = $env:API_KEY
$base = "http://localhost:8080"   # or http://localhost:8081

curl "$base/api/v1/auth/health" -H "X-API-Key: $k"

curl -X POST "$base/api/v1/auth/register" -H "Content-Type: application/json" -H "X-API-Key: $k" `
  -d "{\"email\":\"kaung@example.com\",\"password\":\"password123\",\"name\":\"Kaung\"}"

curl -X POST "$base/api/v1/auth/login" -H "Content-Type: application/json" -H "X-API-Key: $k" `
  -d "{\"email\":\"kaung@example.com\",\"password\":\"password123\"}"

curl "$base/api/v1/auth/me" -H "X-API-Key: $k" -H "Authorization: Bearer <accessToken>"
```

Watch Kafka (optional):

```powershell
docker exec -it collabflow-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic user.created --from-beginning
```

Postman: `postman/CollabFlow-Auth.postman_collection.json` (set `baseUrl` to `http://localhost:8080` for the gateway).
