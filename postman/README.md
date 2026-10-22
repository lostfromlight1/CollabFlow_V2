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
| **Platform** | Roles · Org CRUD · Members · Soft-delete · Negatives · Workspaces · **Projects** (CRUD, members, archive) |
| **Local env** | Shared `accessToken`, `userId`, `otherUserId`, `organizationId`, `workspaceId`, `projectId`, … |

Most requests assert status + `ApiResponse` / `errorCode` via Postman **Tests**.

## Suggested run order

1. **Auth → 1. Happy path → Register (unique email)** — saves tokens + `userId` into the environment  
2. **Auth → 3. Second user → Register other user** — saves `otherUserId`  
3. **Platform → 1. Org happy path** (collection pre-request copies env token)  
4. **Platform → 2. Members** (needs `otherUserId` as org member)  
5. **Platform → 5. Workspaces** (needs `organizationId`; add member requires org membership) — skip Soft-delete workspace before Projects  
6. **Platform → 6. Projects** (needs active `workspaceId`; add member requires workspace membership)  
7. Run **Negative** folders anytime  

Soft-delete org (folder 3) or workspace (folder 5) deactivates parents — run project cases on an active workspace, or create fresh org/workspace first.

## Headers

| Header | When |
|--------|------|
| `X-API-Key` | Almost every `/api/**` call |
| `Authorization: Bearer {{accessToken}}` | Protected routes (`/me`, orgs, workspaces, projects, roles) |

Gateway: `http://localhost:8080` · Swagger: `:8081` auth · `:8082` platform
