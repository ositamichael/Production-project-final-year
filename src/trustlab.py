"""Deterministic robustness experiments for ScamShield development data.

TrustLab applies documented transformations to the development challenge set,
runs both detector strategies on exactly the same cases and records failures.
It deliberately does not use the held-out evaluation set: that set must remain
separate from development and tuning.
"""

from __future__ import annotations

import csv
import hashlib
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Protocol

from .core import DECISION_THRESHOLD, MODEL_VERSION, train_from_csv
from .detectors import ContextualDetectorStrategy, DetectorStrategy, TfidfBaselineStrategy
from .evidence import EvidenceItem, EvidenceKind
from .repository import EvaluationRepository
from .services import StrategyComparisonService


TRUSTLAB_VERSION = "trustlab-v1"
TRANSFORMATION_VERSION = "language-transformations-v1"
DATASET_ROLE = "Development robustness suite; not independent final evaluation."


@dataclass(frozen=True)
class TransformedMessage:
    """One transformed case and its declared metamorphic relationship."""

    transformation: str
    relation: str
    expected_label: str
    text: str


class MessageTransformation(Protocol):
    name: str
    relation: str

    def apply(self, text: str, label: str) -> TransformedMessage | None: ...


class IdentityTransformation:
    name = "identity"
    relation = "preserve_label"

    def apply(self, text: str, label: str) -> TransformedMessage:
        return TransformedMessage(self.name, self.relation, label, text.strip())


class NeutralPaddingTransformation:
    name = "neutral_padding"
    relation = "preserve_label"

    def apply(self, text: str, label: str) -> TransformedMessage:
        return TransformedMessage(
            self.name, self.relation, label, f"Hello. {text.strip()} Thank you."
        )


class PoliteToneTransformation:
    name = "polite_tone"
    relation = "preserve_label"
    _replacements = (
        (re.compile(r"\bimmediately\b", re.I), "as soon as possible"),
        (re.compile(r"\bnow\b", re.I), "when you can"),
        (re.compile(r"\bplease\s+", re.I), "kindly "),
        (re.compile(r"\bsend\b", re.I), "please send"),
        (re.compile(r"\bpay\b", re.I), "please pay"),
        (re.compile(r"\bconfirm\b", re.I), "please confirm"),
        (re.compile(r"\bshare\b", re.I), "please share"),
    )

    def apply(self, text: str, label: str) -> TransformedMessage:
        transformed = text.strip()
        for pattern, replacement in self._replacements:
            transformed = pattern.sub(replacement, transformed)
        transformed = re.sub(r"\bplease\s+please\b", "please", transformed, flags=re.I)
        return TransformedMessage(self.name, self.relation, label, transformed)


class UrgencySofteningTransformation:
    name = "urgency_softening"
    relation = "preserve_label"
    _replacements = (
        (re.compile(r"\burgent(?:ly)?\b", re.I), "important"),
        (re.compile(r"\bimmediately\b", re.I), "soon"),
        (re.compile(r"\bwithin\s+\d+\s*(?:minutes?|hours?)\b", re.I), "soon"),
        (re.compile(r"\bin\s+the\s+next\s+\d+\s*(?:minutes?|hours?)\b", re.I), "soon"),
        (re.compile(r"\bbefore\s+midnight\b", re.I), "today"),
        (re.compile(r"\btonight\b", re.I), "later today"),
    )

    def apply(self, text: str, label: str) -> TransformedMessage:
        transformed = text.strip()
        for pattern, replacement in self._replacements:
            transformed = pattern.sub(replacement, transformed)
        return TransformedMessage(self.name, self.relation, label, transformed)


class FormattingVariationTransformation:
    name = "formatting_variation"
    relation = "preserve_label"

    def apply(self, text: str, label: str) -> TransformedMessage:
        transformed = re.sub(r"[,;:]\s*", " — ", text.strip())
        return TransformedMessage(
            self.name, self.relation, label, re.sub(r"\s+", " ", transformed)
        )


class AwarenessQuotationTransformation:
    """Convert a scam request into explicit security-awareness material."""

    name = "awareness_quotation"
    relation = "change_to_legitimate"

    def apply(self, text: str, label: str) -> TransformedMessage | None:
        if label != "phishing":
            return None
        transformed = (
            f'Security awareness example: scammers may write “{text.strip()}” '
            "Do not follow that instruction or share any information."
        )
        return TransformedMessage(self.name, self.relation, "legitimate", transformed)


class DeniedRequestTransformation:
    """Place a scam message inside an explicit refusal and verification context."""

    name = "denied_request_context"
    relation = "change_to_legitimate"

    def apply(self, text: str, label: str) -> TransformedMessage | None:
        if label != "phishing":
            return None
        transformed = (
            f'I received the message “{text.strip()}” but I did not act on it. '
            "I will not send money, share credentials or follow its link; I will verify independently."
        )
        return TransformedMessage(self.name, self.relation, "legitimate", transformed)


TRANSFORMATIONS: tuple[MessageTransformation, ...] = (
    IdentityTransformation(),
    NeutralPaddingTransformation(),
    PoliteToneTransformation(),
    UrgencySofteningTransformation(),
    FormattingVariationTransformation(),
    AwarenessQuotationTransformation(),
    DeniedRequestTransformation(),
)


def _prediction(detector_name: str, result: dict) -> tuple[str, float]:
    if detector_name == "contextual":
        score = float(result["risk_score"])
        return ("phishing" if score >= DECISION_THRESHOLD else "legitimate", score)
    score = float(result["score"])
    return str(result["prediction"]), score


def _metrics(records: list[dict]) -> dict:
    tp = sum(r["expected"] == r["predicted"] == "phishing" for r in records)
    tn = sum(r["expected"] == r["predicted"] == "legitimate" for r in records)
    fp = sum(r["expected"] == "legitimate" and r["predicted"] == "phishing" for r in records)
    fn = sum(r["expected"] == "phishing" and r["predicted"] == "legitimate" for r in records)
    precision = tp / (tp + fp) if tp + fp else 0.0
    recall = tp / (tp + fn) if tp + fn else 0.0
    f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
    return {
        "sample_count": len(records),
        "accuracy": round((tp + tn) / len(records), 3) if records else 0.0,
        "precision": round(precision, 3),
        "recall": round(recall, 3),
        "f1": round(f1, 3),
        "confusion_matrix": {
            "true_positive": tp,
            "true_negative": tn,
            "false_positive": fp,
            "false_negative": fn,
        },
    }


def transformation_manifest() -> list[dict[str, str]]:
    return [
        {"name": transformation.name, "relation": transformation.relation}
        for transformation in TRANSFORMATIONS
    ]


def run_trustlab(
    train_path: Path,
    seed_path: Path,
    repository: EvaluationRepository | None = None,
) -> dict:
    """Replay deterministic development transformations against both detectors."""

    if seed_path.name == "heldout_messages.csv":
        raise ValueError("TrustLab must not run transformations on the held-out evaluation set.")
    with seed_path.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    model = train_from_csv(train_path)
    detectors: list[DetectorStrategy] = [
        ContextualDetectorStrategy(model),
        TfidfBaselineStrategy(model),
    ]
    comparison = StrategyComparisonService(detectors)
    records: list[dict] = []
    identity_predictions: dict[tuple[str, str], str] = {}

    for index, row in enumerate(rows, start=1):
        label = row["label"].strip().lower()
        scenario = row["scenario"].strip()
        source_key = f"challenge-{index:03d}"
        for transformation in TRANSFORMATIONS:
            transformed = transformation.apply(row["text"], label)
            if transformed is None:
                continue
            evidence = EvidenceItem(
                text=transformed.text,
                kind=EvidenceKind.TEXT,
                source_label="challenge_evaluation",
            )
            compared = comparison.compare(evidence)
            for detector_name, detector_result in compared.items():
                predicted, score = _prediction(detector_name, detector_result)
                if transformed.transformation == "identity":
                    identity_predictions[(source_key, detector_name)] = predicted
                original_prediction = identity_predictions[(source_key, detector_name)]
                records.append(
                    {
                        "case_key": f"{source_key}-{transformed.transformation}-{detector_name}",
                        "source_key": source_key,
                        "scenario": scenario,
                        "detector": detector_name,
                        "transformation": transformed.transformation,
                        "relation": transformed.relation,
                        "expected": transformed.expected_label,
                        "predicted": predicted,
                        "score": round(score, 3),
                        "correct": predicted == transformed.expected_label,
                        "changed_from_original": predicted != original_prediction,
                        "text_sha256": hashlib.sha256(transformed.text.encode("utf-8")).hexdigest(),
                    }
                )

    detector_summaries = {}
    for detector in ("contextual", "tfidf_baseline"):
        detector_records = [record for record in records if record["detector"] == detector]
        by_transformation = {}
        for item in transformation_manifest():
            selected = [
                record for record in detector_records
                if record["transformation"] == item["name"]
            ]
            if selected:
                by_transformation[item["name"]] = _metrics(selected)
        preserved = [record for record in detector_records if record["relation"] == "preserve_label"]
        detector_summaries[detector] = {
            "overall": _metrics(detector_records),
            "label_preserving": _metrics(preserved),
            "prediction_stability": round(
                sum(not record["changed_from_original"] for record in preserved) / len(preserved), 3
            ) if preserved else 0.0,
            "by_transformation": by_transformation,
        }

    manifest = transformation_manifest()
    result = {
        "suite": {
            "version": TRUSTLAB_VERSION,
            "transformation_version": TRANSFORMATION_VERSION,
            "dataset_role": DATASET_ROLE,
            "seed_dataset": f"data/{seed_path.name}",
            "seed_sha256": hashlib.sha256(seed_path.read_bytes()).hexdigest(),
            "seed_case_count": len(rows),
            "transformed_case_count": len({(r["source_key"], r["transformation"]) for r in records}),
            "transformation_manifest_sha256": hashlib.sha256(
                json.dumps(manifest, sort_keys=True).encode("utf-8")
            ).hexdigest(),
        },
        "model_version": MODEL_VERSION,
        "decision_threshold": DECISION_THRESHOLD,
        "transformations": manifest,
        "detectors": detector_summaries,
        "records": records,
        "failure_examples": [record for record in records if not record["correct"]],
        "limitations": [
            "TrustLab uses author-created development cases and is not an independent final evaluation.",
            "Automatic transformations approximate language variation and do not represent real-world prevalence.",
            "A stable prediction can still be wrong; stability is reported alongside correctness.",
            "Failures are recorded for analysis and must not be silently removed or tuned against the final test set.",
        ],
    }
    if repository is not None:
        repository.record_trustlab(result)
    return result


if __name__ == "__main__":
    root = Path(__file__).resolve().parents[1]
    output = run_trustlab(
        root / "data" / "sample_messages.csv",
        root / "data" / "challenge_messages.csv",
    )
    (root / "data" / "trustlab_results.json").write_text(
        json.dumps(output, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps({name: values["overall"] for name, values in output["detectors"].items()}, indent=2))
