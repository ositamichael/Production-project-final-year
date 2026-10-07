"""Verify that versioned evaluation exports match a fresh calculation.

Run dates are deliberately excluded from the comparison: they describe when an
export was produced, while this check establishes whether the model, dataset,
threshold, metrics and case-level decisions reproduce.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from src.evaluate import evaluate  # noqa: E402 - repository root is added above


def _load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def _comparable(result: dict[str, Any]) -> dict[str, Any]:
    return {
        "dataset": result["dataset"],
        "model_version": result["model_version"],
        "decision_threshold": result["decision_threshold"],
        "score_scale": result["score_scale"],
        "metrics": result["metrics"],
        "records": result["records"],
        "baseline_metrics": result["baseline_metrics"],
        "failure_examples": result["failure_examples"],
    }


def _verify(test_name: str, export_name: str) -> None:
    actual = evaluate(ROOT / "data" / "sample_messages.csv", ROOT / "data" / test_name)
    expected = _load(ROOT / "data" / export_name)
    if _comparable(actual) != _comparable(expected):
        raise SystemExit(
            f"Evaluation mismatch for {test_name}. Run `python -m src.evaluate`, "
            "inspect the changes and document any intentional model or dataset update."
        )
    metrics = actual["metrics"]
    print(
        f"verified {test_name}: n={actual['dataset']['test_rows']}, "
        f"precision={metrics['precision']:.3f}, recall={metrics['recall']:.3f}, "
        f"f1={metrics['f1']:.3f}"
    )


if __name__ == "__main__":
    _verify("heldout_messages.csv", "evaluation_results.json")
    _verify("challenge_messages.csv", "challenge_evaluation.json")
