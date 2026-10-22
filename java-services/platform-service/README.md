# platform-service

**Port:** 8082 · Java 21 / Boot 4.1 / Gradle  
**Owns:** organizations, organization_members, workspaces, workspace_members, roles, permissions, role_permissions

## Week 5 — Organizations

- Create org → caller becomes **Organization Owner**
- List / get / update / soft-delete
- Members: add / change role / remove
- Permission checks via `role_permissions`
- Kafka after commit: `organization.created`, `organization.member_added`

## Week 6 — Workspaces

- Create workspace under an org (`workspace.create` on org role) → caller becomes **Workspace Admin**
- List by org (org members), get/update/soft-delete (workspace permissions)
- Members + **join via invite code** (must already be org member)
- Kafka: `workspace.created`, `workspace.updated`, `workspace.deleted`, `workspace.member_added`, `workspace.member_removed`

Seeded role IDs:

| Role | UUID |
|------|------|
| Organization Owner | `a0000000-0000-4000-8000-000000000001` |
| Organization Admin | `a0000000-0000-4000-8000-000000000002` |
| Organization Member | `a0000000-0000-4000-8000-000000000003` |
| Workspace Admin | `a0000000-0000-4000-8000-000000000004` |
| Workspace Member | `a0000000-0000-4000-8000-000000000005` |
| Workspace Viewer | `a0000000-0000-4000-8000-000000000006` |

## Run

Needs Postgres (`users` from auth Flyway first), Kafka. Prefer gateway `:8080`.
Platform migrations use table `flyway_platform_history` (V1 roles, V2 orgs, V3 workspaces).
`baseline-on-migrate` + `baseline-version: 0` is required because the shared schema already has auth tables.

```powershell
cd D:\Codes\CollabFlow_V2\java-services
.\gradlew.bat :platform-service:bootRun
```

Swagger: http://localhost:8082/swagger-ui.html

## Headers

- `X-API-Key`
- `Authorization: Bearer <accessToken>` (from auth login)

## Conventions

- Controllers use `@CurrentUserId` (JWT principal UUID) and `@ApiId` (MDC) — see `.cursor/rules/java-spring.mdc` + [services-mvp.md](../../docs/services-mvp.md).
- Domain logic: `OrganizationService` / `WorkspaceService` + `service/impl/`
- Logs: console + dated files under repo `logs/platform/platform-service.yyyy-MM-dd.log`.
