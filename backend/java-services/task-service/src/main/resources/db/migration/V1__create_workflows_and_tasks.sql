-- Workflows + tasks (docs/database.md). sprint_id nullable, no FK until sprints.
-- task.* permissions seeded into shared roles tables (platform-owned).

INSERT INTO permissions (id, name, description, category, resource, action) VALUES
    ('b0000000-0000-4000-8000-000000000016', 'task.create', 'Create task', 'task', 'task', 'create'),
    ('b0000000-0000-4000-8000-000000000017', 'task.read', 'View task', 'task', 'task', 'read'),
    ('b0000000-0000-4000-8000-000000000018', 'task.update', 'Update task', 'task', 'task', 'update'),
    ('b0000000-0000-4000-8000-000000000019', 'task.delete', 'Delete task', 'task', 'task', 'delete'),
    ('b0000000-0000-4000-8000-000000000020', 'task.assign', 'Assign task', 'task', 'task', 'assign'),
    ('b0000000-0000-4000-8000-000000000021', 'task.move', 'Move task between statuses', 'task', 'task', 'move');

-- Project Admin: all task.*
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-4000-8000-000000000007', id FROM permissions
WHERE name IN (
    'task.create', 'task.read', 'task.update', 'task.delete', 'task.assign', 'task.move'
);

-- Project Member: create/read/update/assign/move (no delete)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-4000-8000-000000000008', id FROM permissions
WHERE name IN (
    'task.create', 'task.read', 'task.update', 'task.assign', 'task.move'
);

-- Project Viewer: read
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('a0000000-0000-4000-8000-000000000009', 'b0000000-0000-4000-8000-000000000017');

CREATE TABLE status_workflows (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    workspace_id  UUID REFERENCES workspaces (id) ON DELETE CASCADE,
    is_default    BOOLEAN NOT NULL DEFAULT FALSE,
    is_template   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_status_workflows_workspace_id ON status_workflows (workspace_id);
CREATE INDEX idx_status_workflows_is_default ON status_workflows (is_default);

CREATE TABLE workflow_statuses (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id    UUID NOT NULL REFERENCES status_workflows (id) ON DELETE CASCADE,
    name           VARCHAR(100) NOT NULL,
    display_order  INTEGER NOT NULL,
    color          VARCHAR(7) NOT NULL DEFAULT '#6B7280',
    category       VARCHAR(20) NOT NULL,
    CONSTRAINT chk_workflow_statuses_category CHECK (category IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT uq_workflow_statuses_order UNIQUE (workflow_id, display_order)
);

CREATE INDEX idx_workflow_statuses_workflow_id ON workflow_statuses (workflow_id);

CREATE TABLE workflow_transitions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id       UUID NOT NULL REFERENCES status_workflows (id) ON DELETE CASCADE,
    from_status_id    UUID NOT NULL REFERENCES workflow_statuses (id) ON DELETE CASCADE,
    to_status_id      UUID NOT NULL REFERENCES workflow_statuses (id) ON DELETE CASCADE,
    require_comment   BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_workflow_transitions UNIQUE (workflow_id, from_status_id, to_status_id),
    CONSTRAINT chk_workflow_transitions_no_self CHECK (from_status_id <> to_status_id)
);

CREATE INDEX idx_workflow_transitions_workflow_id ON workflow_transitions (workflow_id);

CREATE TABLE task_types (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(100) NOT NULL,
    description        TEXT,
    icon               VARCHAR(50),
    color              VARCHAR(7) NOT NULL DEFAULT '#6B7280',
    workspace_id       UUID REFERENCES workspaces (id) ON DELETE CASCADE,
    is_system          BOOLEAN NOT NULL DEFAULT FALSE,
    default_status_id  UUID REFERENCES workflow_statuses (id) ON DELETE SET NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_task_types_workspace_id ON task_types (workspace_id);

-- Fixed UUIDs for local Postman / seeds
INSERT INTO status_workflows (id, name, description, workspace_id, is_default, is_template)
VALUES (
    'c0000000-0000-4000-8000-000000000001',
    'Default Workflow',
    'Standard Kanban workflow',
    NULL,
    TRUE,
    TRUE
);

INSERT INTO workflow_statuses (id, workflow_id, name, display_order, color, category) VALUES
    ('c0000000-0000-4000-8000-000000000011', 'c0000000-0000-4000-8000-000000000001', 'Backlog', 1, '#6B7280', 'TODO'),
    ('c0000000-0000-4000-8000-000000000012', 'c0000000-0000-4000-8000-000000000001', 'To Do', 2, '#3B82F6', 'TODO'),
    ('c0000000-0000-4000-8000-000000000013', 'c0000000-0000-4000-8000-000000000001', 'In Progress', 3, '#F59E0B', 'IN_PROGRESS'),
    ('c0000000-0000-4000-8000-000000000014', 'c0000000-0000-4000-8000-000000000001', 'In Review', 4, '#8B5CF6', 'IN_PROGRESS'),
    ('c0000000-0000-4000-8000-000000000015', 'c0000000-0000-4000-8000-000000000001', 'Done', 5, '#10B981', 'DONE');

-- Forward-only transitions
INSERT INTO workflow_transitions (id, workflow_id, from_status_id, to_status_id, require_comment) VALUES
    ('c0000000-0000-4000-8000-000000000021', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000011', 'c0000000-0000-4000-8000-000000000012', FALSE),
    ('c0000000-0000-4000-8000-000000000022', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000011', 'c0000000-0000-4000-8000-000000000013', FALSE),
    ('c0000000-0000-4000-8000-000000000023', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000011', 'c0000000-0000-4000-8000-000000000014', FALSE),
    ('c0000000-0000-4000-8000-000000000024', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000011', 'c0000000-0000-4000-8000-000000000015', FALSE),
    ('c0000000-0000-4000-8000-000000000025', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000012', 'c0000000-0000-4000-8000-000000000013', FALSE),
    ('c0000000-0000-4000-8000-000000000026', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000012', 'c0000000-0000-4000-8000-000000000014', FALSE),
    ('c0000000-0000-4000-8000-000000000027', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000012', 'c0000000-0000-4000-8000-000000000015', FALSE),
    ('c0000000-0000-4000-8000-000000000028', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000013', 'c0000000-0000-4000-8000-000000000014', FALSE),
    ('c0000000-0000-4000-8000-000000000029', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000013', 'c0000000-0000-4000-8000-000000000015', FALSE),
    ('c0000000-0000-4000-8000-00000000002a', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000014', 'c0000000-0000-4000-8000-000000000015', FALSE);

INSERT INTO task_types (id, name, description, icon, color, workspace_id, is_system, default_status_id) VALUES
    ('d0000000-0000-4000-8000-000000000001', 'Task', 'Standard task', 'TASK', '#3B82F6', NULL, TRUE, 'c0000000-0000-4000-8000-000000000012'),
    ('d0000000-0000-4000-8000-000000000002', 'Bug', 'Bug report', 'BUG', '#EF4444', NULL, TRUE, 'c0000000-0000-4000-8000-000000000012'),
    ('d0000000-0000-4000-8000-000000000003', 'Story', 'User story', 'STORY', '#8B5CF6', NULL, TRUE, 'c0000000-0000-4000-8000-000000000012'),
    ('d0000000-0000-4000-8000-000000000004', 'Epic', 'Large feature/epic', 'EPIC', '#F59E0B', NULL, TRUE, 'c0000000-0000-4000-8000-000000000011');

CREATE TABLE tasks (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    task_type_id       UUID NOT NULL REFERENCES task_types (id),
    project_id         UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    workspace_id       UUID NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    organization_id    UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    parent_task_id     UUID REFERENCES tasks (id) ON DELETE SET NULL,
    status_id          UUID NOT NULL REFERENCES workflow_statuses (id),
    assignee_id        UUID REFERENCES users (id) ON DELETE SET NULL,
    reporter_id        UUID NOT NULL REFERENCES users (id),
    priority           VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    position           INTEGER NOT NULL DEFAULT 0,
    start_date         TIMESTAMPTZ,
    due_date           TIMESTAMPTZ,
    estimated_hours    NUMERIC(10, 2),
    actual_hours       NUMERIC(10, 2),
    story_points       INTEGER,
    sprint_id          UUID,
    attachments_count  INTEGER NOT NULL DEFAULT 0,
    comments_count     INTEGER NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at          TIMESTAMPTZ,
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_tasks_project_id ON tasks (project_id);
CREATE INDEX idx_tasks_workspace_id ON tasks (workspace_id);
CREATE INDEX idx_tasks_organization_id ON tasks (organization_id);
CREATE INDEX idx_tasks_parent_task_id ON tasks (parent_task_id);
CREATE INDEX idx_tasks_status_id ON tasks (status_id);
CREATE INDEX idx_tasks_assignee_id ON tasks (assignee_id);
CREATE INDEX idx_tasks_sprint_id ON tasks (sprint_id);
