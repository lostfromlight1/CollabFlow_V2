# CollabFlow Database Documentation

**Database Name**: `collabflow`  
**Version**: 2.5.0  
**Status**: Slim MVP **complete** (Weeks 1–14); Phase 2 Wave 5 sprints migrated  

**Technology**: **PostgreSQL 17** (shared database) — [tech-stack.md](tech-stack.md)  
**ORM**: Spring Data JPA / Hibernate (Java, Boot 4.1.x) + GORM AutoMigrate (`collab-service`)  
**Auth**: JWT via `auth-service`

> **Architecture:** Deployables in [services-mvp.md](services-mvp.md). This file is the **schema catalog**.  
> Prefer **Migrated (shipped)** below when coding. Sections further down may still describe **design-only** columns/tables for Phase 2 — do not invent Flyway for those until scope expands.

---

## Table ownership (slim MVP)

All services connect to the same `collabflow` database. “Owns” = which service migrates / writes.

### Migrated (shipped)

| Slim service | How | Tables |
|--------------|-----|--------|
| `auth-service` | Flyway | `users` (schema + credentials/sessions columns), `refresh_tokens`, `email_tokens` |
| `user-service` | Shared table (no Flyway) | **Writes** profile fields + admin create on `users`; does not own migrations |
| `platform-service` | Flyway | `roles`, `permissions`, `role_permissions`, `organizations`, `organization_members`, `workspaces`, `workspace_members`, `projects`, `project_members` |
| `task-service` | Flyway | `status_workflows`, `workflow_statuses`, `workflow_transitions`, `task_types`, `tasks`, `sprints` (`tasks.sprint_id` FK) |
| `collab-service` | GORM AutoMigrate | `notifications`, `invitations`, `comments`, `comment_mentions`, `activity_feed`, `processed_events` (consumer idempotency) |
| `realtime-service` | — | **none** (Redis + Kafka; read-only ACL on `project_members` / `organization_members` / `projects` / `workspaces` / `tasks`) |
| `api-gateway` | — | **none** |

### Design-only / not migrated in MVP

Kept in this doc for later; **do not** add Flyway/GORM until Phase 2+ ([future-plan.md](future-plan.md), [features.md](features.md)):

| Area | Tables |
|------|--------|
| Platform | `tags` |
| Task extras | `watchers`, `custom_fields`, `custom_field_options`, `task_type_custom_fields`, `task_custom_field_values`, `task_dependencies`, `task_tags` |
| Collab extras | `comment_attachments`, `comment_reactions` |
| Phase 2 documents | `document_folders`, `documents`, `document_permissions`, `document_tags` |

**Cross-service reads:** OK (e.g. collab reads `users`). Prefer IDs in Kafka events over wide cross-owner joins.

**Roles:** `roles` / `permissions` owned by **platform**. Task seeds `task.*` and `sprint.*` permissions into those shared tables.

---

## MVP vs full schema

| Shipped in MVP | Deferred |
|----------------|----------|
| Auth, platform hierarchy, RBAC seed, workflows + tasks + sprints, collab core tables above | Documents, tags, custom fields, watchers, comment reactions/attachments |
| Default workflow + system task types seed | Search/export tables (none) |
| `sprints` + `tasks.sprint_id` FK (Phase 2 Wave 5) | Sprint analytics / export |

---

## Table of Contents

1. [Table ownership (slim MVP)](#table-ownership-slim-mvp)
2. [Authentication Tables](#authentication-tables)
3. [Organization Management Tables](#organization-management-tables)
4. [Workspace Management Tables](#workspace-management-tables)
5. [Project Management Tables](#project-management-tables)
6. [Task Management Tables](#task-management-tables)
7. [Document Management Tables](#document-management-tables) (Phase 2)
8. [Collaboration Tables](#collaboration-tables)
9. [Notification & Activity Tables](#notification--activity-tables)
10. [Entity Relationships](#entity-relationships)
11. [Indexes](#indexes)
12. [Data Types & Constraints](#data-types--constraints)
13. [Seed Data](#seed-data)

---

## Authentication Tables

**Owner (schema / credentials):** `auth-service` (Flyway).  
**Phase 2 Wave 3 split:** `users` remains a **shared** table. **`user-service`** owns profile mutations (`GET/PUT /api/v1/users/me`) and admin create (`POST /api/v1/admin/users`, including initial password hash). **`auth-service`** owns credentials/sessions/`email_tokens` (login, JWT, refresh, verify/reset, change-password) and still runs Flyway for `users` / `refresh_tokens` / `email_tokens`. Auth may still insert rows on public register when enabled.

### 1. users

**Purpose**: Stores user account information for JWT authentication and profile display.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique user identifier |
| email | VARCHAR(255) | UNIQUE, NOT NULL | User's email address (login identifier) |
| password | VARCHAR(255) | NOT NULL | Hashed password (bcrypt/argon2) |
| name | VARCHAR(255) | NOT NULL | User's full display name |
| avatar_url | VARCHAR(500) | NULL | URL to user's profile picture |
| phone | VARCHAR(20) | NULL | User's phone number |
| bio | TEXT | NULL | User's biography/profile description |
| is_active | BOOLEAN | DEFAULT true | Account status (active/inactive) |
| is_email_verified | BOOLEAN | DEFAULT false | Email verification status |
| is_master_admin | BOOLEAN | DEFAULT false | Phase 2: admin portal master admin |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Account creation timestamp |
| updated_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Indexes**:
- `idx_users_email` on email (for login queries)
- `idx_users_is_active` on is_active (for filtering active users)
- partial `idx_users_is_master_admin` where `is_master_admin = true`

**Notes**:
- Password should be hashed using bcrypt with minimum 10 rounds
- Email should be case-insensitive for uniqueness checks
- Avatar URL can be relative path or absolute URL
- Master admin is seeded by auth-service on boot (`MASTER_ADMIN_EMAIL` / `MASTER_ADMIN_PASSWORD`)

---

### 1b. email_tokens (Phase 2 — auth)

**Purpose**: One-time tokens for email verify and password reset.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY | Token row id |
| user_id | UUID | FK users.id ON DELETE CASCADE | Owner |
| token | VARCHAR(128) | UNIQUE, NOT NULL | Opaque token string |
| purpose | VARCHAR(32) | NOT NULL | `VERIFY_EMAIL` or `RESET_PASSWORD` |
| expires_at | TIMESTAMPTZ | NOT NULL | Expiry |
| used_at | TIMESTAMPTZ | NULL | When consumed |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Created |

---

### 2. refresh_tokens

**Purpose**: Manages JWT refresh tokens for secure authentication and token rotation.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique token identifier |
| user_id | UUID | FOREIGN KEY (users.id), NOT NULL, ON DELETE CASCADE | Associated user |
| token | VARCHAR(500) | UNIQUE, NOT NULL | Encoded refresh token |
| expires_at | TIMESTAMPTZ | NOT NULL | Token expiration timestamp |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Token creation timestamp |
| revoked_at | TIMESTAMPTZ | NULL | Token revocation timestamp |
| is_revoked | BOOLEAN | DEFAULT false | Revocation status |

**Indexes**:
- `idx_refresh_tokens_user_id` on user_id (for user token lookups)
- `idx_refresh_tokens_token` on token (for token validation)
- `idx_refresh_tokens_expires_at` on expires_at (for cleanup jobs)

**Notes**:
- Tokens should be rotated on each refresh (old token revoked, new token issued)
- Implement cleanup job to delete expired tokens older than 30 days
- Token should be signed with HS256 or RS256 algorithm

---

## Organization Management Tables

**Owner:** `platform-service` (with workspaces, projects, roles)

### 3. organizations

**Purpose**: Represents organizations/companies (top-level entity).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique organization identifier |
| name | VARCHAR(255) | NOT NULL | Organization name |
| description | TEXT | NULL | Organization description |
| logo_url | VARCHAR(500) | NULL | Organization logo URL |
| owner_id | UUID | FOREIGN KEY (users.id), NOT NULL, ON DELETE CASCADE | Organization owner |
| slug | VARCHAR(100) | UNIQUE, NOT NULL | URL-friendly organization identifier |
| plan | VARCHAR(20) | NOT NULL, DEFAULT 'FREE', CHECK (plan IN ('FREE', 'PRO', 'ENTERPRISE')) | Subscription plan |
| max_members | INTEGER | DEFAULT 10 | Maximum allowed members |
| max_workspaces | INTEGER | DEFAULT 5 | Maximum allowed workspaces |
| is_active | BOOLEAN | DEFAULT true | Organization active status |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Indexes**:
- `idx_organizations_owner_id` on owner_id (for owner's organizations)
- `idx_organizations_slug` on slug (for URL lookup)
- `idx_organizations_is_active` on is_active (for filtering active organizations)

**Notes**:
- slug should be lowercase, alphanumeric with hyphens
- Plan determines feature limits and pricing
- Soft delete recommended (set is_active = false)

---

### 4. organization_members

**Purpose**: Manages organization membership with roles.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique membership identifier |
| organization_id | UUID | FOREIGN KEY (organizations.id), NOT NULL, ON DELETE CASCADE | Associated organization |
| user_id | UUID | FOREIGN KEY (users.id), NOT NULL, ON DELETE CASCADE | Associated user |
| role_id | UUID | FOREIGN KEY (roles.id), NOT NULL | Member's role |
| joined_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Membership join timestamp |
| last_active_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Last activity timestamp |

**Unique Constraint**: (organization_id, user_id)

**Indexes**:
- `idx_organization_members_organization_id` on organization_id (for organization member lists)
- `idx_organization_members_user_id` on user_id (for user's organizations)
- `idx_organization_members_role_id` on role_id (for filtering by role)

**Notes**:
- User must be organization member to access any workspace within organization
- Role determines permissions (defined in roles and role_permissions tables)
- Update last_active_at on each user action

---

### 5. roles

**Purpose**: Defines roles with associated permissions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique role identifier |
| name | VARCHAR(100) | NOT NULL | Role name (e.g., "Organization Admin", "Workspace Member") |
| description | TEXT | NULL | Role description |
| scope | VARCHAR(20) | NOT NULL, CHECK (scope IN ('ORGANIZATION', 'WORKSPACE', 'PROJECT')) | Role scope level |
| is_system | BOOLEAN | DEFAULT false | Pre-built system role |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

**Indexes**:
- `idx_roles_scope` on scope (for filtering by scope)
- `idx_roles_is_system` on is_system (for system roles)

**Notes**:
- System roles cannot be deleted (only modified)
- Scope determines where role applies (org/workspace/project)
- Example roles:
  - Organization: "Owner", "Admin", "Member"
  - Workspace: "Admin", "Member", "Viewer"
  - Project: "Admin", "Member", "Viewer"

---

### 6. permissions

**Purpose**: Defines individual permissions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique permission identifier |
| name | VARCHAR(100) | NOT NULL | Permission name (e.g., "workspace.create", "task.delete") |
| description | TEXT | NULL | Permission description |
| category | VARCHAR(50) | NOT NULL | Permission category (e.g., "workspace", "project", "task") |
| resource | VARCHAR(50) | NOT NULL | Resource type (e.g., "workspace", "project", "task") |
| action | VARCHAR(50) | NOT NULL | Action (e.g., "create", "read", "update", "delete") |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

**Indexes**:
- `idx_permissions_category` on category (for filtering by category)
- `idx_permissions_resource` on resource (for filtering by resource)

**Permission Examples**:
- organization.create, organization.read, organization.update, organization.delete
- workspace.create, workspace.read, workspace.update, workspace.delete, workspace.manage_members
- project.create, project.read, project.update, project.delete, project.manage_members
- task.create, task.read, task.update, task.delete, task.assign, task.move
- sprint.create, sprint.read, sprint.update, sprint.delete
- document.create, document.read, document.update, document.delete, document.share

**Notes**:
- Naming convention: `{resource}.{action}`
- Category for grouping permissions in UI
- System permissions cannot be deleted

---

### 7. role_permissions

**Purpose**: Junction table linking roles to permissions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| role_id | UUID | FOREIGN KEY (roles.id), NOT NULL, ON DELETE CASCADE | Role |
| permission_id | UUID | FOREIGN KEY (permissions.id), NOT NULL, ON DELETE CASCADE | Permission |

**Primary Key**: (role_id, permission_id)

**Notes**:
- Many-to-many relationship
- Defines what permissions each role has
- System roles come with pre-defined permissions

---

## Workspace Management Tables

**Owner:** `platform-service`

### 8. workspaces

**Purpose**: Represents teams/departments within an organization.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique workspace identifier |
| name | VARCHAR(255) | NOT NULL | Workspace name |
| description | TEXT | NULL | Workspace description/purpose |
| image_url | VARCHAR(500) | NULL | Workspace logo/banner image URL |
| organization_id | UUID | FOREIGN KEY (organizations.id), NOT NULL, ON DELETE CASCADE | Parent organization |
| owner_id | UUID | FOREIGN KEY (users.id), NOT NULL | Workspace owner |
| invite_code | VARCHAR(20) | UNIQUE, NOT NULL | 6-20 character invite code |
| default_workflow_id | UUID | FOREIGN KEY (status_workflows.id), NULL | Default workflow for new projects |
| time_zone | VARCHAR(50) | DEFAULT 'UTC' | Workspace time zone |
| date_format | VARCHAR(20) | DEFAULT 'YYYY-MM-DD' | Date display format |
| is_active | BOOLEAN | DEFAULT true | Workspace active status |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Indexes**:
- `idx_workspaces_organization_id` on organization_id (for organization workspaces)
- `idx_workspaces_owner_id` on owner_id (for owner's workspaces)
- `idx_workspaces_invite_code` on invite_code (for joining via code)
- `idx_workspaces_is_active` on is_active (for filtering active workspaces)
- `idx_workspaces_default_workflow_id` on default_workflow_id (for default workflow lookup)

**Notes**:
- Invite code should be randomly generated and case-insensitive
- Soft delete is recommended (set is_active = false instead of deleting)
- Time zone should be IANA time zone identifier (e.g., 'America/New_York')
- Workspace must belong to an organization
- default_workflow_id is used when project.workflow_id is NULL

---

### 9. workspace_members

**Purpose**: Manages workspace membership with roles.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique membership identifier |
| workspace_id | UUID | FOREIGN KEY (workspaces.id), NOT NULL, ON DELETE CASCADE | Associated workspace |
| user_id | UUID | FOREIGN KEY (users.id), NOT NULL, ON DELETE CASCADE | Associated user |
| role_id | UUID | FOREIGN KEY (roles.id), NOT NULL | Member's role (scope=WORKSPACE) |
| joined_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Membership join timestamp |
| last_active_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Last activity timestamp |

**Unique Constraint**: (workspace_id, user_id)

**Indexes**:
- `idx_workspace_members_workspace_id` on workspace_id (for workspace member lists)
- `idx_workspace_members_user_id` on user_id (for user's workspaces)
- `idx_workspace_members_role_id` on role_id (for filtering by role)

**Notes**:
- User must be organization member to be workspace member
- Role determines permissions (defined in roles and role_permissions tables)
- Update last_active_at on each user action
- No permission flags - all permissions derived from role

---

## Project Management Tables

**Owner:** `platform-service` (projects + members). Workflow tables used by projects are owned by `task-service` (see Task Management).

### 10. projects

**Purpose**: Represents projects with flexible nesting (sub-projects support).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique project identifier |
| name | VARCHAR(255) | NOT NULL | Project name |
| description | TEXT | NULL | Project description |
| image_url | VARCHAR(500) | NULL | Project image/logo URL |
| workspace_id | UUID | FOREIGN KEY (workspaces.id), NOT NULL, ON DELETE CASCADE | Parent workspace |
| parent_project_id | UUID | FOREIGN KEY (projects.id), NULL, ON DELETE SET NULL | Parent project (for sub-projects) |
| owner_id | UUID | FOREIGN KEY (users.id), NOT NULL | Project owner |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE', CHECK (status IN ('ACTIVE', 'ARCHIVED', 'ON_HOLD')) | Project status |
| project_type | VARCHAR(50) | NOT NULL, DEFAULT 'SOFTWARE', CHECK (project_type IN ('SOFTWARE', 'MARKETING', 'HR', 'CUSTOM')) | Project type/category |
| start_date | TIMESTAMPTZ | NULL | Project start date |
| due_date | TIMESTAMPTZ | NULL | Project due date |
| workflow_id | UUID | FOREIGN KEY (status_workflows.id), NULL | Assigned status workflow |
| lead_id | UUID | FOREIGN KEY (users.id), NULL | Project lead |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Indexes**:
- `idx_projects_workspace_id` on workspace_id (for workspace projects)
- `idx_projects_parent_project_id` on parent_project_id (for sub-projects)
- `idx_projects_owner_id` on owner_id (for owner's projects)
- `idx_projects_status` on status (for filtering by status)
- `idx_projects_workflow_id` on workflow_id (for workflow lookup)

**Notes**:
- Supports infinite nesting via parent_project_id
- Soft delete recommended (status = ARCHIVED)
- Project lead may differ from owner
- workflow_id can be NULL to use workspace default workflow
- Each project can have its own custom workflow or share with other projects

---

### 11. project_members

**Purpose**: Manages project membership with roles.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique project membership identifier |
| project_id | UUID | FOREIGN KEY (projects.id), NOT NULL, ON DELETE CASCADE | Associated project |
| user_id | UUID | FOREIGN KEY (users.id), NOT NULL, ON DELETE CASCADE | Associated user |
| role_id | UUID | FOREIGN KEY (roles.id), NOT NULL | Member's role (scope=PROJECT) |
| joined_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Membership join timestamp |

**Unique Constraint**: (project_id, user_id)

**Indexes**:
- `idx_project_members_project_id` on project_id (for project member lists)
- `idx_project_members_user_id` on user_id (for user's projects)
- `idx_project_members_role_id` on role_id (for filtering by role)

**Notes**:
- User must be workspace member to be project member
- Role determines permissions (defined in roles and role_permissions tables)
- No permission flags - all permissions derived from role
- Links directly to user (not workspace_member) for simpler queries

---

### 12. status_workflows

**Purpose**: Status workflows that can be assigned to projects.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique workflow identifier |
| name | VARCHAR(255) | NOT NULL | Workflow name |
| description | TEXT | NULL | Workflow description |
| workspace_id | UUID | FOREIGN KEY (workspaces.id), NULL, ON DELETE CASCADE | NULL = global template, set = workspace-specific |
| is_default | BOOLEAN | DEFAULT false | Whether this is the default workflow for new projects |
| is_template | BOOLEAN | DEFAULT false | Whether this is a template workflow (can be copied) |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Indexes**:
- `idx_status_workflows_workspace_id` on workspace_id (for workspace workflows)
- `idx_status_workflows_is_default` on is_default (for default workflow lookup)

**Notes**:
- workspace_id = NULL indicates global template workflow (shared across all workspaces)
- workspace_id = set indicates workspace-specific workflow
- is_default marks the default workflow for new projects in a workspace
- is_template marks workflows that can be copied as templates
- Projects can share the same workflow or have unique workflows

---

### 13. workflow_statuses

**Purpose**: Defines individual statuses within a workflow.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique status identifier |
| workflow_id | UUID | FOREIGN KEY (status_workflows.id), NOT NULL, ON DELETE CASCADE | Parent workflow |
| name | VARCHAR(100) | NOT NULL | Status name (e.g., "To Do", "In Progress") |
| display_order | INTEGER | NOT NULL | Display order in UI |
| color | VARCHAR(7) | DEFAULT '#6B7280' | Hex color code for UI |
| category | VARCHAR(20) | NOT NULL, CHECK (category IN ('TODO', 'IN_PROGRESS', 'DONE')) | Status category |

**Unique Constraint**: (workflow_id, display_order)

**Indexes**:
- `idx_workflow_statuses_workflow_id` on workflow_id (for workflow's statuses)

**Notes**:
- Category used for aggregate reporting (TODO vs IN_PROGRESS vs DONE)
- display_order determines left-to-right order in Kanban
- Color should be valid hex color (e.g., #FF5733)

---

### 14. workflow_transitions

**Purpose**: Defines valid status transitions within a workflow.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique transition identifier |
| workflow_id | UUID | FOREIGN KEY (status_workflows.id), NOT NULL, ON DELETE CASCADE | Parent workflow |
| from_status_id | UUID | FOREIGN KEY (workflow_statuses.id), NOT NULL, ON DELETE CASCADE | Source status |
| to_status_id | UUID | FOREIGN KEY (workflow_statuses.id), NOT NULL, ON DELETE CASCADE | Target status |
| require_comment | BOOLEAN | DEFAULT false | Whether comment is required for transition |

**Unique Constraint**: (workflow_id, from_status_id, to_status_id)

**Indexes**:
- `idx_workflow_transitions_workflow_id` on workflow_id (for workflow's transitions)

**Notes**:
- Only defined transitions are allowed
- If no transition exists, status change is blocked
- Self-transitions (from_status_id = to_status_id) not allowed
- require_comment enforces comment before transition

---

## Task Management Tables

**Owner:** `task-service` (includes workflows, custom fields, sprints, watchers, task_tags)

### 15. task_types

**Purpose**: Custom task types with associated custom fields.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique task type identifier |
| name | VARCHAR(100) | NOT NULL | Task type name (e.g., "Bug", "Story", "Epic") |
| description | TEXT | NULL | Task type description |
| icon | VARCHAR(50) | NULL | Icon/emoji for UI |
| color | VARCHAR(7) | DEFAULT '#6B7280' | Hex color code for UI |
| workspace_id | UUID | FOREIGN KEY (workspaces.id), NULL, ON DELETE CASCADE | NULL = global type |
| is_system | BOOLEAN | DEFAULT false | Pre-built system type |
| default_status_id | UUID | FOREIGN KEY (workflow_statuses.id), NULL | Initial status for this type |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

**Indexes**:
- `idx_task_types_workspace_id` on workspace_id (for workspace-specific types)

**Notes**:
- workspace_id = NULL indicates global/system task type
- is_system = true for pre-built types (Bug, Story, Epic, Task)
- Custom fields linked via task_type_custom_fields junction table
- default_status_id references workflow_statuses table (not statuses)

---

### 14. task_type_custom_fields

**Purpose**: Junction table linking task types to custom fields.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| task_type_id | UUID | FOREIGN KEY (task_types.id), NOT NULL, ON DELETE CASCADE | Task type |
| custom_field_id | UUID | FOREIGN KEY (custom_fields.id), NOT NULL, ON DELETE CASCADE | Custom field |

**Primary Key**: (task_type_id, custom_field_id)

**Notes**:
- Many-to-many relationship
- Custom fields appear for tasks of this type

---

### 15. custom_fields

**Purpose**: Defines custom field types and configurations.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique custom field identifier |
| name | VARCHAR(255) | NOT NULL | Field name |
| description | TEXT | NULL | Field description |
| field_type | VARCHAR(20) | NOT NULL, CHECK (field_type IN ('TEXT', 'NUMBER', 'DROPDOWN', 'DATE', 'USER', 'CHECKBOX', 'MULTI_SELECT')) | Field data type |
| workspace_id | UUID | FOREIGN KEY (workspaces.id), NULL, ON DELETE CASCADE | NULL = global field |
| is_required | BOOLEAN | DEFAULT false | Whether field is required |
| is_unique | BOOLEAN | DEFAULT false | Whether field value must be unique |
| default_value | TEXT | NULL | Default value |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

**Indexes**:
- `idx_custom_fields_workspace_id` on workspace_id (for workspace-specific fields)

**Field Types**:
- **TEXT**: Single-line text input
- **NUMBER**: Numeric input (integer or decimal)
- **DROPDOWN**: Single selection from predefined options
- **DATE**: Date picker
- **USER**: User selection (references users table)
- **CHECKBOX**: Boolean toggle
- **MULTI_SELECT**: Multiple selections from predefined options

**Notes**:
- Dropdown/multi-select options stored in custom_field_options table
- workspace_id = NULL indicates global/system field
- is_unique enforces uniqueness across all tasks using this field

---

### 16. custom_field_options

**Purpose**: Options for dropdown and multi-select fields.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique option identifier |
| custom_field_id | UUID | FOREIGN KEY (custom_fields.id), NOT NULL, ON DELETE CASCADE | Parent custom field |
| value | VARCHAR(255) | NOT NULL | Option value (stored in DB) |
| label | VARCHAR(255) | NOT NULL | Display label (shown in UI) |
| color | VARCHAR(7) | DEFAULT '#6B7280' | Hex color code for UI |
| display_order | INTEGER | DEFAULT 0 | Display order in dropdown |

**Indexes**:
- `idx_custom_field_options_custom_field_id` on custom_field_id (for field's options)

**Notes**:
- Only used for DROPDOWN and MULTI_SELECT field types
- display_order determines order in dropdown
- value is what's stored, label is what's shown

---

### 17. tasks

**Purpose**: Main task entity with flexible nesting.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique task identifier |
| name | VARCHAR(255) | NOT NULL | Task title |
| description | TEXT | NULL | Task description (rich text) |
| task_type_id | UUID | FOREIGN KEY (task_types.id), NOT NULL | Task type |
| project_id | UUID | FOREIGN KEY (projects.id), NOT NULL, ON DELETE CASCADE | Parent project |
| workspace_id | UUID | FOREIGN KEY (workspaces.id), NOT NULL, ON DELETE CASCADE | Parent workspace |
| organization_id | UUID | FOREIGN KEY (organizations.id), NOT NULL, ON DELETE CASCADE | Parent organization |
| parent_task_id | UUID | FOREIGN KEY (tasks.id), NULL, ON DELETE SET NULL | Parent task (for sub-tasks) |
| status_id | UUID | FOREIGN KEY (workflow_statuses.id), NOT NULL | Current status |
| assignee_id | UUID | FOREIGN KEY (users.id), NULL, ON DELETE SET NULL | Assigned user |
| reporter_id | UUID | FOREIGN KEY (users.id), NOT NULL | User who created task |
| priority | VARCHAR(20) | NOT NULL, DEFAULT 'MEDIUM', CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')) | Task priority |
| position | INTEGER | DEFAULT 0 | Position in Kanban column |
| start_date | TIMESTAMPTZ | NULL | Task start date |
| due_date | TIMESTAMPTZ | NULL | Task due date |
| estimated_hours | DECIMAL(10, 2) | NULL | Estimated effort in hours |
| actual_hours | DECIMAL(10, 2) | NULL | Actual effort in hours |
| story_points | INTEGER | NULL | Story points (for agile estimation) |
| sprint_id | UUID | FOREIGN KEY (sprints.id), NULL, ON DELETE SET NULL | Associated sprint |
| attachments_count | INTEGER | DEFAULT 0 | Number of attachments |
| comments_count | INTEGER | DEFAULT 0 | Number of comments |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Last update timestamp |
| closed_at | TIMESTAMPTZ | NULL | Task completion timestamp |

**Indexes**:
- `idx_tasks_project_id` on project_id (for project tasks)
- `idx_tasks_workspace_id` on workspace_id (for workspace tasks)
- `idx_tasks_organization_id` on organization_id (for organization tasks)
- `idx_tasks_parent_task_id` on parent_task_id (for sub-tasks)
- `idx_tasks_status_id` on status_id (for status filtering)
- `idx_tasks_assignee_id` on assignee_id (for assignee's tasks)
- `idx_tasks_sprint_id` on sprint_id (for sprint tasks)

**Notes**:
- Supports infinite nesting via parent_task_id
- assignee_id now references users directly (not project_members)
- position used for drag-and-drop ordering in Kanban
- attachments_count and comments_count for quick counts (should be updated on changes)
- closed_at set when status moves to DONE category
- organization_id added for organization-level queries

---

### 18. task_custom_field_values

**Purpose**: Stores custom field values for tasks.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique value identifier |
| task_id | UUID | FOREIGN KEY (tasks.id), NOT NULL, ON DELETE CASCADE | Associated task |
| custom_field_id | UUID | FOREIGN KEY (custom_fields.id), NOT NULL, ON DELETE CASCADE | Custom field |
| value | TEXT | NOT NULL | Field value (JSON for multi-select) |

**Unique Constraint**: (task_id, custom_field_id)

**Indexes**:
- `idx_task_custom_field_values_task_id` on task_id (for task's custom fields)

**Notes**:
- value stored as TEXT, parsed based on field_type
- For MULTI_SELECT, value is JSON array: '["option1","option2"]'
- For USER, value is user UUID
- For DATE, value is ISO date string
- For CHECKBOX, value is "true" or "false"

---

### 19. task_dependencies

**Purpose**: Defines task dependencies (blocking relationships).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique dependency identifier |
| task_id | UUID | FOREIGN KEY (tasks.id), NOT NULL, ON DELETE CASCADE | Dependent task |
| depends_on_task_id | UUID | FOREIGN KEY (tasks.id), NOT NULL, ON DELETE CASCADE | Task this depends on |
| dependency_type | VARCHAR(20) | DEFAULT 'BLOCKS', CHECK (dependency_type IN ('BLOCKS', 'RELATES_TO', 'CLONES')) | Dependency type |

**Unique Constraint**: (task_id, depends_on_task_id)

**Indexes**:
- `idx_task_dependencies_task_id` on task_id (for task's dependencies)

**Dependency Types**:
- **BLOCKS**: Task cannot start until dependency is complete
- **RELATES_TO**: Related tasks, no blocking
- **CLONES**: This task is a clone of another task

**Notes**:
- Prevent circular dependencies in application layer
- BLOCKS dependencies should prevent task status changes
- CLONES can copy custom field values from original

---

### 20. sprints

**Purpose**: Full Scrum sprint management.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique sprint identifier |
| name | VARCHAR(255) | NOT NULL | Sprint name |
| description | TEXT | NULL | Sprint description |
| project_id | UUID | FOREIGN KEY (projects.id), NOT NULL, ON DELETE CASCADE | Parent project |
| workspace_id | UUID | FOREIGN KEY (workspaces.id), NOT NULL, ON DELETE CASCADE | Parent workspace |
| organization_id | UUID | FOREIGN KEY (organizations.id), NOT NULL, ON DELETE CASCADE | Parent organization |
| sprint_number | INTEGER | NOT NULL | Sprint number (auto-increment) |
| start_date | TIMESTAMPTZ | NOT NULL | Sprint start date |
| end_date | TIMESTAMPTZ | NOT NULL | Sprint end date |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PLANNING', CHECK (status IN ('PLANNING', 'ACTIVE', 'COMPLETED', 'CANCELLED')) | Sprint status |
| goal | TEXT | NULL | Sprint goal |
| capacity | INTEGER | NULL | Total capacity (story points or hours) |
| velocity | INTEGER | NULL | Actual velocity (calculated after completion) |
| backlog_order | INTEGER | DEFAULT 0 | Order in backlog |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Indexes**:
- `idx_sprints_project_id` on project_id (for project sprints)
- `idx_sprints_workspace_id` on workspace_id (for workspace sprints)
- `idx_sprints_organization_id` on organization_id (for organization sprints)
- `idx_sprints_status` on status (for filtering by status)

**Sprint Lifecycle**:
1. **PLANNING**: Sprint is being planned, tasks being added
2. **ACTIVE**: Sprint is in progress, no new tasks should be added
3. **COMPLETED**: Sprint finished, velocity calculated
4. **CANCELLED**: Sprint cancelled before completion

**Notes**:
- sprint_number should auto-increment per project
- Only one sprint per project should be ACTIVE at a time
- capacity vs velocity for planning vs actual comparison
- backlog_order for ordering in sprint planning view
- organization_id added for organization-level queries

---

## Document Management Tables

> **Phase 2 — Wave 7.** Do not create these tables in MVP Flyway migrations until document-service owns them. Scaffold: `go-services/document-service` (:9083). Owner: `document-service` (Go).

### 21. document_folders

**Purpose**: Folder structure for document organization.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique folder identifier |
| name | VARCHAR(255) | NOT NULL | Folder name |
| workspace_id | UUID | FOREIGN KEY (workspaces.id), NOT NULL, ON DELETE CASCADE | Parent workspace |
| project_id | UUID | FOREIGN KEY (projects.id), NULL, ON DELETE CASCADE | Parent project (optional) |
| parent_folder_id | UUID | FOREIGN KEY (document_folders.id), NULL, ON DELETE SET NULL | Parent folder (for nesting) |
| created_by | UUID | FOREIGN KEY (users.id), NOT NULL | User who created folder |
| color | VARCHAR(7) | NULL | Hex color code for UI |
| icon | VARCHAR(50) | NULL | Icon for UI |
| display_order | INTEGER | DEFAULT 0 | Display order |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

**Indexes**:
- `idx_document_folders_workspace_id` on workspace_id (for workspace folders)
- `idx_document_folders_project_id` on project_id (for project folders)
- `idx_document_folders_parent_folder_id` on parent_folder_id (for sub-folders)

**Notes**:
- Supports infinite nesting via parent_folder_id
- project_id = NULL means workspace-level folder
- display_order for custom sorting in UI

---

### 22. documents

**Purpose**: Full document management with version control.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique document identifier |
| name | VARCHAR(255) | NOT NULL | Document filename |
| description | TEXT | NULL | Document description |
| file_path | VARCHAR(500) | NOT NULL | Path to stored file |
| file_type | VARCHAR(100) | NOT NULL | MIME type (e.g., 'application/pdf') |
| file_size | BIGINT | NOT NULL | File size in bytes |
| entity_type | VARCHAR(20) | NOT NULL, CHECK (entity_type IN ('WORKSPACE', 'PROJECT', 'TASK', 'COMMENT')) | Attached entity type |
| entity_id | UUID | NOT NULL | Attached entity ID |
| folder_id | UUID | FOREIGN KEY (document_folders.id), NULL, ON DELETE SET NULL | Parent folder |
| uploaded_by | UUID | FOREIGN KEY (users.id), NOT NULL | User who uploaded |
| version | INTEGER | NOT NULL, DEFAULT 1 | Version number |
| parent_document_id | UUID | FOREIGN KEY (documents.id), NULL, ON DELETE SET NULL | Previous version |
| is_latest | BOOLEAN | DEFAULT true | Whether this is latest version |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Indexes**:
- `idx_documents_entity` on (entity_type, entity_id) (for entity's documents)
- `idx_documents_folder_id` on folder_id (for folder's documents)
- `idx_documents_uploaded_by` on uploaded_by (for user's uploads)

**Notes**:
- Version control: new upload creates new row, parent_document_id points to previous
- is_latest = true only for most recent version
- entity_type + entity_id determines what document is attached to
- file_path can be local path or cloud storage URL (S3, etc.)

---

### 23. document_permissions

**Purpose**: Fine-grained document access control.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique permission identifier |
| document_id | UUID | FOREIGN KEY (documents.id), NOT NULL, ON DELETE CASCADE | Document |
| user_id | UUID | FOREIGN KEY (users.id), NULL, ON DELETE CASCADE | User (NULL = role-based) |
| role_id | UUID | FOREIGN KEY (roles.id), NULL | ON DELETE CASCADE | Role for permission |
| can_view | BOOLEAN | DEFAULT true | Can view document |
| can_edit | BOOLEAN | DEFAULT false | Can edit document |
| can_download | BOOLEAN | DEFAULT true | Can download document |

**Unique Constraint**: (document_id, user_id)

**Indexes**:
- `idx_document_permissions_document_id` on document_id (for document's permissions)

**Notes**:
- user_id = NULL means permission applies to all users with specified role
- If no permission entry, organization/workspace/project membership determines access
- Permissions checked in application layer

---

### 24. document_tags

**Purpose**: Junction table linking documents to tags.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| document_id | UUID | FOREIGN KEY (documents.id), NOT NULL, ON DELETE CASCADE | Document |
| tag_id | UUID | FOREIGN KEY (tags.id), NOT NULL, ON DELETE CASCADE | Tag |

**Primary Key**: (document_id, tag_id)

**Notes**:
- Many-to-many relationship
- Tags used for document categorization and search

---

### 25. tags

**Purpose**: Per-workspace tags/labels.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique tag identifier |
| name | VARCHAR(100) | NOT NULL | Tag name |
| color | VARCHAR(7) | DEFAULT '#6B7280' | Hex color code for UI |
| workspace_id | UUID | FOREIGN KEY (workspaces.id), NOT NULL, ON DELETE CASCADE | Parent workspace |
| usage_count | INTEGER | DEFAULT 0 | Number of times tag is used |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

**Indexes**:
- `idx_tags_workspace_id` on workspace_id (for workspace tags)

**Notes**:
- Tags are workspace-specific (not global)
- usage_count updated when tag is added/removed
- Tag names should be case-insensitive for uniqueness (enforced in app layer)

---

### 26. task_tags

**Purpose**: Many-to-many relationship between tasks and tags.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| task_id | UUID | FOREIGN KEY (tasks.id), NOT NULL, ON DELETE CASCADE | Task |
| tag_id | UUID | FOREIGN KEY (tags.id), NOT NULL, ON DELETE CASCADE | Tag |
| added_by | UUID | FOREIGN KEY (users.id), NOT NULL | User who added tag |
| added_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | When tag was added |

**Primary Key**: (task_id, tag_id)

**Indexes**:
- `idx_task_tags_task_id` on task_id (for task's tags)
- `idx_task_tags_tag_id` on tag_id (for tag's tasks)

**Notes**:
- Tracks who added which tag (audit trail)
- Same tag can be added to multiple tasks

---

## Collaboration Tables

**Owner:** `collab-service` (Go). Comment attachments that reference `documents` stay unused until Phase 2 documents exist (or store lightweight URLs later).

### 27. comments

**Purpose**: Comments with rich collaboration features.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique comment identifier |
| content | TEXT | NOT NULL | Comment content (rich text) |
| entity_type | VARCHAR(20) | NOT NULL, CHECK (entity_type IN ('TASK', 'PROJECT', 'DOCUMENT')) | Commented entity type |
| entity_id | UUID | NOT NULL | Commented entity ID |
| author_id | UUID | FOREIGN KEY (users.id), NOT NULL | Comment author |
| parent_comment_id | UUID | FOREIGN KEY (comments.id), NULL, ON DELETE SET NULL | Parent comment (for replies) |
| is_internal | BOOLEAN | DEFAULT false | Internal-only comment |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Indexes**:
- `idx_comments_entity` on (entity_type, entity_id) (for entity's comments)
- `idx_comments_author_id` on author_id (for user's comments)
- `idx_comments_parent_comment_id` on parent_comment_id (for comment's replies)

**Notes**:
- Supports threaded replies via parent_comment_id
- is_internal for comments only visible to team members
- Rich text content (markdown, HTML, etc.)
- Attachments linked via comment_attachments table

---

### 28. comment_attachments

**Purpose**: Links comments to document attachments.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| comment_id | UUID | FOREIGN KEY (comments.id), NOT NULL, ON DELETE CASCADE | Comment |
| document_id | UUID | FOREIGN KEY (documents.id), NOT NULL, ON DELETE CASCADE | Attached document |

**Primary Key**: (comment_id, document_id)

**Notes**:
- Many-to-many relationship
- Documents can be attached to multiple comments

---

### 29. comment_reactions

**Purpose**: Track who reacted to which comment.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique reaction identifier |
| comment_id | UUID | FOREIGN KEY (comments.id), NOT NULL, ON DELETE CASCADE | Comment |
| user_id | UUID | FOREIGN KEY (users.id), NOT NULL, ON DELETE CASCADE | User who reacted |
| emoji | VARCHAR(50) | NOT NULL | Emoji (e.g., '👍', '❤️') |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | When reaction was added |

**Unique Constraint**: (comment_id, user_id, emoji)

**Indexes**:
- `idx_comment_reactions_comment_id` on comment_id (for comment's reactions)
- `idx_comment_reactions_user_id` on user_id (for user's reactions)

**Notes**:
- Same user can react with different emojis
- Reacting again with same emoji removes the reaction (toggle)
- Common emojis: 👍 👎 ❤️ 😂 🎉 🔥

---

### 30. comment_mentions

**Purpose**: Track @mentions in comments.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique mention identifier |
| comment_id | UUID | FOREIGN KEY (comments.id), NOT NULL, ON DELETE CASCADE | Comment |
| mentioned_user_id | UUID | FOREIGN KEY (users.id), NOT NULL, ON DELETE CASCADE | Mentioned user |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | When mention was added |

**Unique Constraint**: (comment_id, mentioned_user_id)

**Indexes**:
- `idx_comment_mentions_comment_id` on comment_id (for comment's mentions)
- `idx_comment_mentions_mentioned_user_id` on mentioned_user_id (for user's mentions)

**Notes**:
- Triggers notification to mentioned user
- Parse comment content for @username patterns
- Duplicate mentions ignored (enforced by unique constraint)

---

## Notification & Activity Tables

**Owner:** `collab-service` (Go) for `activity_feed`, `notifications`, `invitations`.  
**Exception:** `watchers` is written by `task-service` (Java); collab only reacts to events.

### 31. activity_feed

**Purpose**: Track all user actions for activity feed.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique activity identifier |
| action_type | VARCHAR(50) | NOT NULL, CHECK (action_type IN ('TASK_CREATED', 'TASK_UPDATED', 'TASK_ASSIGNED', 'TASK_DELETED', 'COMMENT_ADDED', 'MEMBER_JOINED', 'MEMBER_LEFT', 'PROJECT_CREATED', 'SPRINT_STARTED', 'DOCUMENT_UPLOADED')) | Action type |
| entity_type | VARCHAR(20) | NOT NULL, CHECK (entity_type IN ('TASK', 'PROJECT', 'WORKSPACE', 'DOCUMENT', 'SPRINT', 'ORGANIZATION')) | Affected entity type |
| entity_id | UUID | NOT NULL | Affected entity ID |
| organization_id | UUID | FOREIGN KEY (organizations.id), NOT NULL, ON DELETE CASCADE | Organization |
| workspace_id | UUID | FOREIGN KEY (workspaces.id), NULL, ON DELETE SET NULL | Workspace (optional) |
| project_id | UUID | FOREIGN KEY (projects.id), NULL, ON DELETE SET NULL | Project (optional) |
| user_id | UUID | FOREIGN KEY (users.id), NOT NULL | User who performed action |
| actor_name | VARCHAR(255) | NOT NULL | Actor's display name |
| actor_email | VARCHAR(255) | NOT NULL | Actor's email |
| changes | JSONB | NULL | Before/after values (for updates) |
| metadata | JSONB | NULL | Additional context |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | When action occurred |

**Indexes**:
- `idx_activity_feed_organization_id` on organization_id (for organization activity)
- `idx_activity_feed_workspace_id` on workspace_id (for workspace activity)
- `idx_activity_feed_project_id` on project_id (for project activity)
- `idx_activity_feed_user_id` on user_id (for user's activity)
- `idx_activity_feed_entity` on (entity_type, entity_id) (for entity's activity)
- `idx_activity_feed_created_at` on created_at DESC (for timeline ordering)

**Notes**:
- changes JSONB structure: `{"field": {"old": "value", "new": "value"}}`
- metadata JSONB for custom context per action type
- Implement cleanup job to archive activities older than 90 days
- Activity feed visible to organization members
- organization_id now required for organization-level filtering

---

### 32. notifications

**Purpose**: User notifications with types.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique notification identifier |
| recipient_id | UUID | FOREIGN KEY (users.id), NOT NULL, ON DELETE CASCADE | Notification recipient |
| sender_id | UUID | FOREIGN KEY (users.id), NULL | User who triggered notification |
| notification_type | VARCHAR(50) | NOT NULL, CHECK (notification_type IN ('TASK_ASSIGNED', 'COMMENT_MENTION', 'PROJECT_INVITE', 'SPRINT_STARTED', 'TASK_UPDATED', 'DOCUMENT_SHARED', 'ORGANIZATION_INVITE')) | Notification type |
| title | VARCHAR(255) | NOT NULL | Notification title |
| message | TEXT | NOT NULL | Notification message |
| entity_type | VARCHAR(20) | NOT NULL, CHECK (entity_type IN ('TASK', 'PROJECT', 'WORKSPACE', 'DOCUMENT', 'SPRINT', 'ORGANIZATION')) | Related entity type |
| entity_id | UUID | NOT NULL | Related entity ID |
| organization_id | UUID | FOREIGN KEY (organizations.id), NOT NULL, ON DELETE CASCADE | Organization |
| is_read | BOOLEAN | DEFAULT false | Read status |
| read_at | TIMESTAMPTZ | NULL | When notification was read |
| action_url | VARCHAR(500) | NULL | Deep link to relevant entity |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | When notification was created |

**Indexes**:
- `idx_notifications_recipient_id` on recipient_id (for user's notifications)
- `idx_notifications_organization_id` on organization_id (for organization notifications)
- `idx_notifications_is_read` on is_read (for filtering unread)
- `idx_notifications_created_at` on created_at DESC (for timeline ordering)

**Notes**:
- sender_id can be NULL for system-generated notifications
- action_url for deep linking (e.g., `/organizations/{id}/workspaces/{id}/tasks/{id}`)
- Implement cleanup job to delete read notifications older than 30 days
- Notifications sent via email/websocket/push notification
- organization_id added for organization-level filtering

---

### 33. watchers

**Owner:** `task-service` (not collab). Listed here because it drives notification preferences.

**Purpose**: Users watching specific entities for updates.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique watch identifier |
| user_id | UUID | FOREIGN KEY (users.id), NOT NULL, ON DELETE CASCADE | User watching |
| entity_type | VARCHAR(20) | NOT NULL, CHECK (entity_type IN ('TASK', 'PROJECT')) | Watched entity type |
| entity_id | UUID | NOT NULL | Watched entity ID |
| notification_level | VARCHAR(20) | NOT NULL, DEFAULT 'ALL', CHECK (notification_level IN ('ALL', 'MENTIONS_ONLY', 'NONE')) | Notification preference |
| watched_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | When user started watching |

**Unique Constraint**: (user_id, entity_type, entity_id)

**Indexes**:
- `idx_watchers_user_id` on user_id (for user's watched entities)
- `idx_watchers_entity` on (entity_type, entity_id) (for entity's watchers)

**Notification Levels**:
- **ALL**: Notify on all changes
- **MENTIONS_ONLY**: Only notify when mentioned
- **NONE**: No notifications (watching for reference only)

**Notes**:
- Watchers receive notifications based on notification_level
- Can watch tasks or projects
- Self-subscription model (users opt-in to watch)

---

### 34. invitations

**Purpose**: Organization, workspace, and project invitations.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Unique invitation identifier |
| email | VARCHAR(255) | NOT NULL | Invitee's email |
| organization_id | UUID | FOREIGN KEY (organizations.id), NOT NULL, ON DELETE CASCADE | Target organization |
| workspace_id | UUID | FOREIGN KEY (workspaces.id), NULL, ON DELETE SET NULL | Target workspace (optional) |
| project_id | UUID | FOREIGN KEY (projects.id), NULL, ON DELETE SET NULL | Target project (optional) |
| invited_by | UUID | FOREIGN KEY (users.id), NOT NULL | User who sent invitation |
| invitation_type | VARCHAR(20) | NOT NULL, DEFAULT 'ORGANIZATION', CHECK (invitation_type IN ('ORGANIZATION', 'WORKSPACE', 'PROJECT')) | Invitation type |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING', CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED')) | Invitation status |
| token | VARCHAR(255) | UNIQUE, NOT NULL | Unique invitation token |
| expires_at | TIMESTAMPTZ | NOT NULL | Expiration timestamp |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

**Indexes**:
- `idx_invitations_email` on email (for user's invitations)
- `idx_invitations_organization_id` on organization_id (for organization invitations)
- `idx_invitations_workspace_id` on workspace_id (for workspace invitations)
- `idx_invitations_token` on token (for invitation lookup)
- `idx_invitations_status` on status (for filtering by status)

**Notes**:
- Token used for invitation link (e.g., `/invite/{token}`)
- expires_at typically 7 days from creation
- Expired invitations marked EXPIRED by cleanup job
- Same email can have multiple pending invitations to different organizations
- organization_id now required (all invitations belong to an organization)

---

## Entity Relationships

### Primary Relationships

```
organizations (1) ----< (N) organization_members
organizations (1) ----< (N) workspaces
organizations (1) ----< (N) tasks
organizations (1) ----< (N) sprints
organizations (1) ----< (N) activity_feed
organizations (1) ----< (N) notifications
organizations (1) ----< (N) invitations

users (1) ----< (N) refresh_tokens
users (1) ----< (N) organization_members
users (1) ----< (N) workspace_members
users (1) ----< (N) project_members
users (1) ----< (N) organizations (as owner)
users (1) ----< (N) workspaces (as owner)
users (1) ----< (N) projects (as owner)
users (1) ----< (N) tasks (as assignee)
users (1) ----< (N) tasks (as reporter)
users (1) ----< (N) comments (as author)
users (1) ----< (N) activity_feed
users (1) ----< (N) notifications (as recipient)
users (1) ----< (N) watchers
users (1) ----< (N) invitations (as invited_by)

roles (1) ----< (N) organization_members
roles (1) ----< (N) workspace_members
roles (1) ----< (N) project_members
roles (1) ----< (N) role_permissions
roles (1) ----< (N) document_permissions

permissions (1) ----< (N) role_permissions

workspaces (1) ----< (N) workspace_members
workspaces (1) ----< (N) projects
workspaces (1) ----< (N) tasks
workspaces (1) ----< (N) sprints
workspaces (1) ----< (N) document_folders
workspaces (1) ----< (N) tags
workspaces (1) ----< (N) activity_feed
workspaces (1) ----< (N) invitations

workspace_members (1) ----< (N) project_members (removed - now direct user link)

projects (1) ----< (N) project_members
projects (1) ----< (N) tasks
projects (1) ----< (N) sprints
projects (1) ----< (N) document_folders
projects (1) ----< (N) activity_feed

tasks (1) ----< (N) task_custom_field_values
tasks (1) ----< (N) task_dependencies (as dependent)
tasks (1) ----< (N) task_dependencies (as dependency)
tasks (1) ----< (N) task_tags
tasks (1) ----< (N) comments
tasks (1) ----< (N) activity_feed
tasks (1) ----< (N) watchers

statuses (1) ----< (N) task_types (as default)
statuses (1) ----< (N) tasks

task_types (1) ----< (N) task_type_custom_fields
task_types (1) ----< (N) tasks

custom_fields (1) ----< (N) custom_field_options
custom_fields (1) ----< (N) task_type_custom_fields
custom_fields (1) ----< (N) task_custom_field_values

document_folders (1) ----< (N) document_folders (as parent)
document_folders (1) ----< (N) documents

documents (1) ----< (N) document_permissions
documents (1) ----< (N) document_tags
documents (1) ----< (N) comment_attachments
documents (1) ----< (N) documents (as parent - version history)

tags (1) ----< (N) document_tags
tags (1) ----< (N) task_tags

comments (1) ----< (N) comment_attachments
comments (1) ----< (N) comment_reactions
comments (1) ----< (N) comment_mentions
comments (1) ----< (N) comments (as parent - replies)

status_workflows (1) ----< (N) workflow_statuses
status_workflows (1) ----< (N) workflow_transitions
status_workflows (1) ----< (N) workspaces (as default)
status_workflows (1) ----< (N) projects (as assigned workflow)

workflow_statuses (1) ----< (N) workflow_transitions (as from)
workflow_statuses (1) ----< (N) workflow_transitions (as to)
workflow_statuses (1) ----< (N) task_types (as default)
workflow_statuses (1) ----< (N) tasks

sprints (1) ----< (N) tasks
```

### Self-Referencing Relationships

- **projects**: parent_project_id → projects.id (sub-projects)
- **tasks**: parent_task_id → tasks.id (sub-tasks)
- **document_folders**: parent_folder_id → document_folders.id (nested folders)
- **documents**: parent_document_id → documents.id (version history)
- **comments**: parent_comment_id → comments.id (threaded replies)
- **task_dependencies**: task_id → tasks.id, depends_on_task_id → tasks.id

### Hierarchy

```
Organization
  └── Workspace
       └── Project
            └── Task
                 └── Sub-task
```

---

## Indexes

### Index Strategy

Indexes are created to optimize:
1. **Foreign Key Lookups**: All foreign keys have indexes
2. **Unique Constraints**: All unique constraints have backing indexes
3. **Common Queries**: Frequently queried columns (status, created_at, etc.)
4. **Search & Filter**: Columns used in WHERE clauses
5. **Sorting**: Columns used in ORDER BY (e.g., created_at DESC)

### Index Maintenance

- **Regular Reindex**: Reindex on large tables monthly
- **Monitor**: Monitor slow queries and add indexes as needed
- **Remove Unused**: Remove unused indexes to save space
- **Composite Indexes**: Consider composite indexes for multi-column queries

### Index Statistics

Total indexes: ~55 indexes across 34 tables
Index size estimate: ~10-20% of table size (typical for PostgreSQL)

---

## Data Types & Constraints

### Data Types Used

| Type | Usage | Notes |
|------|-------|-------|
| UUID | Primary keys, foreign keys | Uses gen_random_uuid() for generation |
| VARCHAR(n) | Variable-length strings | n = 255 for names, 500 for URLs, 20 for codes |
| TEXT | Long text content | No length limit, used for descriptions |
| INTEGER | Whole numbers | Used for counts, order, IDs |
| BIGINT | Large integers | Used for file sizes |
| DECIMAL(10, 2) | Decimal numbers | Used for hours (10 digits, 2 decimal places) |
| BOOLEAN | True/false values | Default false for most flags |
| TIMESTAMPTZ | Timestamps with timezone | Always use with timezone, not TIMESTAMP |
| JSONB | JSON data | Used for flexible metadata, changes |

### Constraints

1. **PRIMARY KEY**: Each table has UUID primary key
2. **FOREIGN KEY**: All relationships enforced at database level
3. **UNIQUE**: Email, invite codes, tokens, combinations
4. **NOT NULL**: Required fields enforced
5. **CHECK**: Enum values validated (role, status, priority, etc.)
6. **DEFAULT**: Sensible defaults for timestamps, booleans, counts

### Cascade Rules

- **ON DELETE CASCADE**: Child records deleted when parent deleted
- **ON DELETE SET NULL**: Child records set to NULL when parent deleted
- **ON DELETE RESTRICT**: Prevent deletion if child records exist (not used)

---

## Seed Data

### Default Status Workflow

```sql
-- Create default workflow
INSERT INTO status_workflows (id, name, description, is_default, created_at)
VALUES (
    gen_random_uuid(),
    'Default Workflow',
    'Standard Kanban workflow',
    true,
    CURRENT_TIMESTAMP
);

-- Create statuses
INSERT INTO workflow_statuses (id, workflow_id, name, display_order, color, category)
VALUES
    (gen_random_uuid(), (SELECT id FROM status_workflows WHERE is_default = true), 'Backlog', 1, '#6B7280', 'TODO'),
    (gen_random_uuid(), (SELECT id FROM status_workflows WHERE is_default = true), 'To Do', 2, '#3B82F6', 'TODO'),
    (gen_random_uuid(), (SELECT id FROM status_workflows WHERE is_default = true), 'In Progress', 3, '#F59E0B', 'IN_PROGRESS'),
    (gen_random_uuid(), (SELECT id FROM status_workflows WHERE is_default = true), 'In Review', 4, '#8B5CF6', 'IN_PROGRESS'),
    (gen_random_uuid(), (SELECT id FROM status_workflows WHERE is_default = true), 'Done', 5, '#10B981', 'DONE');

-- Create transitions (all forward transitions allowed)
-- This would be populated with all valid status transitions
```

### System Roles

```sql
-- Organization Roles
INSERT INTO roles (id, name, description, scope, is_system, created_at)
VALUES
    (gen_random_uuid(), 'Organization Owner', 'Full control over organization', 'ORGANIZATION', true, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Organization Admin', 'Administrative access to organization', 'ORGANIZATION', true, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Organization Member', 'Standard organization member', 'ORGANIZATION', true, CURRENT_TIMESTAMP);

-- Workspace Roles
INSERT INTO roles (id, name, description, scope, is_system, created_at)
VALUES
    (gen_random_uuid(), 'Workspace Admin', 'Full control over workspace', 'WORKSPACE', true, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Workspace Member', 'Standard workspace member', 'WORKSPACE', true, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Workspace Viewer', 'Read-only access to workspace', 'WORKSPACE', true, CURRENT_TIMESTAMP);

-- Project Roles
INSERT INTO roles (id, name, description, scope, is_system, created_at)
VALUES
    (gen_random_uuid(), 'Project Admin', 'Full control over project', 'PROJECT', true, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Project Member', 'Standard project member', 'PROJECT', true, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Project Viewer', 'Read-only access to project', 'PROJECT', true, CURRENT_TIMESTAMP);
```

### System Permissions

```sql
-- Organization Permissions
INSERT INTO permissions (id, name, description, category, resource, action, created_at)
VALUES
    (gen_random_uuid(), 'organization.create', 'Create organization', 'organization', 'organization', 'create', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'organization.read', 'View organization', 'organization', 'organization', 'read', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'organization.update', 'Update organization', 'organization', 'organization', 'update', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'organization.delete', 'Delete organization', 'organization', 'organization', 'delete', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'organization.manage_members', 'Manage organization members', 'organization', 'organization', 'manage_members', CURRENT_TIMESTAMP);

-- Workspace Permissions
INSERT INTO permissions (id, name, description, category, resource, action, created_at)
VALUES
    (gen_random_uuid(), 'workspace.create', 'Create workspace', 'workspace', 'workspace', 'create', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'workspace.read', 'View workspace', 'workspace', 'workspace', 'read', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'workspace.update', 'Update workspace', 'workspace', 'workspace', 'update', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'workspace.delete', 'Delete workspace', 'workspace', 'workspace', 'delete', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'workspace.manage_members', 'Manage workspace members', 'workspace', 'workspace', 'manage_members', CURRENT_TIMESTAMP);

-- Project Permissions
INSERT INTO permissions (id, name, description, category, resource, action, created_at)
VALUES
    (gen_random_uuid(), 'project.create', 'Create project', 'project', 'project', 'create', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'project.read', 'View project', 'project', 'project', 'read', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'project.update', 'Update project', 'project', 'project', 'update', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'project.delete', 'Delete project', 'project', 'project', 'delete', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'project.manage_members', 'Manage project members', 'project', 'project', 'manage_members', CURRENT_TIMESTAMP);

-- Task Permissions
INSERT INTO permissions (id, name, description, category, resource, action, created_at)
VALUES
    (gen_random_uuid(), 'task.create', 'Create task', 'task', 'task', 'create', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'task.read', 'View task', 'task', 'task', 'read', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'task.update', 'Update task', 'task', 'task', 'update', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'task.delete', 'Delete task', 'task', 'task', 'delete', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'task.assign', 'Assign task', 'task', 'task', 'assign', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'task.move', 'Move task between statuses', 'task', 'task', 'move', CURRENT_TIMESTAMP);

-- Sprint Permissions
INSERT INTO permissions (id, name, description, category, resource, action, created_at)
VALUES
    (gen_random_uuid(), 'sprint.create', 'Create sprint', 'sprint', 'sprint', 'create', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'sprint.read', 'View sprint', 'sprint', 'sprint', 'read', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'sprint.update', 'Update sprint', 'sprint', 'sprint', 'update', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'sprint.delete', 'Delete sprint', 'sprint', 'sprint', 'delete', CURRENT_TIMESTAMP);

-- Document Permissions
INSERT INTO permissions (id, name, description, category, resource, action, created_at)
VALUES
    (gen_random_uuid(), 'document.create', 'Upload document', 'document', 'document', 'create', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'document.read', 'View document', 'document', 'document', 'read', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'document.update', 'Update document', 'document', 'document', 'update', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'document.delete', 'Delete document', 'document', 'document', 'delete', CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'document.share', 'Share document', 'document', 'document', 'share', CURRENT_TIMESTAMP);
```

### Role Permissions Mapping

```sql
-- Organization Owner has all organization permissions
-- Organization Admin has all organization permissions except delete
-- Organization Member has read permissions only

-- Workspace Admin has all workspace permissions
-- Workspace Member has create, read, update permissions
-- Workspace Viewer has read permissions only

-- Project Admin has all project permissions
-- Project Member has create, read, update permissions
-- Project Viewer has read permissions only
```

### System Task Types

```sql
INSERT INTO task_types (id, name, description, icon, color, workspace_id, is_system, default_status_id, created_at)
VALUES
    (gen_random_uuid(), 'Task', 'Standard task', '📋', '#3B82F6', NULL, true, (SELECT id FROM workflow_statuses WHERE name = 'To Do'), CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Bug', 'Bug report', '🐛', '#EF4444', NULL, true, (SELECT id FROM workflow_statuses WHERE name = 'To Do'), CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Story', 'User story', '📖', '#8B5CF6', NULL, true, (SELECT id FROM workflow_statuses WHERE name = 'To Do'), CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Epic', 'Large feature/epic', '🎯', '#F59E0B', NULL, true, (SELECT id FROM workflow_statuses WHERE name = 'Backlog'), CURRENT_TIMESTAMP);
```

---

## Permission System

### How Permissions Work

1. **User has Role**: User is assigned a role at organization/workspace/project level
2. **Role has Permissions**: Role is linked to permissions via role_permissions table
3. **Permission Check**: Application checks if user's role has required permission
4. **Hierarchy**: Organization permissions > Workspace permissions > Project permissions

### Permission Check Example

```java
// Check if user can create workspace
public boolean canCreateWorkspace(UUID userId, UUID organizationId) {
    // Get user's role in organization
    OrganizationMember member = organizationMemberRepository
        .findByUserIdAndOrganizationId(userId, organizationId);
    
    // Get role's permissions
    List<Permission> permissions = rolePermissionRepository
        .findPermissionsByRoleId(member.getRoleId());
    
    // Check if workspace.create permission exists
    return permissions.stream()
        .anyMatch(p -> p.getName().equals("workspace.create"));
}
```

### Permission Inheritance

- **Organization Admin**: Automatically has all workspace and project permissions
- **Workspace Admin**: Automatically has all project permissions within workspace
- **Project Admin**: Full control over project only

This is implemented in application layer, not database level.

---

## Performance Considerations

### Query Optimization

1. **Use EXPLAIN ANALYZE**: Analyze slow queries
2. **N+1 Problem**: Use JOINs or batch loading
3. **Pagination**: Always use LIMIT/OFFSET for large lists
4. **Selective Queries**: Filter by indexed columns
5. **Avoid SELECT ***: Only select needed columns

### Caching Strategy

1. **Redis Cache**: Cache frequently accessed data (organizations, workspaces, roles, permissions)
2. **Query Cache**: Enable PostgreSQL query cache
3. **Application Cache**: Cache user permissions, statuses
4. **CDN**: Cache static assets (avatars, images)

### Backup Strategy

1. **Daily Backups**: pg_dump daily at midnight
2. **Point-in-Time Recovery**: WAL archiving enabled
3. **Replication**: Hot standby for read queries
4. **Off-site Backup**: Backup stored in cloud storage

---

## Security Considerations

### Data Protection

1. **Password Hashing**: Use bcrypt with minimum 10 rounds
2. **Sensitive Data**: Encrypt at rest for sensitive fields
3. **Row-Level Security**: Implement RLS for multi-tenant isolation
4. **Audit Logging**: Log all data access and modifications

### Access Control

1. **Principle of Least Privilege**: Database user has minimum required permissions
2. **Connection Pooling**: Use connection pool with proper credentials
3. **SSL/TLS**: Encrypted database connections
4. **IP Whitelist**: Restrict database access by IP

---

## Migration Strategy

### Version Control

Use Flyway or Liquibase for database migrations:

1. **Versioned Migrations**: Each change in separate migration file
2. **Repeatable Migrations**: Idempotent scripts for views/functions
3. **Rollback**: Provide rollback scripts for each migration
4. **Testing**: Test migrations on staging before production

### Migration Example

```sql
-- V1__Create_users_table.sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- V2__Add_phone_to_users.sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20);

-- V3__Add_organizations.sql
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
```

---

## Monitoring & Maintenance

### Health Checks

1. **Connection Pool**: Monitor pool usage
2. **Slow Queries**: Log queries > 1 second
3. **Table Bloat**: Monitor and vacuum tables
4. **Index Usage**: Monitor unused indexes

### Maintenance Tasks

1. **VACUUM ANALYZE**: Weekly table maintenance
2. **REINDEX**: Monthly index rebuild
3. **Statistics Update**: Weekly statistics refresh
4. **Archive Data**: Monthly archive old activity feed

---

## API-Database Mapping

### Common Operations

| Operation | Tables Involved | Notes |
|-----------|----------------|-------|
| User Login | users, refresh_tokens | Validate credentials, issue tokens |
| Create Organization | organizations, organization_members | Create org, add owner as org owner |
| Join Organization | organization_members, organizations | Validate invite, add member |
| Create Workspace | workspaces, workspace_members | Create workspace, add owner as workspace admin |
| Join Workspace | workspace_members, workspaces | Validate invite code, add member |
| Create Project | projects, project_members | Create project, add owner as project admin |
| Create Task | tasks, task_custom_field_values | Create task, save custom fields |
| Assign Task | tasks, notifications | Update assignee, send notification |
| Add Comment | comments, comment_mentions | Create comment, parse mentions |
| Upload Document | documents, document_permissions | Save file, set permissions |
| Start Sprint | sprints, tasks | Create sprint, assign tasks |
| Check Permission | roles, role_permissions, permissions | Verify user has required permission |

---

## Appendix

### Common SQL Queries

#### Get User's Organizations
```sql
SELECT o.*
FROM organizations o
JOIN organization_members om ON o.id = om.organization_id
WHERE om.user_id = :userId AND o.is_active = true;
```

#### Get Organization Workspaces
```sql
SELECT w.*
FROM workspaces w
WHERE w.organization_id = :organizationId AND w.is_active = true;
```

#### Get Project Tasks with Assignee
```sql
SELECT t.*, u.name as assignee_name
FROM tasks t
LEFT JOIN users u ON t.assignee_id = u.id
WHERE t.project_id = :projectId
ORDER BY t.position;
```

#### Check User Permission
```sql
SELECT p.*
FROM permissions p
JOIN role_permissions rp ON p.id = rp.permission_id
JOIN roles r ON rp.role_id = r.id
JOIN workspace_members wm ON r.id = wm.role_id
WHERE wm.user_id = :userId
  AND wm.workspace_id = :workspaceId
  AND p.name = :permissionName;
```

#### Get Task Activity Feed
```sql
SELECT af.*
FROM activity_feed af
WHERE af.entity_type = 'TASK' AND af.entity_id = :taskId
ORDER BY af.created_at DESC
LIMIT 50;
```

#### Get Unread Notifications
```sql
SELECT n.*
FROM notifications n
WHERE n.recipient_id = :userId AND n.is_read = false
ORDER BY n.created_at DESC;
```

### Database Size Estimates

- **Small (< 100 users)**: ~150 MB
- **Medium (100-1000 users)**: ~2-10 GB
- **Large (1000-10000 users)**: ~20-100 GB
- **Enterprise (> 10000 users)**: ~200+ GB

---

**Document Version**: 2.5.0  
**Last Updated**: 2026-10-23  

**Changes from v2.4.0**:
- Migrated `sprints` (task-service Flyway V2); `tasks.sprint_id` now FK → `sprints.id` ON DELETE SET NULL
- Seeded `sprint.create|read|update|delete` into project Admin/Member/Viewer roles

**Changes from v2.3.0**:
- Split **Migrated (shipped)** vs **design-only** tables to match Flyway + collab AutoMigrate
- Noted `processed_events`; `sprint_id` reserved without `sprints` table
- MVP marked complete (Weeks 1–14)

**Changes from v2.2.0**:
- Aligned with slim MVP ([services-mvp.md](services-mvp.md)): table ownership by auth / platform / task / collab
- Header: shared DB, JPA + GORM; removed NextAuth as schema requirement
- Document tables marked Phase 2 / deferred for MVP migrations
- `roles`/`permissions` owned by platform-service (not a separate workflow service)

**Changes from v2.1.0**:
- Updated status_workflows to support per-project workflows
- Added workspace_id to status_workflows (NULL = global template, set = workspace-specific)
- Added is_template flag to status_workflows for template workflows
- Added default_workflow_id to workspaces table
- Added workflow_id to projects table (NULL = use workspace default)
- Updated entity relationships to include project-workflow relationship
- Projects can now have unique workflows or share with other projects

**Changes from v2.0.0**:
- Re-added status_workflows, workflow_statuses, workflow_transitions tables
- Changed single statuses table back to workflow-based system
- Updated task_types.default_status_id to reference workflow_statuses
- Updated task.status_id to reference workflow_statuses

**Changes from v1.0.0**:
- Added organizations table and hierarchy
- Replaced permission flags with proper role-based permission system
- Added roles, permissions, and role_permissions tables
- Updated workspace_members and project_members to use role_id
- Added organization_id to tasks, sprints, activity_feed, notifications, invitations
- Updated assignee_id in tasks to reference users directly
