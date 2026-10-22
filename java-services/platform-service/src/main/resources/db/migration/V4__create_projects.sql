-- PROJECT RBAC seeds + projects / project_members (docs/database.md)
-- workflow_id: nullable UUID, no FK until task-service Week 8

INSERT INTO roles (id, name, description, scope, is_system) VALUES
    ('a0000000-0000-4000-8000-000000000007', 'Project Admin', 'Full control over project', 'PROJECT', TRUE),
    ('a0000000-0000-4000-8000-000000000008', 'Project Member', 'Standard project member', 'PROJECT', TRUE),
    ('a0000000-0000-4000-8000-000000000009', 'Project Viewer', 'Read-only project access', 'PROJECT', TRUE);

INSERT INTO permissions (id, name, description, category, resource, action) VALUES
    ('b0000000-0000-4000-8000-000000000011', 'project.create', 'Create project', 'project', 'project', 'create'),
    ('b0000000-0000-4000-8000-000000000012', 'project.read', 'View project', 'project', 'project', 'read'),
    ('b0000000-0000-4000-8000-000000000013', 'project.update', 'Update project', 'project', 'project', 'update'),
    ('b0000000-0000-4000-8000-000000000014', 'project.delete', 'Delete/archive project', 'project', 'project', 'delete'),
    ('b0000000-0000-4000-8000-000000000015', 'project.manage_members', 'Manage project members', 'project', 'project', 'manage_members');

-- Workspace Admin may create projects (checked on workspace membership)
INSERT INTO role_permissions (role_id, permission_id)
VALUES ('a0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000011');

-- Project Admin: all project.* except create (create is workspace-scoped)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-4000-8000-000000000007', id FROM permissions
WHERE name IN (
    'project.read', 'project.update', 'project.delete', 'project.manage_members'
);

-- Member + Viewer: read
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('a0000000-0000-4000-8000-000000000008', 'b0000000-0000-4000-8000-000000000012'),
    ('a0000000-0000-4000-8000-000000000009', 'b0000000-0000-4000-8000-000000000012');

CREATE TABLE projects (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    image_url          VARCHAR(500),
    workspace_id       UUID NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    parent_project_id  UUID REFERENCES projects (id) ON DELETE SET NULL,
    owner_id           UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    project_type       VARCHAR(50) NOT NULL DEFAULT 'SOFTWARE',
    start_date         TIMESTAMPTZ,
    due_date           TIMESTAMPTZ,
    workflow_id        UUID,
    lead_id            UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_projects_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'ON_HOLD')),
    CONSTRAINT chk_projects_type CHECK (project_type IN ('SOFTWARE', 'MARKETING', 'HR', 'CUSTOM'))
);

CREATE INDEX idx_projects_workspace_id ON projects (workspace_id);
CREATE INDEX idx_projects_parent_project_id ON projects (parent_project_id);
CREATE INDEX idx_projects_owner_id ON projects (owner_id);
CREATE INDEX idx_projects_status ON projects (status);
CREATE INDEX idx_projects_workflow_id ON projects (workflow_id);

CREATE TABLE project_members (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id     UUID NOT NULL REFERENCES roles (id),
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_project_members_project_user UNIQUE (project_id, user_id)
);

CREATE INDEX idx_project_members_project_id ON project_members (project_id);
CREATE INDEX idx_project_members_user_id ON project_members (user_id);
CREATE INDEX idx_project_members_role_id ON project_members (role_id);
