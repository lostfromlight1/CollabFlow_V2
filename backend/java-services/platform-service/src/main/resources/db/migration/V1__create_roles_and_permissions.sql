-- Platform RBAC (docs/database.md) — owned by platform-service
-- Uses flyway_platform_history (separate from auth flyway_schema_history)

CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    scope       VARCHAR(20) NOT NULL,
    is_system   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_roles_scope CHECK (scope IN ('ORGANIZATION', 'WORKSPACE', 'PROJECT'))
);

CREATE INDEX idx_roles_scope ON roles (scope);
CREATE INDEX idx_roles_is_system ON roles (is_system);

CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    category    VARCHAR(50) NOT NULL,
    resource    VARCHAR(50) NOT NULL,
    action      VARCHAR(50) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_permissions_name UNIQUE (name)
);

CREATE INDEX idx_permissions_category ON permissions (category);
CREATE INDEX idx_permissions_resource ON permissions (resource);

CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Fixed UUIDs so API clients / seeds stay stable locally
INSERT INTO roles (id, name, description, scope, is_system) VALUES
    ('a0000000-0000-4000-8000-000000000001', 'Organization Owner', 'Full control over organization', 'ORGANIZATION', TRUE),
    ('a0000000-0000-4000-8000-000000000002', 'Organization Admin', 'Administrative access to organization', 'ORGANIZATION', TRUE),
    ('a0000000-0000-4000-8000-000000000003', 'Organization Member', 'Standard organization member', 'ORGANIZATION', TRUE);

INSERT INTO permissions (id, name, description, category, resource, action) VALUES
    ('b0000000-0000-4000-8000-000000000001', 'organization.create', 'Create organization', 'organization', 'organization', 'create'),
    ('b0000000-0000-4000-8000-000000000002', 'organization.read', 'View organization', 'organization', 'organization', 'read'),
    ('b0000000-0000-4000-8000-000000000003', 'organization.update', 'Update organization', 'organization', 'organization', 'update'),
    ('b0000000-0000-4000-8000-000000000004', 'organization.delete', 'Delete organization', 'organization', 'organization', 'delete'),
    ('b0000000-0000-4000-8000-000000000005', 'organization.manage_members', 'Manage organization members', 'organization', 'organization', 'manage_members'),
    ('b0000000-0000-4000-8000-000000000006', 'workspace.create', 'Create workspace', 'workspace', 'workspace', 'create');

-- Owner: all org + workspace.create
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-4000-8000-000000000001', id FROM permissions
WHERE name IN (
    'organization.create', 'organization.read', 'organization.update', 'organization.delete',
    'organization.manage_members', 'workspace.create'
);

-- Admin: all except organization.delete
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'a0000000-0000-4000-8000-000000000002', id FROM permissions
WHERE name IN (
    'organization.create', 'organization.read', 'organization.update',
    'organization.manage_members', 'workspace.create'
);

-- Member: read only
INSERT INTO role_permissions (role_id, permission_id)
VALUES ('a0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000002');
