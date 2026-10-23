# Implementation tracker

## How to update

- Status: Not started | In progress | Done | Blocked
- A feature is Done only when backend and frontend both work for that row, the frontend build is clean, and touched backend tests pass.

## Features

| Id | Area | Pages / routes | Endpoints | Backend changes | Frontend | Backend | Tests | Notes |
|----|------|----------------|-----------|-----------------|----------|---------|-------|-------|
| A1 | Auth | `/`, `/login` | AUTH-003, USER-001 | — | Done | Done | next build | existing |
| A2 | Auth | `/register` | AUTH-002 | — | Done | Done | next build | gated by server flag |
| A3 | Auth | `/verify-email`, login resend | AUTH-008, AUTH-009 | — | Done | Done | next build | |
| A4 | Auth | `/forgot-password`, `/reset-password` | AUTH-010, AUTH-011 | — | Done | Done | next build | |
| A5 | Auth | `/select-org`, logout | AUTH-016, AUTH-017, AUTH-005 | — | Done | Done | next build | existing |
| A6 | Auth | API client | AUTH-004 | — | Done | Done | next build | silent refresh on 401 |
| A7 | Settings | `/settings/security` | AUTH-012–015 | — | Done | Done | next build | |
| B1 | Profile | `/settings/profile` | USER-001, USER-002 | — | Done | Done | next build | |
| C1 | Admin | `/admin`, `/admin/users`, `/admin/organizations` | USER-020, ADMIN-ORG-001, ADMIN-ORG-002 | — | Done | Done | next build | create actions are dialogs on the directories |
| C2 | Admin | `/admin/organizations/[id]` transfer | ADMIN-ORG-003 | — | Done | Done | next build | new owner must already be a member |
| C3 | Admin | `/admin/users`, `/admin/users/[id]`, `/admin/organizations`, `/admin/organizations/[id]` | USER-021, USER-022, ADMIN-ORG-004, ADMIN-ORG-005, ADMIN-ORG-009, ADMIN-ORG-010 | paged directory queries | Done | Done | admin SQL tests | scratch tables removed |
| C4 | Admin | user detail actions | AUTH-021–025, USER-023, USER-024 | deactivate, reset, resend, master-admin flag, soft delete | Done | Done | AuthServiceImplTest | deleted login message is distinct |
| C5 | Admin | org detail actions | ADMIN-ORG-006, ADMIN-ORG-007 | suspend and activate orgs | Done | Done | AdminConsoleServiceTest | ORG-005 stays owner-only |
| C6 | Admin | `/admin` | ADMIN-STATS-001 | counts and 30-day signups | Done | Done | next build | active users = last_login_at within 7 days |
| C7 | Admin | export buttons | USER-025, ADMIN-ORG-008 | CSV, 5000 row cap | Done | Done | next build | same filters as the tables |
| C8 | Admin | `/admin/audit` | ADMIN-AUDIT-001 | `admin_audit_events` | Done | Done | next build | each admin mutation writes a row |
| C9 | Admin | `/admin/announcements` + shell banner | ADMIN-ANN-001–004, ANN-001 | `announcements` | Done | Done | next build | any signed-in user can read the active banner |
| C10 | Admin | `/admin/feature-flags` | ADMIN-FLAG-001–003, FLAG-001 | `feature_flags` | Done | Done | next build | missing key means enabled; existing pages are not gated |
| C11 | Admin | user detail “View as user” | AUTH-026, AUTH-027 | 15-minute read-only token | Done | Done | ImpersonationRulesTest | no refresh while impersonating |
| D1 | Org | `/org`, `/org/settings`, `/org/new` | ORG-001, ORG-003–005, ORG-010 | — | Done | Done | next build | self-serve still gated |
| D2 | Org members | `/org/members` | ORG-006–009, ROLE-001 | name + email on member DTOs | Done | Done | platform compile | |
| E1 | Invitations | `/org/invitations`, `/invitations`, `/invite/[token]` | INV-001–006 | — | Done | Done | next build | lists are paged |
| F1 | Workspaces | `/workspaces`, `/workspaces/[id]` | WS-001–003, PRJ-001–002 | — | Done | Done | next build | existing, linked to projects |
| F2 | Workspace settings | `/workspaces/[id]/settings`, `/workspaces/join` | WS-004, WS-005, WS-010 | — | Done | Done | next build | |
| F3 | Workspace members | `/workspaces/[id]/members` | WS-006–009 | name + email | Done | Done | platform compile | |
| G1 | Projects | `/projects/[id]`, settings, members | PRJ-003–009, TASK-008, ACT-003 | name + email on project members | Done | Done | next build | |
| H1 | Tasks | `/projects/[id]/tasks`, board, detail | TASK-001–010, WF-001, WF-004 | — | Done | Done | next build | CRITICAL maps to urgent chip |
| I1 | Sprints | `/projects/[id]/sprints`, detail | SPRINT-001–009 | — | Done | Done | next build | |
| J1 | Comments | task detail panel | CMT-001–005 | — | Done | Done | next build | |
| K1 | Activity | `/activity`, project/task timelines | ACT-001, ACT-003 | — | Done | Done | next build | already paged |
| K2 | Notifications | `/notifications`, sidebar count | NOTIF-001–004 | — | Done | Done | next build | already paged |
| L1 | Realtime | board + task detail | RT-001 | — | Done | Done | next build | reload on project room message |
| M1 | Documents | `/projects/[id]/documents` | DOC-001–003 | disk storage, JWT, project ACL, AutoMigrate | Done | Done | `go test ./internal/storage` | MinIO not in slim Compose |

## Changelog

### Phase 1 — tracker + client (2026-10-23)

- Built: `docs/implementation-tracker.md`, AUTH-004 retry in `frontend/lib/api/client.ts`
- Decisions: one refresh in flight; 403 is not retried

### Phase 2 — auth + settings (2026-10-23)

- Built: register, resend, forgot/reset, profile, security
- Files: `frontend/app/(auth)/*`, `frontend/app/(app)/settings/*`, `frontend/lib/api/auth.ts`

### Phase 3 — org + workspaces (2026-10-23)

- Fixed: member responses now include `name` and `email` via `UserDisplayLookup` (read-only SQL on `users`)
- Built: org overview/settings/members, workspace settings/members/join
- Files: platform `MemberResponse`, `WorkspaceMemberResponse`, `ProjectMemberResponse`, three service impls

### Phase 4 — invitations (2026-10-23)

- Built: send, list, cancel, my invitations, public peek + accept
- Decision: invite list endpoints already return `PageResponse`; the UI reads `content`

### Phase 5 — projects + tasks (2026-10-23)

- Built: project home, settings, members, task table, board, detail, subtasks
- Decision: board columns come from WF-001 statuses; move uses TASK-006

### Phase 6 — comments + sprints (2026-10-23)

- Built: comment thread on the task page; sprint list and detail actions

### Phase 7 — activity + notifications (2026-10-23)

- Built: `/activity`, `/notifications`, unread count in the sidebar
- Decision: both lists were already paged; no new pagination API

### Phase 8 — realtime (2026-10-23)

- Built: WebSocket subscribe to `project:{id}` through the gateway; pages reload from REST

### Phase 9 — documents (2026-10-23)

- Built: real upload, list, and download
- Decision: files go to `DOCUMENT_STORAGE_DIR`. Slim Compose has no MinIO process, so object storage stays a follow-up. Metadata uses GORM AutoMigrate, same as collab-service.
- Docs: `docs/services-mvp.md`, `docs/fe-contract.md`, document-service README

### Phase 10 — admin transfer + demo removal (2026-10-23)

- Built: admin transfer form
- Removed: `frontend/app/(demo)/`, `frontend/demo/`, `lib/demo-nav-links.ts`, `lib/features.ts`, `NEXT_PUBLIC_ENABLE_DEMO`

### Phase 11 — master admin console (2026-10-23)

- Built: user and organization directories, detail actions, dashboard, CSV export, audit log, announcements, feature flags, read-only impersonation
- Schema: auth `V3__admin_user_lifecycle.sql` (`deleted_at`, `last_login_at`); platform `V5__admin_platform.sql`
- Decision: suspend is `is_active`. There is no separate ban status. Audit rows are inserted with JDBC so auth and user-service do not validate platform's table at boot.
- Gateway: credential admin paths are routed to auth-service ahead of the `/admin/users/**` wildcard. An impersonation token may GET/HEAD, plus POST the end route.

## Backend gaps

| Id | Gap | Phase | Status |
|----|-----|-------|--------|
| G1 | Member DTOs lacked name/email | 3 | Done |
| G2 | Document upload/list were stubs | 9 | Done |
| G3 | No single “my permissions” API | — | Open — UI uses role id from membership |
| G4 | No user search; add-member forms take a user id | — | Open |

## Bugs found

| Id | Where | Symptom | Fix | Status |
|----|-------|---------|-----|--------|
| B1 | Member list DTOs | Tables could not show who a member is | Added name and email | Done |
| B2 | DOC-001/002 | Routes returned a stub and an empty list | Replaced with storage + ACL | Done |

## Follow-ups

| Id | Item | Why deferred |
|----|------|----------------|
| F1 | MinIO/S3 for document bytes | Not in the slim Compose stack; disk storage is the working store |
| F2 | User search for member pickers | No existing endpoint; UI uses user id |
| F3 | Server-side pagination for member lists | Lists are bounded for MVP; tables page on the client |
| F4 | Drag-and-drop on the board | Status change uses the column select and TASK-006 |
