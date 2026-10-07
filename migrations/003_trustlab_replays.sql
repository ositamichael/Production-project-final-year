CREATE TABLE IF NOT EXISTS trustlab_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    suite_version TEXT NOT NULL,
    transformation_version TEXT NOT NULL,
    dataset_role TEXT NOT NULL,
    seed_sha256 TEXT NOT NULL,
    transformation_manifest_sha256 TEXT NOT NULL,
    model_version TEXT NOT NULL,
    decision_threshold REAL NOT NULL CHECK (decision_threshold >= 0 AND decision_threshold <= 1),
    seed_case_count INTEGER NOT NULL CHECK (seed_case_count >= 0),
    transformed_case_count INTEGER NOT NULL CHECK (transformed_case_count >= 0)
);

CREATE TABLE IF NOT EXISTS trustlab_case_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id INTEGER NOT NULL REFERENCES trustlab_runs(id) ON DELETE CASCADE,
    case_key TEXT NOT NULL,
    source_key TEXT NOT NULL,
    scenario TEXT NOT NULL,
    detector TEXT NOT NULL,
    transformation TEXT NOT NULL,
    relation TEXT NOT NULL CHECK (relation IN ('preserve_label', 'change_to_legitimate')),
    expected_label TEXT NOT NULL CHECK (expected_label IN ('phishing', 'legitimate')),
    predicted_label TEXT NOT NULL CHECK (predicted_label IN ('phishing', 'legitimate')),
    risk_score REAL NOT NULL CHECK (risk_score >= 0 AND risk_score <= 1),
    correct INTEGER NOT NULL CHECK (correct IN (0, 1)),
    changed_from_original INTEGER NOT NULL CHECK (changed_from_original IN (0, 1)),
    text_sha256 TEXT NOT NULL,
    UNIQUE (run_id, case_key)
);

CREATE INDEX IF NOT EXISTS idx_trustlab_runs_version
    ON trustlab_runs(suite_version, created_at);

CREATE INDEX IF NOT EXISTS idx_trustlab_cases_run_detector
    ON trustlab_case_results(run_id, detector, transformation);
