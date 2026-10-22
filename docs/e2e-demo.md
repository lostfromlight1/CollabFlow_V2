# CollabFlow — E2E / testing guide

Portfolio smoke path for slim MVP + **Phase 2 admin tenancy**.

**Start stack first:** [running.md](running.md) (`.env` → Compose + MailHog → Java/Go).  
**Postman:** [postman/README.md](../postman/README.md).

---

## Phase 2 happy path (admin-provisioned)

Use Postman **CollabFlow Auth → 4. Phase 2 admin tenancy** (or curl below).

| Step | Action | Expect |
|------|--------|--------|
| 1 | `POST /api/v1/auth/login` as master admin | tokens; `GET /auth/me` → `masterAdmin=true` |
| 2 | `POST /api/v1/admin/users` — create app user | `201`; MailHog shows **welcome** (SMTP) |
| 3 | `POST /api/v1/admin/organizations` — `ownerUserId` = new user | org + default workspace |
| 4 | Login as app user | `selectedOrganizationId` / `organizations[]` |
| 5 | Product APIs with `Authorization` + `X-Organization-Id` + `X-API-Key` | 200 |
| 6 | Create task / assign → collab + realtime | Kafka side effects |

Optional: public-register verify uses `POST /auth/verify-email` + MailHog token (`AUTH_PUBLIC_REGISTER_ENABLED=true`).

Master admin defaults (from `.env.example`): `admin@collabflow.local` / `ChangeMeAdmin123!`.  
MailHog UI: http://localhost:8025

### Curl sketch (gateway)

```powershell
$k = $env:API_KEY   # or load from .env — see running.md
$base = "http://localhost:8080"

# 1) Master admin login
curl -s -X POST "$base/api/v1/auth/login" -H "Content-Type: application/json" -H "X-API-Key: $k" `
  -d "{\"email\":\"admin@collabflow.local\",\"password\":\"ChangeMeAdmin123!\"}"
# → save accessToken as $admin

# 2) Admin create user
curl -s -X POST "$base/api/v1/admin/users" -H "Content-Type: application/json" `
  -H "X-API-Key: $k" -H "Authorization: Bearer $admin" `
  -d "{\"email\":\"dev@example.com\",\"temporaryPassword\":\"password123\",\"name\":\"Dev\"}"
# → MailHog shows welcome; admin-created users are emailVerified

# 3) Admin create org (ownerUserId = created user id)
# 4) Attach member if needed
# 5) User login → call tasks with X-Organization-Id
```

---

## Auth response shape (Phase 2)

```json
{
  "success": true,
  "data": {
    "userId": "...",
    "email": "...",
    "name": "...",
    "accessToken": "...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "selectedOrganizationId": "...",
    "organizations": [{ "id": "...", "name": "...", "slug": "...", "roleId": "..." }],
    "requiresOrganizationSelection": false
  }
}
```

Postman scripts must use `j.data.userId` (not `j.data.user.id`).

---

## MVP self-serve (opt-in only)

Public register / self-serve org create stay **off** unless:

- `AUTH_PUBLIC_REGISTER_ENABLED=true`
- `PLATFORM_SELF_SERVE_ORG_CREATE=true`

---

## Cross-service checks

- Missing `X-Organization-Id` on tasks/sprints → 400/403  
- Realtime subscribe as non-member → FORBIDDEN; `/broadcast` only in dev  
- Email: auth/invite/welcome → Kafka `email.send` → email-service → MailHog  

---

## Done when

You can narrate: *master admin → create user (SMTP) → org + member → org login → hierarchy → Kafka inbox + WS*.
