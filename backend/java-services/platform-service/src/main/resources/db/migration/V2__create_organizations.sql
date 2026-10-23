-- Organizations + members (docs/database.md)

CREATE TABLE organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    logo_url        VARCHAR(500),
    owner_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    slug            VARCHAR(100) NOT NULL,
    plan            VARCHAR(20) NOT NULL DEFAULT 'FREE',
    max_members     INTEGER DEFAULT 10,
    max_workspaces  INTEGER DEFAULT 5,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_organizations_slug UNIQUE (slug),
    CONSTRAINT chk_organizations_plan CHECK (plan IN ('FREE', 'PRO', 'ENTERPRISE'))
);

CREATE INDEX idx_organizations_owner_id ON organizations (owner_id);
CREATE INDEX idx_organizations_slug ON organizations (slug);
CREATE INDEX idx_organizations_is_active ON organizations (is_active);

CREATE TABLE organization_members (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id          UUID NOT NULL REFERENCES roles (id),
    joined_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_organization_members_org_user UNIQUE (organization_id, user_id)
);

CREATE INDEX idx_organization_members_organization_id ON organization_members (organization_id);
CREATE INDEX idx_organization_members_user_id ON organization_members (user_id);
CREATE INDEX idx_organization_members_role_id ON organization_members (role_id);
