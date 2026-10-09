CREATE TABLE lost_found_reports (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    reporter_id UUID NOT NULL REFERENCES user_accounts(id),
    report_type VARCHAR(20) NOT NULL,
    category VARCHAR(30) NOT NULL,
    item_name VARCHAR(160) NOT NULL,
    colour VARCHAR(80),
    public_description VARCHAR(800) NOT NULL,
    private_identifying_details VARCHAR(1000) NOT NULL,
    last_seen_location VARCHAR(180) NOT NULL,
    occurred_on DATE NOT NULL,
    custody_status VARCHAR(30) NOT NULL,
    report_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    moderation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    photo_storage_key VARCHAR(100),
    photo_media_type VARCHAR(40),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_lost_found_type CHECK (report_type IN ('LOST', 'FOUND')),
    CONSTRAINT ck_lost_found_category CHECK (
        category IN ('ELECTRONICS', 'KEYS', 'BAGS', 'CLOTHING', 'DOCUMENTS', 'ACCESSORIES', 'OTHER')
    ),
    CONSTRAINT ck_lost_found_custody CHECK (
        custody_status IN ('WITH_REPORTER', 'CAMPUS_SECURITY', 'SERVICE_DESK', 'NOT_APPLICABLE')
    ),
    CONSTRAINT ck_lost_found_status CHECK (report_status IN ('OPEN', 'MATCHED', 'RETURNED', 'WITHDRAWN')),
    CONSTRAINT ck_lost_found_moderation CHECK (moderation_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_lost_found_photo_pair CHECK (
        (photo_storage_key IS NULL AND photo_media_type IS NULL)
        OR (photo_storage_key IS NOT NULL AND photo_media_type IS NOT NULL)
    )
);

CREATE INDEX idx_lost_found_catalogue
    ON lost_found_reports(university_id, moderation_status, report_status, occurred_on DESC);

CREATE INDEX idx_lost_found_reporter
    ON lost_found_reports(reporter_id, created_at DESC);

CREATE TABLE lost_found_claims (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    report_id UUID NOT NULL REFERENCES lost_found_reports(id) ON DELETE CASCADE,
    claimant_id UUID NOT NULL REFERENCES user_accounts(id),
    private_ownership_evidence VARCHAR(1000) NOT NULL,
    claim_status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_lost_found_claim UNIQUE (report_id, claimant_id),
    CONSTRAINT ck_lost_found_claim_status CHECK (
        claim_status IN ('SUBMITTED', 'REVIEWING', 'APPROVED', 'REJECTED', 'WITHDRAWN')
    )
);

CREATE INDEX idx_lost_found_claimant
    ON lost_found_claims(claimant_id, created_at DESC);

ALTER TABLE notifications DROP CONSTRAINT ck_notification_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notification_type CHECK (
    notification_type IN (
        'CONNECTION_REQUEST', 'CONNECTION_ACCEPTED', 'MESSAGE', 'SAFETY',
        'EVENT', 'BOOKING', 'LOST_FOUND'
    )
);
