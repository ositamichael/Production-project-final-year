ALTER TABLE connections ADD COLUMN blocked_by_id UUID REFERENCES user_accounts(id);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    recipient_id UUID NOT NULL REFERENCES user_accounts(id),
    notification_type VARCHAR(30) NOT NULL,
    title VARCHAR(180) NOT NULL,
    body VARCHAR(500) NOT NULL,
    resource_type VARCHAR(30),
    resource_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_notification_type CHECK (
        notification_type IN ('CONNECTION_REQUEST', 'CONNECTION_ACCEPTED', 'MESSAGE', 'SAFETY')
    )
);

CREATE INDEX idx_notifications_recipient_time
    ON notifications(recipient_id, created_at DESC);
CREATE INDEX idx_notifications_unread
    ON notifications(recipient_id, read_at, created_at DESC);

CREATE TABLE safety_reports (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    reporter_id UUID NOT NULL REFERENCES user_accounts(id),
    reported_account_id UUID NOT NULL REFERENCES user_accounts(id),
    reason VARCHAR(40) NOT NULL,
    details VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_report_distinct_users CHECK (reporter_id <> reported_account_id),
    CONSTRAINT ck_report_reason CHECK (
        reason IN ('HARASSMENT', 'SPAM', 'IMPERSONATION', 'UNSAFE_BEHAVIOUR', 'OTHER')
    ),
    CONSTRAINT ck_report_status CHECK (status IN ('OPEN', 'REVIEWING', 'RESOLVED', 'DISMISSED'))
);

CREATE INDEX idx_safety_reports_university_status
    ON safety_reports(university_id, status, created_at DESC);
