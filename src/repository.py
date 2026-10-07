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


class NullAnalysisRepository:
    """Default repository: analysis is returned but no history is stored."""

    status = "disabled"

    def record(self, evidence: EvidenceItem, result: dict) -> None:
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

    def close(self) -> None:
        with self._lock:
            self._connection.close()
