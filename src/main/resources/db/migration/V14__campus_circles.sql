CREATE TABLE campus_circles (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    owner_account_id UUID REFERENCES user_accounts(id),
    name VARCHAR(120) NOT NULL,
    description VARCHAR(600) NOT NULL,
    category VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_campus_circle_name UNIQUE (university_id, name),
    CONSTRAINT ck_campus_circle_category CHECK (category IN
      ('ACADEMIC', 'SOCIAL', 'COMMUNITY', 'CAREERS', 'PROJECTS')),
    CONSTRAINT ck_campus_circle_name CHECK (CHAR_LENGTH(TRIM(name)) >= 3),
    CONSTRAINT ck_campus_circle_description CHECK (CHAR_LENGTH(TRIM(description)) >= 10)
);

CREATE INDEX idx_campus_circles_university
    ON campus_circles(university_id, category, active);

CREATE TABLE circle_members (
    circle_id UUID NOT NULL REFERENCES campus_circles(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    member_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (circle_id, account_id),
    CONSTRAINT ck_circle_member_role CHECK (member_role IN ('OWNER', 'MODERATOR', 'MEMBER'))
);

CREATE INDEX idx_circle_members_account
    ON circle_members(account_id, joined_at);
