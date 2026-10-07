"""Validated evidence representations and adapters.

Raw screenshots and recordings remain in the browser.  The server receives only
text that the user has reviewed, together with a small source-kind label.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Mapping, Protocol


ALLOWED_SOURCE_LABELS = frozenset(
    {
        "pasted_text",
        "browser_reviewed_screenshot_ocr",
        "browser_reviewed_recording_ocr",
        "synthetic_pre_supplied_demo_text",
        "heldout_evaluation",
        "challenge_evaluation",
    }
)


class EvidenceKind(str, Enum):
    TEXT = "text"
    OCR_TEXT = "ocr_text"
    RECORDING_TEXT = "recording_text"


@dataclass(frozen=True)
class EvidenceItem:
    """One validated, analysis-ready piece of textual evidence."""

    text: str
    kind: EvidenceKind
    source_label: str
    metadata: Mapping[str, str] = field(default_factory=dict)


class EvidenceAdapter(Protocol):
    """Convert an input payload into the common evidence representation."""

    def adapt(self, payload: Mapping[str, object]) -> EvidenceItem: ...


class _TextFieldAdapter:
    field_name = "text"
    kind = EvidenceKind.TEXT
    default_source = "pasted_text"

    def adapt(self, payload: Mapping[str, object]) -> EvidenceItem:
        value = payload.get(self.field_name, "")
        if not isinstance(value, str):
            raise ValueError("The reviewed evidence must be supplied as text.")
        text = value.strip()
        if len(text) < 12:
            raise ValueError("Please enter a longer message so the system has enough context to analyse it.")
        if len(text) > 4000:
            raise ValueError("Please keep the message below 4,000 characters for this prototype.")
        requested_source = str(payload.get("source_label") or self.default_source).strip()
        source = requested_source if requested_source in ALLOWED_SOURCE_LABELS else self.default_source
        return EvidenceItem(text=text, kind=self.kind, source_label=source)


class TextEvidenceAdapter(_TextFieldAdapter):
    """Adapt text pasted or typed directly into the scanner."""


class OcrTextEvidenceAdapter(_TextFieldAdapter):
    """Adapt user-reviewed text extracted from a screenshot in the browser."""

    field_name = "ocr_text"
    kind = EvidenceKind.OCR_TEXT
    default_source = "browser_reviewed_screenshot_ocr"


class RecordingTextEvidenceAdapter(_TextFieldAdapter):
    """Adapt user-reviewed text combined from sampled recording frames."""

    field_name = "recording_text"
    kind = EvidenceKind.RECORDING_TEXT
    default_source = "browser_reviewed_recording_ocr"


def adapter_for(kind: str) -> EvidenceAdapter:
    """Return the adapter for a declared evidence kind."""

    adapters: dict[str, EvidenceAdapter] = {
        EvidenceKind.TEXT.value: TextEvidenceAdapter(),
        EvidenceKind.OCR_TEXT.value: OcrTextEvidenceAdapter(),
        EvidenceKind.RECORDING_TEXT.value: RecordingTextEvidenceAdapter(),
    }
    try:
        return adapters[kind]
    except KeyError as error:
        raise ValueError("Unsupported evidence type. Use text, screenshot OCR or recording OCR text.") from error
