-- Master-admin lifecycle columns on the shared users table.
-- deleted_at set = soft-deleted (email stays unique until restore).
-- last_login_at stamped on successful login. Rollback: DROP the two columns and indexes.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at)
    WHERE deleted_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_last_login_at ON users (last_login_at);

CREATE INDEX IF NOT EXISTS idx_users_created_at ON users (created_at);
