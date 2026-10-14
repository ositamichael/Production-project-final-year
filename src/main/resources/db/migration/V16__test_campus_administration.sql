ALTER TABLE user_accounts ADD COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE user_accounts ADD CONSTRAINT ck_user_account_status
    CHECK (account_status IN ('ACTIVE', 'SUSPENDED'));

ALTER TABLE campus_events ADD COLUMN organiser_id UUID REFERENCES user_accounts(id);
ALTER TABLE service_offerings ADD COLUMN organiser_id UUID REFERENCES user_accounts(id);
ALTER TABLE learning_meetings ADD COLUMN organiser_id UUID REFERENCES user_accounts(id);
ALTER TABLE opportunities ADD COLUMN organiser_id UUID REFERENCES user_accounts(id);

CREATE TABLE meeting_assignments (
    meeting_id UUID NOT NULL REFERENCES learning_meetings(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    assigned_by UUID NOT NULL REFERENCES user_accounts(id),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (meeting_id, user_id)
);

CREATE INDEX idx_meeting_assignments_user
    ON meeting_assignments(user_id, assigned_at DESC);

CREATE TABLE admin_audit_log (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    actor_id UUID NOT NULL REFERENCES user_accounts(id),
    action_type VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id UUID,
    summary VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_admin_audit_action CHECK (CHAR_LENGTH(TRIM(action_type)) > 0),
    CONSTRAINT ck_admin_audit_resource CHECK (CHAR_LENGTH(TRIM(resource_type)) > 0),
    CONSTRAINT ck_admin_audit_summary CHECK (CHAR_LENGTH(TRIM(summary)) > 0)
);

CREATE INDEX idx_admin_audit_university_time
    ON admin_audit_log(university_id, created_at DESC);

ALTER TABLE notifications DROP CONSTRAINT ck_notification_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notification_type CHECK (
    notification_type IN (
        'CONNECTION_REQUEST', 'CONNECTION_ACCEPTED', 'MESSAGE', 'SAFETY',
        'EVENT', 'BOOKING', 'LOST_FOUND', 'ANNOUNCEMENT', 'MEETING',
        'SUPPORT', 'OPPORTUNITY', 'CIRCLE', 'ADMIN'
    )
);
