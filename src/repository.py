"""Repository interfaces and privacy-preserving SQLite implementation."""

from __future__ import annotations

import hashlib
import json
import sqlite3
import threading
from pathlib import Path
from typing import Protocol

try:
    from .evidence import EvidenceItem
except ImportError:  # Supports the Render `python src/server.py` entry point.
    from evidence import EvidenceItem


ROOT = Path(__file__).resolve().parents[1]
MIGRATIONS = ROOT / "migrations"


class AnalysisRepository(Protocol):
    """Storage contract used by the analysis service."""

    status: str

    def record(self, evidence: EvidenceItem, result: dict) -> None: ...


class EvaluationRepository(Protocol):
    """Storage contract for versioned evaluation-run metadata."""

    def record_evaluation(self, result: dict, data_sha256: str, dataset_role: str) -> int | None: ...

    def record_trustlab(self, result: dict) -> int | None: ...


class NullAnalysisRepository:
    """Default repository: analysis is returned but no history is stored."""

    status = "disabled"

    def record(self, evidence: EvidenceItem, result: dict) -> None:
        return None

    def record_evaluation(self, result: dict, data_sha256: str, dataset_role: str) -> None:
        return None

    def record_trustlab(self, result: dict) -> None:
        return None


class SqliteAnalysisRepository:
    """Store anonymous research metadata without retaining message text."""

    status = "anonymous_metadata_only"

    def __init__(self, database_path: str | Path):
        self.database_path = str(database_path)
        self._lock = threading.RLock()
        self._connection = sqlite3.connect(self.database_path, check_same_thread=False)
        self._connection.row_factory = sqlite3.Row
        self._connection.execute("PRAGMA foreign_keys = ON")
        self._apply_migrations()

    def _apply_migrations(self) -> None:
        with self._lock:
            self._connection.execute(
                "CREATE TABLE IF NOT EXISTS schema_migrations "
                "(version TEXT PRIMARY KEY, applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)"
            )
            applied = {
                row["version"]
                for row in self._connection.execute("SELECT version FROM schema_migrations")
            }
            for migration in sorted(MIGRATIONS.glob("*.sql")):
                if migration.name in applied:
                    continue
                self._connection.executescript(migration.read_text(encoding="utf-8"))
                self._connection.execute(
                    "INSERT INTO schema_migrations(version) VALUES (?)", (migration.name,)
                )
            self._connection.commit()

    def record(self, evidence: EvidenceItem, result: dict) -> None:
        digest = hashlib.sha256(evidence.text.encode("utf-8")).hexdigest()
        signals = result.get("signals") or []
        with self._lock:
            self._connection.execute(
                """
                INSERT INTO analysis_events (
                    evidence_kind, source_label, text_sha256, text_length,
                    detector, model_version, result_label, risk_score, signals_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    evidence.kind.value,
                    evidence.source_label,
                    digest,
                    len(evidence.text),
                    str(result.get("detector", "unknown")),
                    str(result.get("model_version", "unknown")),
                    str(result.get("label") or result.get("prediction") or "unknown"),
                    float(result.get("risk_score", result.get("score", 0.0))),
                    json.dumps(list(signals)),
                ),
            )
            self._connection.commit()

    def recent_events(self, limit: int = 20) -> list[dict]:
        safe_limit = max(1, min(int(limit), 100))
        with self._lock:
            rows = self._connection.execute(
                "SELECT * FROM analysis_events ORDER BY id DESC LIMIT ?", (safe_limit,)
            ).fetchall()
        return [dict(row) for row in rows]

    def record_evaluation(self, result: dict, data_sha256: str, dataset_role: str) -> int:
        dataset = result["dataset"]
        metrics = result["metrics"]
        confusion = metrics["confusion_matrix"]
        classes = dataset["class_distribution"]
        with self._lock:
            cursor = self._connection.execute(
                """
                INSERT INTO evaluation_runs (
                    dataset_version, dataset_role, data_sha256, model_version,
                    decision_threshold, sample_count, phishing_count, legitimate_count,
                    precision, recall, f1, true_positive, true_negative,
                    false_positive, false_negative
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    dataset["version"],
                    dataset_role,
                    data_sha256,
                    result["model_version"],
                    result["decision_threshold"],
                    dataset["test_rows"],
                    int(classes.get("phishing", 0)),
                    int(classes.get("legitimate", 0)),
                    metrics["precision"],
                    metrics["recall"],
                    metrics["f1"],
                    confusion["true_positive"],
                    confusion["true_negative"],
                    confusion["false_positive"],
                    confusion["false_negative"],
                ),
            )
            run_id = int(cursor.lastrowid)
            self._connection.executemany(
                """
                INSERT INTO evaluation_case_results (
                    run_id, case_key, scenario, expected_label, predicted_label, risk_score
                ) VALUES (?, ?, ?, ?, ?, ?)
                """,
                [
                    (
                        run_id,
                        f"case-{index:03d}",
                        record["scenario"],
                        record["expected"],
                        record["actual"],
                        record["risk_score"],
                    )
                    for index, record in enumerate(result["records"], start=1)
                ],
            )
            self._connection.commit()
        return run_id

    def recent_evaluation_runs(self, limit: int = 20) -> list[dict]:
        safe_limit = max(1, min(int(limit), 100))
        with self._lock:
            rows = self._connection.execute(
                "SELECT * FROM evaluation_runs ORDER BY id DESC LIMIT ?", (safe_limit,)
            ).fetchall()
        return [dict(row) for row in rows]

    def record_trustlab(self, result: dict) -> int:
        suite = result["suite"]
        with self._lock:
            cursor = self._connection.execute(
                """
                INSERT INTO trustlab_runs (
                    suite_version, transformation_version, dataset_role,
                    seed_sha256, transformation_manifest_sha256, model_version,
                    decision_threshold, seed_case_count, transformed_case_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    suite["version"],
                    suite["transformation_version"],
                    suite["dataset_role"],
                    suite["seed_sha256"],
                    suite["transformation_manifest_sha256"],
                    result["model_version"],
                    result["decision_threshold"],
                    suite["seed_case_count"],
                    suite["transformed_case_count"],
                ),
            )
            run_id = int(cursor.lastrowid)
            self._connection.executemany(
                """
                INSERT INTO trustlab_case_results (
                    run_id, case_key, source_key, scenario, detector,
                    transformation, relation, expected_label, predicted_label,
                    risk_score, correct, changed_from_original, text_sha256
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                [
                    (
                        run_id,
                        record["case_key"],
                        record["source_key"],
                        record["scenario"],
                        record["detector"],
                        record["transformation"],
                        record["relation"],
                        record["expected"],
                        record["predicted"],
                        record["score"],
                        int(record["correct"]),
                        int(record["changed_from_original"]),
                        record["text_sha256"],
                    )
                    for record in result["records"]
                ],
            )
            self._connection.commit()
        return run_id

    def recent_trustlab_runs(self, limit: int = 20) -> list[dict]:
        safe_limit = max(1, min(int(limit), 100))
        with self._lock:
            rows = self._connection.execute(
                "SELECT * FROM trustlab_runs ORDER BY id DESC LIMIT ?", (safe_limit,)
            ).fetchall()
        return [dict(row) for row in rows]

    def close(self) -> None:
        with self._lock:
            self._connection.close()
