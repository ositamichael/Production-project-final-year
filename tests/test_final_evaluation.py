import csv
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from src.final_evaluation import (
    bootstrap_confidence_intervals,
    run_final_evaluation,
    validate_frozen_dataset,
)


ROOT = Path(__file__).resolve().parents[1]
FIELDS = ["case_id", "text", "label", "scenario", "provenance", "label_rationale"]


class FinalEvaluationTests(unittest.TestCase):
    def _write_dataset(self, folder: Path, rows: list[dict]) -> tuple[Path, Path]:
        dataset = folder / "independent.csv"
        with dataset.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=FIELDS)
            writer.writeheader()
            writer.writerows(rows)
        digest = hashlib.sha256(dataset.read_bytes()).hexdigest()
        manifest = folder / "manifest.json"
        manifest.write_text(
            json.dumps(
                {
                    "dataset_version": "test-independent-v1",
                    "dataset_role": "independent_final_evaluation",
                    "status": "frozen",
                    "frozen_at": "2026-10-07T12:00:00+01:00",
                    "row_count": len(rows),
                    "data_sha256": digest,
                    "provenance_summary": "Unit-test fixture only.",
                    "label_policy": "Phishing requires an unsolicited deceptive sensitive-action request.",
                    "label_review": {"status": "adjudicated", "reviewer_count": 2},
                    "separation": {
                        "model_frozen_before_labels_opened": True,
                        "not_used_for_prompt_or_rule_design": True,
                        "not_used_for_threshold_selection": True,
                    },
                }
            ),
            encoding="utf-8",
        )
        return dataset, manifest

    def test_missing_final_dataset_is_reported_as_pending_not_measured(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            gate = validate_frozen_dataset(
                root / "missing.csv", root / "missing.json", [], minimum_per_class=1
            )
        self.assertFalse(gate.ready)
        self.assertTrue(any("missing" in error.lower() for error in gate.errors))
        self.assertEqual(gate.row_count, 0)

    def test_gate_rejects_overlap_and_insufficient_class_counts(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            rows = [
                {
                    "case_id": "i-001",
                    "text": "The library will be closed on Monday for scheduled maintenance.",
                    "label": "legitimate",
                    "scenario": "notice",
                    "provenance": "fixture",
                    "label_rationale": "No sensitive action request.",
                }
            ]
            dataset, manifest = self._write_dataset(root, rows)
            gate = validate_frozen_dataset(
                dataset,
                manifest,
                [ROOT / "data" / "sample_messages.csv"],
                minimum_per_class=2,
            )
        self.assertFalse(gate.ready)
        self.assertEqual(gate.overlap_count, 1)
        self.assertTrue(any("at least 2" in error for error in gate.errors))

    def test_valid_frozen_fixture_runs_without_changing_model(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            rows = [
                {
                    "case_id": "i-001",
                    "text": "The workshop room booking is confirmed for Thursday afternoon.",
                    "label": "legitimate",
                    "scenario": "ordinary-notice",
                    "provenance": "fixture",
                    "label_rationale": "Routine confirmation without a sensitive request.",
                },
                {
                    "case_id": "i-002",
                    "text": "For the fraud seminar, bring a notebook and arrive ten minutes early.",
                    "label": "legitimate",
                    "scenario": "education",
                    "provenance": "fixture",
                    "label_rationale": "Benign event instruction.",
                },
                {
                    "case_id": "i-003",
                    "text": "Account desk: send the security code from your phone before five o'clock.",
                    "label": "phishing",
                    "scenario": "credential-request",
                    "provenance": "fixture",
                    "label_rationale": "Direct request for an authentication secret.",
                },
                {
                    "case_id": "i-004",
                    "text": "New handset here. Transfer £275 today and keep this conversation secret.",
                    "label": "phishing",
                    "scenario": "payment-impersonation",
                    "provenance": "fixture",
                    "label_rationale": "Changed-contact payment pressure and secrecy.",
                },
            ]
            dataset, manifest = self._write_dataset(root, rows)
            result = run_final_evaluation(
                ROOT / "data" / "sample_messages.csv",
                dataset,
                manifest,
                [ROOT / "data" / "sample_messages.csv"],
                minimum_per_class=2,
            )
        self.assertTrue(result["dataset_gate"]["ready"])
        self.assertEqual(result["dataset"]["version"], "test-independent-v1")
        self.assertEqual(result["dataset"]["role"], "independent_final_evaluation")
        self.assertIn("contextual", result["confidence_intervals"])

    def test_bootstrap_intervals_are_reproducible_and_bounded(self):
        first = bootstrap_confidence_intervals([1, 1, 0, 0], [1, 0, 1, 0], iterations=200)
        second = bootstrap_confidence_intervals([1, 1, 0, 0], [1, 0, 1, 0], iterations=200)
        self.assertEqual(first, second)
        for interval in first.values():
            self.assertGreaterEqual(interval["lower_95"], 0)
            self.assertLessEqual(interval["upper_95"], 1)


if __name__ == "__main__":
    unittest.main()
