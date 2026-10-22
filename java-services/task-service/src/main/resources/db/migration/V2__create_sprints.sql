-- Sprints table + sprint.* permissions (docs/database.md).
-- Adds FK from tasks.sprint_id → sprints.id (column already existed nullable).

INSERT INTO permissions (id, name, description, category, resource, action) VALUES
    ('b0000000-0000-4000-8000-000000000022', 'sprint.create', 'Create sprint', 'sprint', 'sprint', 'create'),
    ('b0000000-0000-4000-8000-000000000023', 'sprint.read', 'View sprint', 'sprint', 'sprint', 'read'),
    ('b0000000-0000-4000-8000-000000000024', 'sprint.update', 'Update sprint', 'sprint', 'sprint', 'update'),
    ('b0000000-0000-4000-8000-000000000025', 'sprint.delete', 'Delete sprint', 'sprint', 'sprint', 'delete');

-- Project Admin: all sprint.*
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-4000-8000-000000000007', id FROM permissions
WHERE name IN (
    'sprint.create', 'sprint.read', 'sprint.update', 'sprint.delete'
);

-- Project Member: create/read/update (no delete)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-4000-8000-000000000008', id FROM permissions
WHERE name IN (
    'sprint.create', 'sprint.read', 'sprint.update'
);

-- Project Viewer: read
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('a0000000-0000-4000-8000-000000000009', 'b0000000-0000-4000-8000-000000000023');

CREATE TABLE sprints (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    project_id         UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    workspace_id       UUID NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    organization_id    UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    sprint_number      INTEGER NOT NULL,
    start_date         TIMESTAMPTZ NOT NULL,
    end_date           TIMESTAMPTZ NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PLANNING',
    goal               TEXT,
    capacity           INTEGER,
    velocity           INTEGER,
    backlog_order      INTEGER NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sprints_status CHECK (status IN ('PLANNING', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_sprints_dates CHECK (end_date >= start_date),
    CONSTRAINT uq_sprints_project_number UNIQUE (project_id, sprint_number)
);

CREATE INDEX idx_sprints_project_id ON sprints (project_id);
CREATE INDEX idx_sprints_workspace_id ON sprints (workspace_id);
CREATE INDEX idx_sprints_organization_id ON sprints (organization_id);
CREATE INDEX idx_sprints_status ON sprints (status);

-- Wire reserved tasks.sprint_id to sprints (nulls remain valid)
ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_sprint_id
    FOREIGN KEY (sprint_id) REFERENCES sprints (id) ON DELETE SET NULL;
