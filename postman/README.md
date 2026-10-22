# Postman — CollabFlow

## Import

1. Open Postman → **Import**
2. Add `CollabFlow-Auth.postman_collection.json`
3. Add `CollabFlow-Platform.postman_collection.json`
4. Add `CollabFlow-Local.postman_environment.json`
5. Select environment **CollabFlow Local**

Re-import (Replace) after pull so Phase 2 folders appear.

Align `apiKey` with repo-root `.env` `API_KEY`.

## Collections

| File | What it covers |
|------|----------------|
| **Auth** | Health · legacy happy path (register — needs `AUTH_PUBLIC_REGISTER_ENABLED`) · negatives · second user · **Phase 2 admin tenancy** |
| **Platform** | Roles · Org · Workspaces · Projects · Tasks · Notifications · Invites · Activity · Realtime |
| **Local env** | Tokens, org/workspace/project/task ids, `organizationId` for `X-Organization-Id` |

## Recommended Phase 2 run order

Full narrative: [docs/e2e-demo.md](../docs/e2e-demo.md).

1. Start stack per [docs/running.md](../docs/running.md) (infra + MailHog + gateway/auth/platform/user/email/…)
2. **Auth → 4. Phase 2 admin tenancy**
   - Login master admin → Me
   - Create user (`/admin/users`) → check MailHog → Verify email
   - Create admin org → Attach member
   - Login as app user → Select organization (if needed)
3. **Platform** folders with Bearer + set `organizationId` in env (collection/requests that send `X-Organization-Id`)
4. Tasks / notifications / realtime as before

### Legacy register path

**Auth → 1. Happy path → Register** only works if `AUTH_PUBLIC_REGISTER_ENABLED=true`. Prefer admin tenancy for Phase 2 demos.

## Auth response fields

Login/register return a **flat** `data` object:

- `userId`, `email`, `name`, `accessToken`, `refreshToken`
- `selectedOrganizationId`, `organizations[]`, `requiresOrganizationSelection`

## Headers

| Header | When |
|--------|------|
| `X-API-Key` | Almost every `/api/**` call |
| `Authorization: Bearer {{accessToken}}` | Protected routes |
| `X-Organization-Id` | Product APIs after org picker (tasks, sprints, …) |

Gateway: `http://localhost:8080`  
Swagger: auth `:8081` · platform `:8082` · task `:8083` · user `:8084`  
MailHog: `http://localhost:8025`
