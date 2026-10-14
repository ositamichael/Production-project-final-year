ALTER TABLE learning_meetings
    ADD COLUMN access_scope VARCHAR(20) NOT NULL DEFAULT 'CAMPUS';

ALTER TABLE learning_meetings
    ADD CONSTRAINT ck_learning_meeting_access_scope
    CHECK (access_scope IN ('CAMPUS', 'ASSIGNED'));

CREATE INDEX idx_learning_meetings_access
    ON learning_meetings(university_id, access_scope, published, starts_at);
