"""Verify the final independent evaluation, or report its honest pending state."""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from src.final_evaluation import run_final_evaluation, validate_frozen_dataset  # noqa: E402

DATASET = ROOT / "data" / "independent_evaluation_v1.csv"
MANIFEST = ROOT / "data" / "independent_evaluation_v1.manifest.json"
EXPORT = ROOT / "data" / "independent_evaluation_results.json"
COMPARISONS = [
    ROOT / "data" / "sample_messages.csv",
    ROOT / "data" / "challenge_messages.csv",
    ROOT / "data" / "heldout_messages.csv",
]


def _comparable(result: dict) -> dict:
    return {
        "dataset": result["dataset"],
        "model_version": result["model_version"],
        "decision_threshold": result["decision_threshold"],
        "metrics": result["metrics"],
        "records": result["records"],
        "baseline_metrics": result["baseline_metrics"],
        "failure_examples": result["failure_examples"],
        "dataset_gate": result["dataset_gate"],
        "confidence_intervals": result["confidence_intervals"],
    }


if __name__ == "__main__":
    if not DATASET.exists() and not MANIFEST.exists() and not EXPORT.exists():
        gate = validate_frozen_dataset(DATASET, MANIFEST, COMPARISONS)
        print("final evaluation pending: " + " ".join(gate.errors))
        raise SystemExit(0)
    if not DATASET.exists() or not MANIFEST.exists() or not EXPORT.exists():
        raise SystemExit(
            "Incomplete final-evaluation evidence: dataset, manifest and result export must appear together."
        )
    actual = run_final_evaluation(
        ROOT / "data" / "sample_messages.csv",
        DATASET,
        MANIFEST,
        COMPARISONS,
    )
    expected = json.loads(EXPORT.read_text(encoding="utf-8"))
    if _comparable(actual) != _comparable(expected):
        raise SystemExit(
            "Final evaluation mismatch. Do not overwrite the frozen result until the change is reviewed."
        )
    print(
        f"verified final evaluation: version={actual['dataset']['version']}, "
        f"n={actual['dataset']['test_rows']}, f1={actual['metrics']['f1']:.3f}"
    )

