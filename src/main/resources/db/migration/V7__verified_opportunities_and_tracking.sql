CREATE TABLE opportunity_publishers (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    name VARCHAR(180) NOT NULL,
    publisher_type VARCHAR(30) NOT NULL,
    website_domain VARCHAR(180),
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    verified_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_opportunity_publisher_type
        CHECK (publisher_type IN ('UNIVERSITY', 'EMPLOYER', 'CHARITY', 'STUDENT_UNION')),
    CONSTRAINT ck_opportunity_publisher_verification
        CHECK (verification_status IN ('PENDING', 'VERIFIED', 'SUSPENDED')),
    CONSTRAINT uq_opportunity_publisher UNIQUE (university_id, name)
);

CREATE INDEX idx_opportunity_publishers_university
    ON opportunity_publishers(university_id, verification_status);

CREATE TABLE opportunities (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    publisher_id UUID NOT NULL REFERENCES opportunity_publishers(id),
    title VARCHAR(180) NOT NULL,
    summary VARCHAR(600) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    opportunity_type VARCHAR(30) NOT NULL,
    work_mode VARCHAR(30) NOT NULL,
    location VARCHAR(180) NOT NULL,
    hours_label VARCHAR(120) NOT NULL,
    compensation_label VARCHAR(120) NOT NULL,
    skills_text VARCHAR(600) NOT NULL,
    eligibility VARCHAR(800) NOT NULL,
    closes_at DATE NOT NULL,
    moderation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_opportunity_type
        CHECK (opportunity_type IN ('JOB', 'PLACEMENT', 'VOLUNTEERING')),
    CONSTRAINT ck_opportunity_work_mode
        CHECK (work_mode IN ('ON_CAMPUS', 'HYBRID', 'REMOTE', 'FLEXIBLE')),
    CONSTRAINT ck_opportunity_moderation
        CHECK (moderation_status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT uq_opportunity_listing UNIQUE (university_id, publisher_id, title, closes_at)
);

CREATE INDEX idx_opportunities_catalogue
    ON opportunities(university_id, moderation_status, closes_at, opportunity_type);

CREATE TABLE saved_opportunities (
    opportunity_id UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_accounts(id),
    saved_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (opportunity_id, user_id)
);

CREATE INDEX idx_saved_opportunities_user
    ON saved_opportunities(user_id, saved_at DESC);

CREATE TABLE opportunity_tracking (
    opportunity_id UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_accounts(id),
    tracking_status VARCHAR(30) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (opportunity_id, user_id),
    CONSTRAINT ck_opportunity_tracking_status
        CHECK (tracking_status IN ('PLANNING', 'APPLIED', 'INTERVIEW', 'OFFER', 'CLOSED'))
);

CREATE INDEX idx_opportunity_tracking_user
    ON opportunity_tracking(user_id, tracking_status, updated_at DESC);
