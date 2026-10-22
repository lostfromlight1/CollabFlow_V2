-- Workspace RBAC seeds + workspaces / workspace_members (docs/database.md)
-- default_workflow_id: nullable UUID, no FK until task-service Week 8

INSERT INTO roles (id, name, description, scope, is_system) VALUES
    ('a0000000-0000-4000-8000-000000000004', 'Workspace Admin', 'Full control over workspace', 'WORKSPACE', TRUE),
    ('a0000000-0000-4000-8000-000000000005', 'Workspace Member', 'Standard workspace member', 'WORKSPACE', TRUE),
    ('a0000000-0000-4000-8000-000000000006', 'Workspace Viewer', 'Read-only workspace access', 'WORKSPACE', TRUE);

INSERT INTO permissions (id, name, description, category, resource, action) VALUES
    ('b0000000-0000-4000-8000-000000000007', 'workspace.read', 'View workspace', 'workspace', 'workspace', 'read'),
    ('b0000000-0000-4000-8000-000000000008', 'workspace.update', 'Update workspace', 'workspace', 'workspace', 'update'),
    ('b0000000-0000-4000-8000-000000000009', 'workspace.delete', 'Delete workspace', 'workspace', 'workspace', 'delete'),
    ('b0000000-0000-4000-8000-000000000010', 'workspace.manage_members', 'Manage workspace members', 'workspace', 'workspace', 'manage_members');

-- Workspace Admin: all workspace.* (except create — that stays org-scoped)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-4000-8000-000000000004', id FROM permissions
WHERE name IN (
    'workspace.read', 'workspace.update', 'workspace.delete', 'workspace.manage_members'
);

-- Member + Viewer: read only
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('a0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000007'),
    ('a0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000007');

CREATE TABLE workspaces (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(255) NOT NULL,
    description          TEXT,
    image_url            VARCHAR(500),
    organization_id      UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    owner_id             UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    invite_code          VARCHAR(20) NOT NULL,
    default_workflow_id  UUID,
    time_zone            VARCHAR(50) NOT NULL DEFAULT 'UTC',
    date_format          VARCHAR(20) NOT NULL DEFAULT 'YYYY-MM-DD',
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_workspaces_invite_code UNIQUE (invite_code)
);

CREATE INDEX idx_workspaces_organization_id ON workspaces (organization_id);
CREATE INDEX idx_workspaces_owner_id ON workspaces (owner_id);
CREATE INDEX idx_workspaces_invite_code ON workspaces (invite_code);
CREATE INDEX idx_workspaces_is_active ON workspaces (is_active);
CREATE INDEX idx_workspaces_default_workflow_id ON workspaces (default_workflow_id);

CREATE TABLE workspace_members (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id     UUID NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id          UUID NOT NULL REFERENCES roles (id),
    joined_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_workspace_members_ws_user UNIQUE (workspace_id, user_id)
);

CREATE INDEX idx_workspace_members_workspace_id ON workspace_members (workspace_id);
CREATE INDEX idx_workspace_members_user_id ON workspace_members (user_id);
CREATE INDEX idx_workspace_members_role_id ON workspace_members (role_id);
