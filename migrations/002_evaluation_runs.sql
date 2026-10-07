CREATE TABLE IF NOT EXISTS evaluation_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dataset_version TEXT NOT NULL,
    dataset_role TEXT NOT NULL,
    data_sha256 TEXT NOT NULL,
    model_version TEXT NOT NULL,
    decision_threshold REAL NOT NULL CHECK (decision_threshold >= 0 AND decision_threshold <= 1),
    sample_count INTEGER NOT NULL CHECK (sample_count >= 0),
    phishing_count INTEGER NOT NULL CHECK (phishing_count >= 0),
    legitimate_count INTEGER NOT NULL CHECK (legitimate_count >= 0),
    precision REAL NOT NULL CHECK (precision >= 0 AND precision <= 1),
    recall REAL NOT NULL CHECK (recall >= 0 AND recall <= 1),
    f1 REAL NOT NULL CHECK (f1 >= 0 AND f1 <= 1),
    true_positive INTEGER NOT NULL CHECK (true_positive >= 0),
    true_negative INTEGER NOT NULL CHECK (true_negative >= 0),
    false_positive INTEGER NOT NULL CHECK (false_positive >= 0),
    false_negative INTEGER NOT NULL CHECK (false_negative >= 0)
);

CREATE TABLE IF NOT EXISTS evaluation_case_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id INTEGER NOT NULL REFERENCES evaluation_runs(id) ON DELETE CASCADE,
    case_key TEXT NOT NULL,
    scenario TEXT NOT NULL,
    expected_label TEXT NOT NULL CHECK (expected_label IN ('phishing', 'legitimate')),
    predicted_label TEXT NOT NULL CHECK (predicted_label IN ('phishing', 'legitimate')),
    risk_score REAL NOT NULL CHECK (risk_score >= 0 AND risk_score <= 1),
    UNIQUE (run_id, case_key)
);

CREATE INDEX IF NOT EXISTS idx_evaluation_runs_dataset_version
    ON evaluation_runs(dataset_version, created_at);

CREATE INDEX IF NOT EXISTS idx_evaluation_cases_run_id
    ON evaluation_case_results(run_id);
