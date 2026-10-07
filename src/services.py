"""Application services that coordinate evidence, detection and persistence."""

from __future__ import annotations

try:
    from .detectors import DetectorStrategy
    from .evidence import EvidenceItem
    from .repository import AnalysisRepository, NullAnalysisRepository
except ImportError:  # Supports the Render `python src/server.py` entry point.
    from detectors import DetectorStrategy
    from evidence import EvidenceItem
    from repository import AnalysisRepository, NullAnalysisRepository


class AnalysisService:
    """Analyse evidence with one strategy and record permitted metadata."""

    def __init__(
        self,
        detector: DetectorStrategy,
        repository: AnalysisRepository | None = None,
    ):
        self.detector = detector
        self.repository = repository or NullAnalysisRepository()

    @property
    def persistence_status(self) -> str:
        return self.repository.status

    def analyse(self, evidence: EvidenceItem) -> dict:
        result = self.detector.analyse(evidence)
        self.repository.record(evidence, result)
        return result


class StrategyComparisonService:
    """Run named strategies against exactly the same evidence item."""

    def __init__(self, detectors: list[DetectorStrategy]):
        if len(detectors) < 2:
            raise ValueError("A controlled comparison requires at least two detector strategies.")
        names = [detector.name for detector in detectors]
        if len(names) != len(set(names)):
            raise ValueError("Detector strategy names must be unique within a comparison.")
        self.detectors = tuple(detectors)

    def compare(self, evidence: EvidenceItem) -> dict[str, dict]:
        return {detector.name: detector.analyse(evidence) for detector in self.detectors}
