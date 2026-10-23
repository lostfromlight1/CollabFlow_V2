# Frontend contract (UAT)

Single source of truth for the Next.js / admin SPA against the **`uat`** backend freeze.  
ApiIds and full route tables: [services-mvp.md](services-mvp.md). Run infra: [running.md](running.md). Demo narrative: [e2e-demo.md](e2e-demo.md). Phase 2 status: [phase-2.md](phase-2.md).

**Not FE SoT:** [progress-tracker.md](progress-tracker.md) (MVP history only), [microservice.md](microservice.md), `docs/archive/**`.

---

## Ports (hit the gateway)

| Role | Port | Notes |
|------|------|--------|
| **API + WS edge** | **8080** | All HTTP via api-gateway |
| Auth | 8081 | Direct only for Swagger |
| Platform | 8082 | |
| Task | 8083 | Includes **sprints** |
| User | 8084 | Profile + admin users |
| Collab | 9081 | Notifications, invites, comments, activity |
| Realtime | 9082 | Prefer WS through gateway (below) |
| Document | 9083 | Upload, list, download (project membership) |
| Email | 9084 | Kafka consumer → SMTP (no public HTTP API) |
| MailHog UI | 8025 | Dev inbox |

SPA app: repo [`frontend/`](../frontend/) (`npm run dev` → `:3000`).  
API base URL for the SPA: `http://localhost:8080` (gateway only).  
Public landing: **`/`** when logged out (session-aware CTA when logged in); see [frontend/README.md](../frontend/README.md).  
Product UI uses **workspaces / projects** (`X-Organization-Id` required). Member lists include `name` and `email`.

---

## Required headers

| Header | When |
|--------|------|
| `X-API-Key` | Every `/api/**` request (value from `.env` `API_KEY`) |
| `Authorization: Bearer <accessToken>` | All authenticated routes |
| `X-Organization-Id: <uuid>` | After org picker — platform / task / collab product routes |

CORS: set `CORS_ALLOWED_ORIGINS=http://localhost:3000` in `.env` (see `.env.example`). Empty origins → browser preflight fails.

---

## Auth → org flow (product story)

Admin-provisioned tenancy (not “register then documents”):

1. Master admin logs in → opens **Users** and **Organizations** (paged directories). Create user, create organization, attach member, and transfer ownership are dialogs, not page sections.
2. User detail can suspend, restore, reset a password, or open a read-only impersonation session. Org detail shows owner, members, and workspace counts.
3. User **login** → if email unverified, verify via link / AUTH-008 (MailHog). A deleted account is rejected with a distinct message from a suspended account.
4. **Org picker** — `GET` orgs / `POST select-organization` (AUTH-016/017) when N > 1.
5. Store `accessToken` + selected `organizationId`; send `X-Organization-Id` on subsequent calls.
6. Then workspace → project → task / collab / realtime.

Admin routes (master admin only, except the two product reads): directories USER-021/022 and ADMIN-ORG-004/005/009/010, lifecycle AUTH-021–025 and USER-023/024 and ADMIN-ORG-006/007, CSV USER-025 and ADMIN-ORG-008, dashboard ADMIN-STATS-001, audit ADMIN-AUDIT-001, announcements ADMIN-ANN-001–004 plus `GET /api/v1/announcements/active`, feature flags ADMIN-FLAG-001–003 plus `GET /api/v1/feature-flags` (a missing key means enabled), impersonation AUTH-026/027 (15-minute read-only token, no refresh).

Public (no JWT): register (if enabled), login, refresh, logout, verify/resend, forgot/reset password, healths, **`GET /api/v1/invitations/token/{token}`** (invite peek). Accept invite still needs JWT.

---

## Profile API (locked)

| Use | Path |
|-----|------|
| **FE profile** | `GET/PUT /api/v1/users/me` (USER-001/002) |
| Legacy | `GET/PUT /api/v1/auth/me` — do **not** prefer for new UI |

---

## WebSocket

```
ws://localhost:8080/api/v1/realtime/ws?token=<accessToken>&apiKey=<API_KEY>
```

(Direct `:9082` also works with `?token=`.) After connect, subscribe e.g. `{"action":"subscribe","room":"project:<projectId>"}`.

Gateway does **not** validate JWT on the WS upgrade; realtime-service does.

---

## Email (UAT default)

Match `.env.example`:

- `EMAIL_SERVICE_ENABLED=true`
- Run **email-service** + **MailHog** (Compose)
- Collab skips its in-process email stub when the flag is true

Verify mail at http://localhost:8025.

---

## Do not use on UAT

| Surface | Why |
|---------|-----|
| `/api/v1/documents/**` upload/list | Stubs only — no MinIO, no JWT/ACL on the service |
| Auth `/me` for new screens | Prefer `/users/me` |
| Assuming only 6 services | Need **user** + **email** (+ MailHog) for admin/verify |

---

## Ops caveats

- **Kafka `LastOffset`:** start collab / realtime / email **before** producing events (or re-fire assign / email). Otherwise consumers miss messages already on the topic.
- Cherry-pick bugfixes onto `uat`; new Phase 2 polish stays on `development`.

---

## Smoke checklist (before FE)

1. Compose: Postgres, Redis, Kafka, MailHog (`backend/infrastructure`)  
2. Gateway + auth + platform + user + task + collab + realtime + **email**  
3. `.env`: `API_KEY`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `EMAIL_SERVICE_ENABLED=true`, `MASTER_ADMIN_*`  
4. Postman **or** `frontend/` (`npm run dev`): admin login → create user → create org → attach → member login  
5. WS subscribe to a project room  
6. Invite peek: `GET /api/v1/invitations/token/...` **without** Bearer (API key only)
