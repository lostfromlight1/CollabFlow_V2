# auth-service

**Port:** 8081 · Java 21 / Boot 4.1 / Gradle  
**Owns:** credentials, sessions, tokens (profiles/admin user CRUD → **user-service**)  
**MVP:** Complete · **Phase 2 Waves 1–3:** largely shipped (see checklist)

Shared: `:common` → envelopes, `ApiKeyFilter`, `DomainEvent`, `@CurrentUserId`, `@ApiId`  
(see `.cursor/rules/java-spring.mdc` + [services-mvp.md](../../../docs/services-mvp.md) ApiId catalog).

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
| Me | Current user profile | AUTH-006 |
| Update profile | `PUT /me` (legacy for FE — prefer **`/api/v1/users/me`**) | AUTH-007 |
| Verify / resend email | Public; required before full login | AUTH-008 / AUTH-009 |
| Forgot / reset / change password | Public reset + authenticated change | AUTH-010–012 |
| Sessions | List + revoke-all | AUTH-013 / AUTH-014 |
| Soft-deactivate | Current account | AUTH-015 |
| Org picker | `select-organization` + list orgs | AUTH-016 / AUTH-017 |
| Redis rate limit | Login/register (+ fail-closed outside `dev` where configured) | — |
| OpenAPI | springdoc Swagger UI | — |

### MVP learning notes

| Piece | What / why |
|-------|------------|
| **Kafka `user.created`** | After DB commit only — never publish if the transaction rolled back. |
| **Redis rate limit** | Shared across auth instances; fail-open today so local work without Redis still logs in. |
| **Gateway JWT** | Prefer `:8080`; gateway verifies Bearer before proxying `/me`. |

## Phase 2 — status (shipped vs leftover)

Ordered waves: [docs/phase-2.md](../../../docs/phase-2.md). FE contract: [docs/fe-contract.md](../../../docs/fe-contract.md).

### Shipped on auth

- [x] Email verification + resend (AUTH-008/009); login requires verified email
- [x] Forgot / reset / change password (AUTH-010–012)
- [x] Sessions list + revoke-all (AUTH-013/014)
- [x] Soft-deactivate (AUTH-015)
- [x] Update profile on auth `PUT /me` (AUTH-007) — **FE should use user-service `/users/me`**
- [x] Org picker: list orgs + select-organization (AUTH-016/017)
- [x] Gate public self-register (config)
- [x] Master admin seed (`MASTER_ADMIN_*` env)
- [x] Produce Kafka `email.send` (email-service / MailHog delivers)
- [x] **user-service** split (:8084) + gateway routes + `USER-NNN` catalog

### Leftover TODO (not UAT blockers)

- [ ] Rate-limit **refresh** (same Redis pattern as login)
- [ ] Harden fail-closed rate limit messaging for all non-dev profiles
- [ ] MFA / OAuth (Phase 3+)

### Frontend / admin portal (out of this service)

- [ ] Admin portal UI + app UI — see [fe-contract.md](../../../docs/fe-contract.md)

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

**Ordered waves:** [docs/phase-2.md](../../../docs/phase-2.md). Deferred catalog: [future-plan.md](../../../docs/future-plan.md).

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
cd D:\codes\CollabFlow_V2\backend\java-services
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
