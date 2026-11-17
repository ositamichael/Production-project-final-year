ALTER TABLE lost_found_reports ADD COLUMN review_note VARCHAR(500);

CREATE TABLE lost_found_review_audit (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    report_id UUID NOT NULL REFERENCES lost_found_reports(id),
    reviewer_id UUID NOT NULL REFERENCES user_accounts(id),
    decision VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_lost_found_review_decision CHECK (decision IN ('APPROVED', 'REJECTED')),
    CONSTRAINT uq_lost_found_review_report UNIQUE (report_id),
    CONSTRAINT ck_lost_found_review_reason CHECK (CHAR_LENGTH(TRIM(reason)) >= 20)
);

CREATE INDEX idx_lost_found_review_university
    ON lost_found_review_audit(university_id, reviewed_at DESC);
