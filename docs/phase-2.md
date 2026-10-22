# CollabFlow — Phase 2 (ordered implementation)

**Status:** Waves 0–8 implemented (scaffolds for W7–W8; tick service READMEs as you harden)  
**Product north star:** Admin-provisioned tenancy (master admin portal → org + users → login → email verify → org picker → `X-Organization-Id`).  
**Per-service checklists:** each service README → Phase 2 TODO (tick boxes as you ship).  
**History / deferred services:** [future-plan.md](future-plan.md)

## Locked decisions

| Topic | Decision |
|-------|----------|
| Start order | **Tenancy / admin first**, then documents, then standalone email-service |
| Login vs verify | Credentials → **email verify required** → org picker / app tokens |
| Profile API | Until split: auth `/me`; after split: **`/api/v1/users/me`** |
| Admin create user | Prototype on **auth**; extract **user-service** after portal APIs work |
| Email | Kafka **`email.send`**: auth/user/collab produce; **email-service** sends real SMTP; `EMAIL_SERVICE_ENABLED=true` disables collab stub |
| Org header | Gateway **forwards** `X-Organization-Id`; **services validate** membership |
| Sprints JWT | **Gateway** fixes permitAll; **task** owns sprints API |
| WS JWT | Keep **downstream** check + document; gateway optional later |
| Master admin | **Auth** seeds; **platform** admin org/member APIs |
| Invite accept | **Collab** → **platform** membership API |

## Waves

### Wave 0 — Foundations
**Owners:** common, api-gateway  
- common: `PageResponse`, `@CurrentOrganizationId`, request-id MDC, richer exceptions  
- gateway: `X-Request-Id`, env upstream URIs, sprints JWT (or drop route), actuator lockdown, unified 401/403  

### Wave 1 — Admin backend + identity
**Owners:** auth, platform, collab (email stub consumer)  
- Auth: verify/resend/reset/change-password; `email.send`; sessions; fail-closed rate limit; master admin seed; gate public register  
- Platform: admin-only org create; attach user; default workspace; ownership transfer; list orgs + role; org Kafka parity  
**Done when:** Postman can admin-create org+user+membership; user can verify email  

### Wave 2 — Org-scoped session + edge
**Owners:** auth, gateway, platform, task, collab, common  
- Org picker login; CORS; forward `X-Organization-Id`; services honor org header  
**Done when:** product APIs fail closed without valid org membership  

### Wave 3 — user-service split
**Owners:** user-service, auth, gateway  
- Extract profiles/admin user CRUD; `/api/v1/users/**`; `USER-NNN` ApiIds; Kafka cutover  

### Wave 4 — Collab invite + email delivery
**Owners:** collab, platform  
- Accept invite → platform membership; invitation email; org-scoped lists; unread count  

### Wave 5 — Task product
**Owners:** task  
- Sprints; sub-tasks; filters; pagination; workflow admin as needed  

### Wave 6 — Realtime ACL
**Owners:** realtime  
- Subscribe ACL; org context; lock `/broadcast`; richer events / user rooms  

### Wave 7 — document-service
After W1–W2 so permissions attach to org/project. See future-plan.  
**Status:** Scaffold shipped — `go-services/document-service` **:9083**; gateway `/api/v1/documents/**`; health + upload/list stubs; `MINIO_*` placeholders (no MinIO dial yet).

### Wave 8 — Standalone email-service
Extract when SMTP/templates/retries justify a deployable.  
**Status:** Real SMTP shipped — `email-service` dials `SMTP_*` (local MailHog `:1025` / UI `:8025`). Set `EMAIL_SERVICE_ENABLED=true` on collab to skip the stub consumer.

## Frontend (parallel)

- Admin portal: master admin → create org → create user → attach  
- App: login → verify → org picker → hierarchy  

Ship API waves first.
