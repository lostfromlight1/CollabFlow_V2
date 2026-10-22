-- Phase 2: master admin flag + one-time email/password tokens (auth-owned)

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_master_admin BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_users_is_master_admin ON users (is_master_admin)
    WHERE is_master_admin = TRUE;

CREATE TABLE email_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token       VARCHAR(128) NOT NULL,
    purpose     VARCHAR(32) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_email_tokens_token UNIQUE (token),
    CONSTRAINT chk_email_tokens_purpose CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD'))
);

CREATE INDEX idx_email_tokens_user_id ON email_tokens (user_id);
CREATE INDEX idx_email_tokens_purpose ON email_tokens (purpose);
