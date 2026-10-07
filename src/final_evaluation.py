"""Guarded, reproducible final evaluation for ScamShield.

The existing 12-message held-out file is useful regression evidence, but it is
too small to support a general reliability claim.  This module deliberately
refuses to call a dataset "final" until its manifest, class balance, labelling
review and separation from development data have been checked.
"""

from __future__ import annotations

import csv
import hashlib
import json
import random
import re
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from .evaluate import evaluate


FINAL_DATASET_ROLE = "independent_final_evaluation"
REQUIRED_COLUMNS = {
    "case_id",
    "text",
    "label",
    "scenario",
    "provenance",
    "label_rationale",
}
ALLOWED_LABELS = {"phishing", "legitimate"}
DEFAULT_MINIMUM_PER_CLASS = 50
BOOTSTRAP_SEED = 20261007
BOOTSTRAP_ITERATIONS = 2000


@dataclass(frozen=True)
class DatasetGate:
    """Result of checking whether final evaluation is allowed to run."""

    ready: bool
    errors: tuple[str, ...]
    warnings: tuple[str, ...]
    dataset_sha256: str
    row_count: int
    class_distribution: dict[str, int]
    overlap_count: int

    def to_dict(self) -> dict:
        return {
            "ready": self.ready,
            "errors": list(self.errors),
            "warnings": list(self.warnings),
            "dataset_sha256": self.dataset_sha256,
            "row_count": self.row_count,
            "class_distribution": self.class_distribution,
            "overlap_count": self.overlap_count,
        }


def _read_csv(path: Path) -> tuple[list[dict[str, str]], set[str]]:
    if not path.exists():
        return [], set()
    with path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        return list(reader), set(reader.fieldnames or [])


def _normalise_text(text: str) -> str:
    return " ".join(re.findall(r"[a-z0-9£$€]+", text.casefold()))


def _token_set(text: str) -> set[str]:
    return set(_normalise_text(text).split())


def _jaccard(left: str, right: str) -> float:
    left_tokens = _token_set(left)
    right_tokens = _token_set(right)
    if not left_tokens or not right_tokens:
        return 0.0
    return len(left_tokens & right_tokens) / len(left_tokens | right_tokens)


def _digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.exists() else ""


def _load_manifest(path: Path) -> tuple[dict, list[str]]:
    if not path.exists():
        return {}, [f"Manifest is missing: {path.name}."]
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError) as exc:
        return {}, [f"Manifest could not be read: {exc}."]
    if not isinstance(payload, dict):
        return {}, ["Manifest root must be a JSON object."]
    return payload, []


def validate_frozen_dataset(
    dataset_path: Path,
    manifest_path: Path,
    comparison_paths: Iterable[Path],
    minimum_per_class: int = DEFAULT_MINIMUM_PER_CLASS,
) -> DatasetGate:
    """Check provenance, labels, freeze metadata and development-set leakage."""

    errors: list[str] = []
    warnings: list[str] = []
    rows, columns = _read_csv(dataset_path)
    if not dataset_path.exists():
        errors.append(f"Final dataset is missing: {dataset_path.name}.")
    missing_columns = sorted(REQUIRED_COLUMNS - columns)
    if missing_columns:
        errors.append("Dataset is missing required columns: " + ", ".join(missing_columns) + ".")

    manifest, manifest_errors = _load_manifest(manifest_path)
    errors.extend(manifest_errors)
    dataset_sha256 = _digest(dataset_path)
    if manifest:
        if manifest.get("dataset_role") != FINAL_DATASET_ROLE:
            errors.append(f"Manifest dataset_role must be '{FINAL_DATASET_ROLE}'.")
        if manifest.get("status") != "frozen":
            errors.append("Manifest status must be 'frozen' before evaluation.")
        if not str(manifest.get("frozen_at", "")).strip():
            errors.append("Manifest requires the date and time when the dataset was frozen.")
        if manifest.get("data_sha256") != dataset_sha256:
            errors.append("Manifest data_sha256 does not match the CSV bytes.")
        if not str(manifest.get("dataset_version", "")).strip():
            errors.append("Manifest requires a non-empty dataset_version.")
        if not str(manifest.get("label_policy", "")).strip():
            errors.append("Manifest requires the label policy used by reviewers.")
        review = manifest.get("label_review", {})
        if not isinstance(review, dict) or review.get("status") != "adjudicated":
            errors.append("Labels must have an adjudicated review recorded in the manifest.")
        reviewer_count = int(review.get("reviewer_count", 0)) if isinstance(review, dict) else 0
        if reviewer_count < 1:
            errors.append("At least one recorded reviewer is required.")
        elif reviewer_count < 2:
            warnings.append("A second reviewer would strengthen label reliability evidence.")
        separation = manifest.get("separation", {})
        separation_checks = (
            "model_frozen_before_labels_opened",
            "not_used_for_prompt_or_rule_design",
            "not_used_for_threshold_selection",
        )
        for check in separation_checks:
            if not isinstance(separation, dict) or separation.get(check) is not True:
                errors.append(f"Manifest separation check '{check}' must be true.")

    class_counts = Counter()
    seen_ids: set[str] = set()
    normalised_rows: dict[str, str] = {}
    for index, row in enumerate(rows, start=2):
        case_id = row.get("case_id", "").strip()
        text = row.get("text", "").strip()
        label = row.get("label", "").strip().lower()
        if not case_id:
            errors.append(f"Row {index} has no case_id.")
        elif case_id in seen_ids:
            errors.append(f"Duplicate case_id: {case_id}.")
        seen_ids.add(case_id)
        if not text:
            errors.append(f"Row {index} has no message text.")
        if label not in ALLOWED_LABELS:
            errors.append(f"Row {index} has invalid label '{label}'.")
        else:
            class_counts[label] += 1
        for field in ("scenario", "provenance", "label_rationale"):
            if not row.get(field, "").strip():
                errors.append(f"Row {index} has no {field.replace('_', ' ')}.")
        normalised = _normalise_text(text)
        if normalised in normalised_rows:
            errors.append(
                f"Cases {normalised_rows[normalised]} and {case_id or index} contain duplicate normalised text."
            )
        elif normalised:
            normalised_rows[normalised] = case_id or str(index)

    for label in sorted(ALLOWED_LABELS):
        if class_counts[label] < minimum_per_class:
            errors.append(
                f"Class '{label}' has {class_counts[label]} cases; at least {minimum_per_class} are required."
            )

    reference_rows: list[tuple[str, str, str]] = []
    for path in comparison_paths:
        comparison, _ = _read_csv(path)
        for row in comparison:
            text = row.get("text", "").strip()
            if text:
                reference_rows.append((path.name, row.get("case_id", ""), text))

    overlap_cases: set[str] = set()
    for row in rows:
        case_id = row.get("case_id", "").strip() or "unidentified case"
        text = row.get("text", "").strip()
        normalised = _normalise_text(text)
        for source_name, _, reference_text in reference_rows:
            reference_normalised = _normalise_text(reference_text)
            if normalised and normalised == reference_normalised:
                overlap_cases.add(case_id)
                errors.append(f"{case_id} exactly overlaps development source {source_name}.")
                break
            if len(_token_set(text)) >= 6 and _jaccard(text, reference_text) >= 0.90:
                overlap_cases.add(case_id)
                errors.append(f"{case_id} is a near-duplicate of development source {source_name}.")
                break

    if manifest and manifest.get("row_count") != len(rows):
        errors.append("Manifest row_count does not match the CSV row count.")
    if len(rows) < 200:
        warnings.append(
            "Fewer than 200 cases limits subgroup analysis and the precision of confidence intervals."
        )

    return DatasetGate(
        ready=not errors,
        errors=tuple(dict.fromkeys(errors)),
        warnings=tuple(dict.fromkeys(warnings)),
        dataset_sha256=dataset_sha256,
        row_count=len(rows),
        class_distribution=dict(sorted(class_counts.items())),
        overlap_count=len(overlap_cases),
    )


def _metric_values(expected: list[int], predicted: list[int]) -> dict[str, float]:
    tp = sum(a == b == 1 for a, b in zip(expected, predicted))
    tn = sum(a == b == 0 for a, b in zip(expected, predicted))
    fp = sum(a == 0 and b == 1 for a, b in zip(expected, predicted))
    fn = sum(a == 1 and b == 0 for a, b in zip(expected, predicted))
    precision = tp / (tp + fp) if tp + fp else 0.0
    recall = tp / (tp + fn) if tp + fn else 0.0
    f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
    return {
        "accuracy": (tp + tn) / len(expected) if expected else 0.0,
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "false_positive_rate": fp / (fp + tn) if fp + tn else 0.0,
    }


def bootstrap_confidence_intervals(
    expected: list[int],
    predicted: list[int],
    iterations: int = BOOTSTRAP_ITERATIONS,
    seed: int = BOOTSTRAP_SEED,
) -> dict[str, dict[str, float]]:
    """Return deterministic percentile intervals for case-level metrics."""

    if len(expected) != len(predicted) or not expected:
        raise ValueError("Expected and predicted labels must have the same non-zero length.")
    rng = random.Random(seed)
    sampled: dict[str, list[float]] = {key: [] for key in _metric_values(expected, predicted)}
    for _ in range(iterations):
        indexes = [rng.randrange(len(expected)) for _ in expected]
        metrics = _metric_values(
            [expected[index] for index in indexes],
            [predicted[index] for index in indexes],
        )
        for key, value in metrics.items():
            sampled[key].append(value)
    intervals = {}
    lower_index = max(0, int(iterations * 0.025) - 1)
    upper_index = min(iterations - 1, int(iterations * 0.975))
    point = _metric_values(expected, predicted)
    for key, values in sampled.items():
        ordered = sorted(values)
        intervals[key] = {
            "estimate": round(point[key], 3),
            "lower_95": round(ordered[lower_index], 3),
            "upper_95": round(ordered[upper_index], 3),
        }
    return intervals


def run_final_evaluation(
    train_path: Path,
    dataset_path: Path,
    manifest_path: Path,
    comparison_paths: Iterable[Path],
    output_path: Path | None = None,
    minimum_per_class: int = DEFAULT_MINIMUM_PER_CLASS,
) -> dict:
    """Run only after the independent-data gate passes; never tune the model here."""

    gate = validate_frozen_dataset(
        dataset_path,
        manifest_path,
        comparison_paths,
        minimum_per_class=minimum_per_class,
    )
    if not gate.ready:
        raise ValueError("Final evaluation blocked:\n- " + "\n- ".join(gate.errors))
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    result = evaluate(
        train_path,
        dataset_path,
        dataset_role=FINAL_DATASET_ROLE,
        dataset_version=str(manifest["dataset_version"]),
    )
    expected = [int(record["expected"] == "phishing") for record in result["records"]]
    predicted = [int(record["actual"] == "phishing") for record in result["records"]]
    result["dataset"]["sha256"] = gate.dataset_sha256
    result["dataset"]["role"] = FINAL_DATASET_ROLE
    result["dataset"]["provenance_summary"] = manifest.get("provenance_summary", "")
    result["dataset_gate"] = gate.to_dict()
    result["confidence_intervals"] = {
        "method": "case bootstrap percentile interval",
        "confidence": 0.95,
        "iterations": BOOTSTRAP_ITERATIONS,
        "seed": BOOTSTRAP_SEED,
        "contextual": bootstrap_confidence_intervals(expected, predicted),
    }
    result["limitations"].append(
        "Confidence intervals reflect sampling variation within this frozen dataset, not every source of real-world uncertainty."
    )
    if output_path is not None:
        output_path.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    return result
