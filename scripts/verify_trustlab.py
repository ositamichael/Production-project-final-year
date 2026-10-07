"""Reproduce the checked-in TrustLab development robustness export."""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from src.trustlab import run_trustlab  # noqa: E402


if __name__ == "__main__":
    expected = json.loads((ROOT / "data" / "trustlab_results.json").read_text(encoding="utf-8"))
    actual = run_trustlab(
        ROOT / "data" / "sample_messages.csv",
        ROOT / "data" / "challenge_messages.csv",
    )
    if actual != expected:
        raise SystemExit(
            "TrustLab replay mismatch. Run `python -m src.trustlab`, inspect the "
            "changes and document any intentional update."
        )
    contextual = actual["detectors"]["contextual"]["overall"]
    baseline = actual["detectors"]["tfidf_baseline"]["overall"]
    print(
        "verified TrustLab: "
        f"cases={actual['suite']['transformed_case_count']}, "
        f"contextual_f1={contextual['f1']:.3f}, baseline_f1={baseline['f1']:.3f}"
    )
