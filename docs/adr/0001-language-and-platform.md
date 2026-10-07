# ADR 0001: Retain Python and browser JavaScript for the research prototype

- **Status:** Accepted
- **Date:** 7 October 2026

## Context

ScamShield already contains a NumPy-based transparent classifier, Python evaluation scripts and browser-based media/OCR interaction. Rewriting the system would consume the remaining project time without directly answering the research question.

## Decision

Retain Python for classification, evaluation and server-side endpoints. Retain JavaScript for browser interaction and privacy-sensitive local media processing. Continue with semantic HTML and CSS for the interface.

## Consequences

- Existing evaluation remains reproducible and readable.
- Browser APIs support local preview and recording-frame sampling.
- Dynamic-language errors require disciplined tests, type-aware design and validation.
- A framework or typed service may become appropriate if authentication, persistent accounts or a public API enters scope.
