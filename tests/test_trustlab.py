import json
import sqlite3
import tempfile
import unittest
from pathlib import Path

from src.repository import SqliteAnalysisRepository
from src.trustlab import TRANSFORMATIONS, run_trustlab


ROOT = Path(__file__).resolve().parents[1]


class TrustLabTransformationTests(unittest.TestCase):
    def test_transformations_are_deterministic_and_declared(self):
        text = "Urgent: send your verification code immediately."
        first = [item.apply(text, "phishing") for item in TRANSFORMATIONS]
        second = [item.apply(text, "phishing") for item in TRANSFORMATIONS]
        self.assertEqual(first, second)
        self.assertEqual(first[0].transformation, "identity")
        self.assertTrue(
            all(item.relation in {"preserve_label", "change_to_legitimate"} for item in first)
        )

    def test_label_changing_transformations_apply_only_to_scam_cases(self):
        legitimate = [
            item.apply("The library opens at nine.", "legitimate")
            for item in TRANSFORMATIONS
        ]
        self.assertEqual(sum(item is None for item in legitimate), 2)
        phishing = [item.apply("Send your code now.", "phishing") for item in TRANSFORMATIONS]
        changed = [item for item in phishing if item.relation == "change_to_legitimate"]
        self.assertEqual(len(changed), 2)
        self.assertTrue(all(item.expected_label == "legitimate" for item in changed))


class TrustLabReplayTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.result = run_trustlab(
            ROOT / "data" / "sample_messages.csv",
            ROOT / "data" / "challenge_messages.csv",
        )

    def test_replay_keeps_final_heldout_data_out_of_development(self):
        with self.assertRaisesRegex(ValueError, "held-out"):
            run_trustlab(
                ROOT / "data" / "sample_messages.csv",
                ROOT / "data" / "heldout_messages.csv",
            )

    def test_replay_has_paired_detector_records_and_failure_analysis(self):
        suite = self.result["suite"]
        self.assertEqual(suite["seed_case_count"], 24)
        self.assertEqual(suite["transformed_case_count"], 144)
        self.assertEqual(len(self.result["records"]), 288)
        self.assertEqual(set(self.result["detectors"]), {"contextual", "tfidf_baseline"})
        self.assertIn("failure_examples", self.result)
        self.assertEqual(len(suite["seed_sha256"]), 64)
        self.assertEqual(len(suite["transformation_manifest_sha256"]), 64)

    def test_export_does_not_store_transformed_message_text(self):
        serialised = json.dumps(self.result)
        self.assertNotIn("Dad here on a new phone", serialised)
        self.assertTrue(
            all(len(record["text_sha256"]) == 64 for record in self.result["records"])
        )

    def test_sqlite_replay_storage_is_relational_and_text_free(self):
        with tempfile.TemporaryDirectory() as directory:
            database = Path(directory) / "trustlab.sqlite3"
            repository = SqliteAnalysisRepository(database)
            result = run_trustlab(
                ROOT / "data" / "sample_messages.csv",
                ROOT / "data" / "challenge_messages.csv",
                repository=repository,
            )
            runs = repository.recent_trustlab_runs()
            repository.close()
            connection = sqlite3.connect(database)
            try:
                count = connection.execute(
                    "SELECT COUNT(*) FROM trustlab_case_results WHERE run_id = ?",
                    (runs[0]["id"],),
                ).fetchone()[0]
                columns = {
                    row[1]
                    for row in connection.execute("PRAGMA table_info(trustlab_case_results)")
                }
            finally:
                connection.close()
            self.assertEqual(count, len(result["records"]))
            self.assertNotIn("text", columns)


if __name__ == "__main__":
    unittest.main()
