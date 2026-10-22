# Postman — CollabFlow

## Import

1. Open Postman → **Import**
2. Add `CollabFlow-Auth.postman_collection.json`
3. Add `CollabFlow-Platform.postman_collection.json`
4. Add `CollabFlow-Local.postman_environment.json`
5. Select environment **CollabFlow Local**

Re-import (or Replace) collections after pulling so new folders/tests appear.

## Collections

| File | Folders / cases |
|------|-----------------|
| **Auth** | Health (± API key) · Happy path (register→login→me→refresh→logout→refresh-fails) · Validation (dup email, short password, bad email, wrong login, Me without token) · Second user (for Platform members) |
| **Platform** | Roles · Org CRUD · Members (add/dup/update/remove) · Soft-delete · Negatives (401, bad slug, blank name, dup slug) |
| **Local env** | Shared `accessToken`, `userId`, `otherUserId`, `organizationId`, … |

Most requests assert status + `ApiResponse` / `errorCode` via Postman **Tests**.

## Suggested run order

1. **Auth → 1. Happy path → Register (unique email)** — saves tokens + `userId` into the environment  
2. **Auth → 3. Second user → Register other user** — saves `otherUserId`  
3. **Platform → 1. Org happy path** (collection pre-request copies env token)  
4. **Platform → 2. Members** (needs `otherUserId`)  
5. Run **Negative** folders anytime  

Soft-delete (folder 3) deactivates the org — run member cases before it, or create a fresh org first.

## Headers

| Header | When |
|--------|------|
| `X-API-Key` | Almost every `/api/**` call |
| `Authorization: Bearer {{accessToken}}` | Protected routes (`/me`, orgs, roles) |

Gateway: `http://localhost:8080` · Swagger: `:8081` auth · `:8082` platform
