# platform-service

**Port:** 8082 · Java 21 / Boot 4.1 / Gradle  
**Owns:** `organizations`, `organization_members`, `workspaces`, `workspace_members`, `projects`, `project_members`, `roles`, `permissions`, `role_permissions`  
**MVP:** Complete · **Phase 2:** TODO checklist below

Hierarchy: **Organization → Workspace → Project** (tasks live in `task-service`).  
ApiIds: [services-mvp.md](../../../docs/services-mvp.md).  
**Ordered waves:** [docs/phase-2.md](../../../docs/phase-2.md).

### See also / Depends on

| Topic | Owner |
|-------|--------|
| Master admin seed + admin create user (prototype) | [auth-service](../auth-service/README.md) |
| Org header forward | [api-gateway](../api-gateway/README.md); validate membership **here** |
| Invite accept → call membership API | [collab-service](../../go-services/collab-service/README.md) |
| Workspace **invite code** vs email **invitation** | **Invite code** = workspace join code (`POST /workspaces/join`, this service; caller must already be org member). **Email invitation** = collab `invitations` + `email.send`; on accept collab calls **ORG-007** here (self-join Member role allowed for Wave 4). |

## What it does

Multi-tenant **tenancy + RBAC** for CollabFlow. Creates and manages orgs, workspaces, and projects; assigns members to seeded roles; enforces permission checks via `PermissionService`. Publishes Kafka events after successful DB commits so collab/realtime (and future consumers) can react.

**MVP:** any authenticated user can create an org and become Owner.  
**Phase 2 direction:** **master admin portal** creates orgs and attaches users; end users must already have org membership; login picks an org (`X-Organization-Id` at the gateway).

## Features (MVP — shipped)

| Area | What it does | ApiIds |
|------|----------------|--------|
| Health | Liveness | PLAT-001 |
| Organizations | Create (caller = Owner, gated), list mine (+ role), get/update/soft-delete; members add/role/remove; ownership transfer; validates user exists (`UserRef`) | ORG-001…010 |
| Admin orgs | Master-admin create (specify owner), attach member, transfer ownership — reads `users.is_master_admin` (auth-owned column) | ADMIN-ORG-001…003 |
| Workspaces | CRUD under org; members; **join via invite code** (must already be org member); default **General** workspace on org create | WS-001…010 |
| Projects | CRUD under workspace; DELETE archives (`ARCHIVED`); optional `parentProjectId`; members must be workspace members | PRJ-001…009 |
| Roles | List seeded system roles | ROLE-001 |
| RBAC | Seeded `roles` / `permissions` / `role_permissions`; checks on mutating APIs | — |
| Kafka | Org: created/updated/deleted + member_added/member_removed. Workspace/project: create/update/delete + member add/remove | — |
| OpenAPI / logs | springdoc; `logs/platform/` | — |

### MVP learning notes

| Piece | What / why |
|-------|------------|
| **Merged platform** | One service instead of org + workspace + project microservices — clearer for a junior portfolio |
| **PermissionService** | Role → permissions in DB; no org roles inside the JWT |
| **Flyway** | Table `flyway_platform_history`; baseline 0 because auth tables already exist in shared DB |

## Seeded roles

| Role | UUID |
|------|------|
| Organization Owner | `a0000000-0000-4000-8000-000000000001` |
| Organization Admin | `a0000000-0000-4000-8000-000000000002` |
| Organization Member | `a0000000-0000-4000-8000-000000000003` |
| Workspace Admin | `a0000000-0000-4000-8000-000000000004` |
| Workspace Member | `a0000000-0000-4000-8000-000000000005` |
| Workspace Viewer | `a0000000-0000-4000-8000-000000000006` |
| Project Admin | `a0000000-0000-4000-8000-000000000007` |
| Project Member | `a0000000-0000-4000-8000-000000000008` |
| Project Viewer | `a0000000-0000-4000-8000-000000000009` |

Cannot assign **Organization Owner** via add-member — use **ORG-010** / **ADMIN-ORG-003** transfer endpoints.

**Master admin:** platform reads `users.is_master_admin` via `UserRef` (column owned by **auth** Flyway). Do not add that column in platform migrations.

**Config:** `collabflow.platform.self-serve-org-create` (env `PLATFORM_SELF_SERVE_ORG_CREATE`, default `false`).

## Phase 2 — TODO

Coordinate with auth (admin users), gateway (org header / CORS), and future **user-service**. Do not invent ApiIds in services-mvp until implemented.

**Default rule:** **org create is admin-portal (master-admin) driven**; end users do not self-serve into empty tenancy.

### Admin portal / tenancy

- [x] **Admin create organization** — `ADMIN-ORG-001`; assign `ownerUserId`
- [x] **Admin attach user to org** — `ADMIN-ORG-002` (also ORG-007 for Wave 4 invite accept)
- [x] **List my organizations (login picker)** — ORG-002 returns `roleId` / `roleName`
- [ ] **Honor `X-Organization-Id`** — validate caller is a member of that org before mutating (defense in depth with gateway)
- [x] **Restrict public self-serve org create** — gate ORG-001 when self-serve disabled
- [x] **Transfer organization ownership** — ORG-010 (owner) + ADMIN-ORG-003 (master admin)

### Hierarchy / membership

- [x] **Default workspace on org create** — seeds **General** workspace
- [x] **Invite-link vs invite-code** — workspace invite **code** (this service) vs collab **email invitation** → ORG-007 (+ WS/PRJ members) on accept; self-join Member role allowed
- [ ] **Member lists with display fields** — name/email via `UserRef` / user-service (admin UX)
- [ ] **Org soft-delete cascade policy** — document + enforce workspace/project behavior when org is deactivated
- [x] **Missing org Kafka events** — `organization.updated`, `organization.deleted`, `organization.member_removed`
- [ ] **Tags** — workspace tags from [database.md](../../../docs/database.md) (design-only today); only if needed before documents

### Hardening

- [ ] **Pagination** on list orgs / members / workspaces / projects
- [x] **Idempotent admin flows** — create org + owner membership + default workspace in one transaction
- [ ] **Audit fields** — who created/updated (columns or activity via collab)
- [ ] **Basic org-level counts** — workspaces / projects / members for admin dashboard (no analytics-service yet)

### Stretch / later

- Split platform into separate org/workspace/project deployables — **not** recommended in Phase 2  
- Billing / plans / `max_members` UI  
- Custom roles CRUD beyond seeded system roles  

## Security

| Layer | How |
|-------|-----|
| Client gate | Header `X-API-Key` = env `API_KEY` |
| User auth | `Authorization: Bearer` (JWT); `@CurrentUserId` |
| Authorization | `PermissionService` + membership tables |

Prefer gateway `:8080`. Swagger: http://localhost:8082/swagger-ui.html

## Layout

`controller` / `service`/`impl` / `dto` / `repository` / `model` / `config` / `event`  
Conventions: `@CurrentUserId` + `@ApiId` — `.cursor/rules/java-spring.mdc`.

## Run

Needs Postgres (`users` from auth Flyway first) + Kafka. Migrations: `flyway_platform_history` (V1 roles → V4 projects). Secrets from repo-root `.env`.

```powershell
cd D:\codes\CollabFlow_V2\backend\java-services
.\gradlew.bat :platform-service:bootRun
```

## Try it (via gateway)

```powershell
$k = $env:API_KEY
$base = "http://localhost:8080"
# After login — Bearer required
curl -X POST "$base/api/v1/organizations" -H "Content-Type: application/json" -H "X-API-Key: $k" `
  -H "Authorization: Bearer <accessToken>" -d "{\"name\":\"Acme\",\"slug\":\"acme\"}"

curl "$base/api/v1/organizations" -H "X-API-Key: $k" -H "Authorization: Bearer <accessToken>"
```

Full path: [e2e-demo.md](../../../docs/e2e-demo.md) · Postman: `postman/CollabFlow-Platform.postman_collection.json`.
