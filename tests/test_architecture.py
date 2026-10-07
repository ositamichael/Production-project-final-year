import sqlite3
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from src.core import explain, load_or_train
from src.detectors import ContextualDetectorStrategy, TfidfBaselineStrategy
from src.evidence import (
    EvidenceKind,
    OcrTextEvidenceAdapter,
    RecordingTextEvidenceAdapter,
    TextEvidenceAdapter,
    adapter_for,
)
from src.evaluate import evaluate
from src.repository import NullAnalysisRepository, SqliteAnalysisRepository
from src.services import AnalysisService, StrategyComparisonService


ROOT = Path(__file__).resolve().parents[1]


class EvidenceAdapterTests(unittest.TestCase):
    def test_text_adapter_validates_and_labels_input(self):
        evidence = TextEvidenceAdapter().adapt({"text": "  Please review this ordinary message.  "})
        self.assertEqual(evidence.text, "Please review this ordinary message.")
        self.assertEqual(evidence.kind, EvidenceKind.TEXT)
        self.assertEqual(evidence.source_label, "pasted_text")

    def test_ocr_and_recording_adapters_keep_sources_distinct(self):
        screenshot = OcrTextEvidenceAdapter().adapt({"ocr_text": "Visible screenshot text for review."})
        recording = RecordingTextEvidenceAdapter().adapt({"recording_text": "Combined recording frame text."})
        self.assertEqual(screenshot.kind, EvidenceKind.OCR_TEXT)
        self.assertEqual(recording.kind, EvidenceKind.RECORDING_TEXT)
        self.assertNotEqual(screenshot.source_label, recording.source_label)

    def test_untrusted_source_label_is_not_preserved(self):
        evidence = TextEvidenceAdapter().adapt(
            {"text": "A sufficiently long message for analysis.", "source_label": "private account 1234"}
        )
        self.assertEqual(evidence.source_label, "pasted_text")

    def test_adapter_factory_rejects_unknown_evidence(self):
        with self.assertRaisesRegex(ValueError, "Unsupported evidence type"):
            adapter_for("camera_stream")

    def test_adapter_preserves_server_length_messages(self):
        with self.assertRaisesRegex(ValueError, "longer message"):
            TextEvidenceAdapter().adapt({"text": "short"})
        with self.assertRaisesRegex(ValueError, "4,000"):
            TextEvidenceAdapter().adapt({"text": "x" * 4001})


class StrategyAndServiceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.model = load_or_train(
            ROOT / "artifacts" / "test_model.json",
            ROOT / "data" / "sample_messages.csv",
        )

    def test_contextual_strategy_preserves_existing_result(self):
        text = "Urgent: send your verification code to this helpdesk now."
        evidence = TextEvidenceAdapter().adapt({"text": text})
        expected = explain(text, self.model.predict_probability(text), self.model)
        actual = AnalysisService(ContextualDetectorStrategy(self.model)).analyse(evidence)
        for key in ("label", "risk_score", "signals", "evidence", "action", "model_version"):
            self.assertEqual(actual[key], expected[key])
        self.assertEqual(actual["detector"], "contextual")

    def test_baseline_strategy_has_a_consistent_comparison_contract(self):
        evidence = TextEvidenceAdapter().adapt({"text": "Please send your password immediately."})
        result = TfidfBaselineStrategy(self.model).analyse(evidence)
        self.assertEqual(result["detector"], "tfidf_baseline")
        self.assertIn(result["prediction"], {"phishing", "legitimate"})
        self.assertGreaterEqual(result["score"], 0)
        self.assertLessEqual(result["score"], 1)

    def test_comparison_service_runs_both_strategies_on_one_evidence_item(self):
        evidence = TextEvidenceAdapter().adapt({"text": "Please send your password immediately."})
        service = StrategyComparisonService(
            [ContextualDetectorStrategy(self.model), TfidfBaselineStrategy(self.model)]
        )
        compared = service.compare(evidence)
        self.assertEqual(set(compared), {"contextual", "tfidf_baseline"})
        self.assertEqual(compared["contextual"]["evidence_kind"], "text")
        self.assertEqual(compared["tfidf_baseline"]["evidence_kind"], "text")

    def test_comparison_service_rejects_an_invalid_configuration(self):
        with self.assertRaisesRegex(ValueError, "at least two"):
            StrategyComparisonService([ContextualDetectorStrategy(self.model)])

    def test_null_repository_keeps_live_analysis_stateless(self):
        service = AnalysisService(ContextualDetectorStrategy(self.model), NullAnalysisRepository())
        evidence = TextEvidenceAdapter().adapt({"text": "The library opens at nine tomorrow morning."})
        service.analyse(evidence)
        self.assertEqual(service.persistence_status, "disabled")

    def test_render_direct_script_import_mode_resolves_architecture_modules(self):
        check = subprocess.run(
            [
                sys.executable,
                "-c",
                "import sys; sys.path.insert(0, 'src'); import server; "
                "print(server.analysis_service.persistence_status)",
            ],
            cwd=ROOT,
            capture_output=True,
            text=True,
            check=False,
        )
        self.assertEqual(check.returncode, 0, check.stderr)
        self.assertEqual(check.stdout.strip(), "disabled")


class SqliteRepositoryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.model = load_or_train(
            ROOT / "artifacts" / "test_model.json",
            ROOT / "data" / "sample_messages.csv",
        )

    def test_repository_records_metadata_without_raw_message(self):
        secret_text = "Send your password and verification code immediately."
        with tempfile.TemporaryDirectory() as directory:
            database = Path(directory) / "research.sqlite3"
            repository = SqliteAnalysisRepository(database)
            service = AnalysisService(ContextualDetectorStrategy(self.model), repository)
            service.analyse(TextEvidenceAdapter().adapt({"text": secret_text}))
            events = repository.recent_events()
            repository.close()

            self.assertEqual(len(events), 1)
            self.assertEqual(events[0]["text_length"], len(secret_text))
            self.assertEqual(len(events[0]["text_sha256"]), 64)
            self.assertNotIn(secret_text, database.read_bytes().decode("utf-8", errors="ignore"))

    def test_migrations_are_idempotent_and_versioned(self):
        with tempfile.TemporaryDirectory() as directory:
            database = Path(directory) / "research.sqlite3"
            first = SqliteAnalysisRepository(database)
            first.close()
            second = SqliteAnalysisRepository(database)
            second.close()
            connection = sqlite3.connect(database)
            try:
                versions = connection.execute("SELECT version FROM schema_migrations").fetchall()
                columns = connection.execute("PRAGMA table_info(analysis_events)").fetchall()
            finally:
                connection.close()
            self.assertEqual(
                versions,
                [("001_analysis_events.sql",), ("002_evaluation_runs.sql",)],
            )
            self.assertNotIn("text", {column[1] for column in columns})

    def test_evaluation_run_and_case_results_are_relational_and_text_free(self):
        with tempfile.TemporaryDirectory() as directory:
            database = Path(directory) / "research.sqlite3"
            repository = SqliteAnalysisRepository(database)
            result = evaluate(
                ROOT / "data" / "sample_messages.csv",
                ROOT / "data" / "heldout_messages.csv",
                repository=repository,
                dataset_role="small held-out evaluation",
            )
            runs = repository.recent_evaluation_runs()
            repository.close()

            connection = sqlite3.connect(database)
            try:
                case_count = connection.execute(
                    "SELECT COUNT(*) FROM evaluation_case_results WHERE run_id = ?",
                    (runs[0]["id"],),
                ).fetchone()[0]
                run_columns = {row[1] for row in connection.execute("PRAGMA table_info(evaluation_runs)")}
                case_columns = {row[1] for row in connection.execute("PRAGMA table_info(evaluation_case_results)")}
            finally:
                connection.close()

            self.assertEqual(case_count, result["dataset"]["test_rows"])
            self.assertEqual(runs[0]["sample_count"], 12)
            self.assertEqual(runs[0]["dataset_version"], "heldout_messages-v1")
            self.assertNotIn("text", run_columns)
            self.assertNotIn("text", case_columns)


if __name__ == "__main__":
    unittest.main()
