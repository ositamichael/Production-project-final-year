CREATE TABLE academic_programmes (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    code VARCHAR(60) NOT NULL,
    title VARCHAR(180) NOT NULL,
    qualification VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_programme_code_per_university UNIQUE (university_id, code)
);

CREATE INDEX idx_programmes_university ON academic_programmes(university_id);

CREATE TABLE academic_modules (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    code VARCHAR(60) NOT NULL,
    title VARCHAR(180) NOT NULL,
    study_level INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_module_code_per_university UNIQUE (university_id, code),
    CONSTRAINT ck_module_study_level CHECK (study_level BETWEEN 1 AND 8)
);

CREATE INDEX idx_modules_university ON academic_modules(university_id);

CREATE TABLE programme_modules (
    programme_id UUID NOT NULL REFERENCES academic_programmes(id) ON DELETE CASCADE,
    module_id UUID NOT NULL REFERENCES academic_modules(id) ON DELETE CASCADE,
    year_of_study INTEGER NOT NULL,
    PRIMARY KEY (programme_id, module_id),
    CONSTRAINT ck_programme_module_year CHECK (year_of_study BETWEEN 1 AND 12)
);

CREATE TABLE enrolments (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    user_id UUID NOT NULL REFERENCES user_accounts(id),
    programme_id UUID NOT NULL REFERENCES academic_programmes(id),
    year_of_study INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_on DATE,
    expected_end_on DATE,
    CONSTRAINT uq_active_programme_enrolment UNIQUE (user_id, programme_id),
    CONSTRAINT ck_enrolment_year CHECK (year_of_study BETWEEN 1 AND 12),
    CONSTRAINT ck_enrolment_status CHECK (status IN ('ACTIVE', 'INTERRUPTED', 'COMPLETED', 'WITHDRAWN'))
);

CREATE INDEX idx_enrolments_university_programme ON enrolments(university_id, programme_id);
CREATE INDEX idx_enrolments_user ON enrolments(user_id);
