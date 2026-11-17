CREATE TABLE pilot_credentials (
    account_id UUID PRIMARY KEY REFERENCES user_accounts(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE location_shares (
    account_id UUID PRIMARY KEY REFERENCES user_accounts(id) ON DELETE CASCADE,
    university_id UUID NOT NULL REFERENCES universities(id),
    latitude DECIMAL(8, 4) NOT NULL,
    longitude DECIMAL(9, 4) NOT NULL,
    accuracy_metres INTEGER NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_location_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_location_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_location_accuracy CHECK (accuracy_metres BETWEEN 0 AND 10000)
);

CREATE INDEX idx_location_shares_active ON location_shares(university_id, expires_at);
