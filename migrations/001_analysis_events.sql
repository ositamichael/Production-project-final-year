CREATE TABLE IF NOT EXISTS analysis_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    evidence_kind TEXT NOT NULL,
    source_label TEXT NOT NULL,
    text_sha256 TEXT NOT NULL,
    text_length INTEGER NOT NULL CHECK (text_length >= 0),
    detector TEXT NOT NULL,
    model_version TEXT NOT NULL,
    result_label TEXT NOT NULL,
    risk_score REAL NOT NULL CHECK (risk_score >= 0 AND risk_score <= 1),
    signals_json TEXT NOT NULL DEFAULT '[]'
);

CREATE INDEX IF NOT EXISTS idx_analysis_events_created_at
    ON analysis_events(created_at);

CREATE INDEX IF NOT EXISTS idx_analysis_events_model_version
    ON analysis_events(model_version);
