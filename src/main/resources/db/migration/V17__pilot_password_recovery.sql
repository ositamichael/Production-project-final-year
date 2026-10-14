CREATE TABLE pilot_password_reset_tokens (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pilot_password_reset_active
    ON pilot_password_reset_tokens(account_id, expires_at, used_at);
