CREATE TABLE pilot_acceptance_results (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    tester_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    scenario_code VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_RUN',
    evidence_note VARCHAR(2000),
    device_label VARCHAR(180),
    tested_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pilot_result_tester_scenario UNIQUE (tester_id, scenario_code),
    CONSTRAINT ck_pilot_result_status CHECK (status IN ('NOT_RUN', 'PASS', 'FAIL', 'BLOCKED')),
    CONSTRAINT ck_pilot_result_scenario CHECK (CHAR_LENGTH(TRIM(scenario_code)) > 0)
);

CREATE INDEX idx_pilot_results_university
    ON pilot_acceptance_results(university_id, updated_at);
