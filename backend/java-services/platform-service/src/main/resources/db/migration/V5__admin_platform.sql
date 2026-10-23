-- Master-admin platform tables. Auth and user-service insert audit rows
-- into admin_audit_events (shared DB, no Flyway of their own).
-- Rollback: DROP TABLE feature_flags, announcements, admin_audit_events.

CREATE TABLE admin_audit_events (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id  UUID NOT NULL REFERENCES users (id),
    action         VARCHAR(64) NOT NULL,
    target_type    VARCHAR(32) NOT NULL,
    target_id      UUID,
    metadata       JSONB,
    ip_address     VARCHAR(64),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_audit_created_at ON admin_audit_events (created_at DESC);
CREATE INDEX idx_admin_audit_actor ON admin_audit_events (actor_user_id);
CREATE INDEX idx_admin_audit_target ON admin_audit_events (target_type, target_id);

CREATE TABLE announcements (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(200) NOT NULL,
    body        TEXT NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at   TIMESTAMPTZ,
    ends_at     TIMESTAMPTZ,
    created_by  UUID NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_announcements_active ON announcements (is_active, starts_at, ends_at);

CREATE TABLE feature_flags (
    key          VARCHAR(100) PRIMARY KEY,
    enabled      BOOLEAN NOT NULL DEFAULT FALSE,
    description  VARCHAR(500),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
