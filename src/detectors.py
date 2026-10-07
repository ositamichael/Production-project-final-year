"""Detector strategies used by ScamShield application services."""

from __future__ import annotations

from typing import Protocol

try:
    from .core import DECISION_THRESHOLD, MODEL_VERSION, ScamShieldModel, explain
    from .evidence import EvidenceItem
except ImportError:  # Supports the Render `python src/server.py` entry point.
    from core import DECISION_THRESHOLD, MODEL_VERSION, ScamShieldModel, explain
    from evidence import EvidenceItem


class DetectorStrategy(Protocol):
    """Common contract for controlled detector comparison."""

    name: str
    model_version: str

    def analyse(self, evidence: EvidenceItem) -> dict: ...


class ContextualDetectorStrategy:
    """Current explainable contextual detector used by the live scanner."""

    name = "contextual"
    model_version = MODEL_VERSION

    def __init__(self, model: ScamShieldModel):
        self.model = model

    def analyse(self, evidence: EvidenceItem) -> dict:
        probability = self.model.predict_probability(evidence.text)
        result = explain(evidence.text, probability, self.model)
        result["detector"] = self.name
        result["evidence_kind"] = evidence.kind.value
        return result


class TfidfBaselineStrategy:
    """Transparent lexical baseline for research comparison only."""

    name = "tfidf_baseline"
    model_version = f"{MODEL_VERSION}-tfidf-only"

    def __init__(self, model: ScamShieldModel, threshold: float = 0.5):
        self.model = model
        self.threshold = threshold

    def analyse(self, evidence: EvidenceItem) -> dict:
        raw_score = self.model.predict_probability(evidence.text)
        score = round(raw_score, 3)
        return {
            "detector": self.name,
            "model_version": self.model_version,
            "prediction": "phishing" if raw_score >= self.threshold else "legitimate",
            "score": score,
            "threshold": self.threshold,
            "top_terms": [
                {"term": term, "contribution": round(contribution, 4)}
                for term, contribution in self.model.top_contributors(evidence.text)
            ],
            "evidence_kind": evidence.kind.value,
            "meaning": "TF-IDF-only research baseline; not a safety verdict.",
        }


def decision_threshold() -> float:
    """Expose the contextual threshold without duplicating its value."""

    return DECISION_THRESHOLD
