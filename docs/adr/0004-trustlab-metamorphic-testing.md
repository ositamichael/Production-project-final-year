# ADR 0004: Use deterministic metamorphic testing for TrustLab

- **Status:** Accepted
- **Date:** 7 October 2026

## Context

The held-out evaluation has only 12 messages and must not become a development target. Ordinary unit tests prove known cases, but they do not show whether small changes in tone, padding, urgency or framing alter the detector unexpectedly. A robustness method is needed that is reproducible, explainable and appropriate for the available data.

## Decision

TrustLab uses deterministic metamorphic transformations over the separate 24-message development challenge set. Every transformation declares one of two relationships:

- `preserve_label`: the message meaning should remain in the same class;
- `change_to_legitimate`: an original scam request is embedded in explicit awareness or refusal context and should be treated as legitimate discussion.

The contextual and TF-IDF strategies receive exactly the same transformed evidence. Reports keep correctness separate from prediction stability and retain all failures. A SHA-256 digest binds each result to its transformed text without duplicating that text in the export or optional database.

## Consequences

The method gives repeatable evidence about selected language variations and reveals failure modes that a perfect score on the small challenge set hides. It does not estimate real-world prevalence, prove general robustness or replace a genuinely independent final evaluation. Because the transformations and challenge examples are development artefacts, future fixes influenced by them must be versioned and must not be described as independent validation.
