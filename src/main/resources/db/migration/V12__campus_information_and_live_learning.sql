CREATE TABLE campus_announcements (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    author_id UUID REFERENCES user_accounts(id),
    author_name VARCHAR(160) NOT NULL,
    author_role VARCHAR(80) NOT NULL,
    category VARCHAR(30) NOT NULL,
    audience_type VARCHAR(30) NOT NULL DEFAULT 'UNIVERSITY',
    audience_label VARCHAR(180),
    title VARCHAR(220) NOT NULL,
    body VARCHAR(5000) NOT NULL,
    action_label VARCHAR(100),
    action_url VARCHAR(1000),
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    urgent BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_announcement_category CHECK (category IN
      ('UNIVERSITY', 'MODULE', 'PLACEMENT', 'INTERNSHIP', 'CAREERS', 'EVENT', 'SOCIETY', 'URGENT')),
    CONSTRAINT ck_announcement_audience CHECK (audience_type IN
      ('UNIVERSITY', 'COURSE', 'YEAR', 'MODULE')),
    CONSTRAINT ck_announcement_title CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT ck_announcement_body CHECK (CHAR_LENGTH(TRIM(body)) > 0)
);

CREATE INDEX idx_announcements_university_time
    ON campus_announcements(university_id, published_at);

CREATE TABLE announcement_reads (
    announcement_id UUID NOT NULL REFERENCES campus_announcements(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    read_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (announcement_id, user_id)
);

CREATE TABLE support_cases (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    reporter_id UUID NOT NULL REFERENCES user_accounts(id),
    reference_code VARCHAR(30) NOT NULL UNIQUE,
    category VARCHAR(30) NOT NULL,
    subject VARCHAR(220) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    assigned_team VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_support_category CHECK (category IN
      ('ACADEMIC', 'IT', 'FINANCE', 'ACCOMMODATION', 'WELLBEING', 'ACCESSIBILITY', 'OTHER')),
    CONSTRAINT ck_support_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'WAITING_FOR_STUDENT', 'RESOLVED', 'CLOSED')),
    CONSTRAINT ck_support_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT ck_support_subject CHECK (CHAR_LENGTH(TRIM(subject)) > 0),
    CONSTRAINT ck_support_description CHECK (CHAR_LENGTH(TRIM(description)) > 0)
);

CREATE INDEX idx_support_cases_reporter_time ON support_cases(reporter_id, updated_at);

CREATE TABLE support_case_replies (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES support_cases(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES user_accounts(id),
    body VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_support_reply_body CHECK (CHAR_LENGTH(TRIM(body)) > 0)
);

CREATE TABLE learning_meetings (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    host_id UUID REFERENCES user_accounts(id),
    host_name VARCHAR(160) NOT NULL,
    title VARCHAR(220) NOT NULL,
    module_code VARCHAR(40),
    meeting_type VARCHAR(30) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    lobby_opens_at TIMESTAMP WITH TIME ZONE NOT NULL,
    location_label VARCHAR(180) NOT NULL DEFAULT 'Online classroom',
    agenda VARCHAR(3000),
    provider_join_url VARCHAR(1000),
    chat_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    questions_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    attendance_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_meeting_type CHECK (meeting_type IN ('LECTURE', 'SEMINAR', 'TUTORIAL', 'PROJECT', 'DROP_IN')),
    CONSTRAINT ck_meeting_times CHECK (ends_at > starts_at),
    CONSTRAINT ck_meeting_title CHECK (CHAR_LENGTH(TRIM(title)) > 0)
);

CREATE INDEX idx_learning_meetings_university_time
    ON learning_meetings(university_id, starts_at);

CREATE TABLE meeting_attendance (
    meeting_id UUID NOT NULL REFERENCES learning_meetings(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    first_joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_left_at TIMESTAMP WITH TIME ZONE,
    join_count INTEGER NOT NULL DEFAULT 1,
    hand_raised BOOLEAN NOT NULL DEFAULT FALSE,
    hand_raised_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (meeting_id, user_id),
    CONSTRAINT ck_attendance_join_count CHECK (join_count > 0)
);

CREATE TABLE meeting_messages (
    id UUID PRIMARY KEY,
    meeting_id UUID NOT NULL REFERENCES learning_meetings(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES user_accounts(id),
    message_type VARCHAR(20) NOT NULL DEFAULT 'CHAT',
    body VARCHAR(2000) NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_meeting_message_type CHECK (message_type IN ('CHAT', 'QUESTION', 'ANSWER', 'ANNOUNCEMENT')),
    CONSTRAINT ck_meeting_message_body CHECK (CHAR_LENGTH(TRIM(body)) > 0)
);

CREATE INDEX idx_meeting_messages_time ON meeting_messages(meeting_id, sent_at);
