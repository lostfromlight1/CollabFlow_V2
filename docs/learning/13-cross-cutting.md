# 13 — Cross-cutting concerns

**Goal:** See the rules that every service shares: security, JSON shape, errors, logs, profiles, and the places docs and code disagree.

## Four different “who are you” checks

They stack. Passing one does not pass the others.

| Check | What it proves | Where |
|-------|----------------|--------|
| `X-API-Key` | The caller knows the client key (`API_KEY`) | `ApiKeyGatewayFilter`. Also `ApiKeyFilter` in `:common` on servlet services. |
| JWT | The caller is a user. `sub` is `users.id`. Claim `email` is present. | Gateway resource server. Each service’s `JwtAuthenticationFilter` or Go `JWTAuth`. Signed HS256 with `JWT_SECRET`. |
| Org role | The user has a permission at org, workspace, or project scope | `PermissionService` plus member tables. Not inside the JWT. |
| Master admin | `users.is_master_admin` is true, user active, not deleted | `AuthServiceImpl.requireActiveMaster`, `UserServiceImpl.requireMasterAdmin`, platform `UserRefRepository.existsByIdAndActiveTrueAndMasterAdminTrue` |

There is no `@PreAuthorize` in the Java tree. Do not add a role claim to the token and expect the gateway to enforce it. The gateway only checks that the signature is valid, except for the impersonation filter.

`X-Organization-Id` is a selected tenant, not a proof of membership by itself. task-service calls `OrganizationContextService.requireMembership`. realtime optionally checks it on subscribe. platform often uses the id in the URL instead.

## Impersonation

A second mode of the same JWT. `actor` is the admin, `sub` is the target, 15-minute expiry, no refresh token (`JwtService.createImpersonationToken`). `ImpersonationRules.allowed` blocks writes, admin GETs, and WebSocket upgrades that present the bearer header. The only mutation allowed is `POST /api/v1/admin/sessions/impersonate/end`.

## Envelopes and errors

| Shape | Type | Fields |
|-------|------|--------|
| Success | `ApiResponse` | `success`, `message`, `data`, `timestamp` |
| Page inside `data` | `PageResponse` | `content`, `page` (0-based), `size`, `totalElements`, `totalPages` |
| Error | `ErrorResponse` | `success` false, `message`, `errorCode`, `path`, `timestamp`, optional `errors[]` |

`GlobalExceptionHandler` in `:common` maps `BusinessException`, `ForbiddenException`, `ConflictException`, `UnauthorizedException`, and `ResourceNotFoundException`. Controllers return `ApiResponse`, not raw entities.

Go handlers often use `gin.H{"success","message","data"}` and sometimes omit `timestamp` and `errorCode`. The frontend `ApiError` uses HTTP status plus the message (`frontend/lib/api/client.ts`).

Gateway auth failures are JSON from `GatewayJsonAuthEntryPoint` and `GatewayJsonAccessDeniedHandler`, not an HTML page. Impersonation denial is a small JSON body with `errorCode` `FORBIDDEN`.

## Logging and `@ApiId`

Java controllers put `@ApiId("TASK-001")` on the handler. `ApiIdMdcInterceptor` copies `apiId` and `userId` onto the SLF4J MDC. Log files for `bootRun` go under `logs/<service>/` at the repo root (`LOG_HOME` overrides). Gateway and Go processes started by `scripts/dev-backend.ps1` log under `logs/backend/`.

ApiIds are for support. They are not `ErrorCode` values. The catalog is [docs/services-mvp.md](../services-mvp.md). Go routes have documentation ids in that file and no annotation.

Do not log passwords, refresh tokens, or `API_KEY`.

## Profiles and configuration

| Topic | Behavior |
|-------|----------|
| Active profile | `SPRING_PROFILES_ACTIVE`, default `dev` on the services that set `${SPRING_PROFILES_ACTIVE:dev}` |
| Secrets | `API_KEY` and `JWT_SECRET` come from the environment. `bootRun` loads repo-root `.env` and fails if either is missing (`backend/java-services/build.gradle`). YAML must not contain the secret literal. |
| Go | Does not load `.env` by itself. `Load()` may substitute a fallback if a variable is empty. Set the variables. Do not copy fallbacks into docs. |
| Actuator | `/actuator/health` and `/info` are public. Other actuator paths are public only for profile `dev`, `local`, or empty (`GatewaySecurityConfig`). |
| Rate limit | Redis down: allow on `dev` / `local` / no profile; reject otherwise (`AuthRateLimiter`). |
| Self-serve orgs | `PLATFORM_SELF_SERVE_ORG_CREATE` default false. |
| Public register | `AUTH_PUBLIC_REGISTER_ENABLED`, default false in `AuthPhase2Properties`. |
| Mail | `EMAIL_SERVICE_ENABLED=true` means email-service sends SMTP and collab does not consume `email.send`. |
| CORS | `CORS_ALLOWED_ORIGINS`. Empty origins break browser preflight ([docs/fe-contract.md](../fe-contract.md)). |

## Service boundaries to keep

| Rule | Why |
|------|-----|
| Browser calls only port 8080 | [docs/fe-contract.md](../fe-contract.md) |
| One shared database `collabflow` | MVP choice in [docs/tech-stack.md](../tech-stack.md). Not a database per service. |
| Flyway only in auth, platform, and task | Separate history tables so versions do not collide |
| Go AutoMigrate only for tables that service owns | collab tables and `documents` |
| Kafka after a successful write | `@TransactionalEventListener(AFTER_COMMIT)` on Java publishers |
| collab does not write platform member tables | It calls platform HTTP on invite accept |
| realtime does not write tasks | It reads them for ACL and broadcasts |
| email has no gateway route | Health is direct on 9084 |
| Do not invent tables, paths, or topics | Update `docs/database.md` and `docs/services-mvp.md` in the same change |
| Archive | `docs/archive/legacy-16-service-plan.md` is not the layout |

Java owns identity, tenancy, and tasks. Go owns notifications, live fan-out, files, and SMTP. user-service is the profile and admin-directory split on the shared `users` table. It is Phase 2 and it is in the build.

## Doc versus code (keep this list)

| Topic | Document | Code |
|-------|----------|------|
| `documents` table | Design-only in [docs/database.md](../database.md) ownership | `document-service` AutoMigrate |
| Document auth | [docs/fe-contract.md](../fe-contract.md) says no JWT/ACL, stubs, no MinIO | Handler checks JWT and `project_members`. Disk storage, not MinIO. `go-services/README.md` still says MinIO. |
| Workflow id columns | Catalog shows foreign keys | Platform V3/V4: nullable UUID, no FK |
| Owner delete | Catalog is softer | V3/V4 `owner_id ON DELETE CASCADE` |
| Mention syntax | Catalog says `@username` | `CommentService` matches `@` plus a UUID |
| Comment entity types | Catalog includes `DOCUMENT` | `Create` allows `TASK` and `PROJECT` only |
| Profile URL | Both AUTH-007 and USER-002 exist | Contract says new UI uses `/api/v1/users/me` |
| AUTH-020 | Deprecated | Create user is USER-020 |
| Deactivate | user-service README mentions it | Route is AUTH-021 on auth-service |
| Platform org header | README TODO | Enforced broadly on tasks; only partly on platform |
| Kafka topics with no consumer | `user.created` and `user.updated` are produced and left without a listener on purpose | Account mail is `email.send`. A profile change has no tenant. Platform topics, `task.deleted`, and `task.status_changed` now have consumers. |
| Deployable count | “6 services” in the MVP table | Gradle also includes user-service; Go also includes document and email |
| Rate limit | Some README text says fail-open | Fail-open only for `dev` and `local` |

When you fix one of these, change the contract and this chapter together. Until then, trust the code column.

## Glossary

| Term | Meaning |
|------|---------|
| ErrorCode | Stable string on `ErrorResponse`, separate from `@ApiId`. |
| Fail-open | If Redis is down, the request is allowed. |
| Fail-closed | If Redis is down, the request is rejected. |
| Contract | `docs/services-mvp.md`, `docs/database.md`, `docs/fe-contract.md`. This folder explains them. |

## Unverified

`ErrorCode` enum constant names were not listed one by one. The handler class and the response fields were read. Open `common/.../exception/ErrorCode.java` for the full set.
