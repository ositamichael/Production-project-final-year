CREATE TABLE pilot_media (
    media_key VARCHAR(64) PRIMARY KEY,
    media_kind VARCHAR(20) NOT NULL,
    media_type VARCHAR(80) NOT NULL,
    content BYTEA NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pilot_media_kind CHECK (media_kind IN ('PROFILE', 'LOST_FOUND'))
);
