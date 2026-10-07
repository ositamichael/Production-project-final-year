# Sprint 2 — Architecture and data engineering

**Start:** 7 October 2026  
**Status:** In progress  
**Branch:** `feat/sprint-2-architecture-data`

## Sprint goal

Separate evidence ingestion, detection and persistence behind tested contracts, and introduce a privacy-preserving SQLite research store without changing the live detector's measured behaviour.

## Backlog and acceptance criteria

- [x] Implement a Detector Strategy contract with contextual and TF-IDF baseline strategies.
- [x] Implement Evidence Adapters for pasted text, reviewed screenshot OCR and reviewed recording OCR.
- [x] Implement an Analysis Service that coordinates the strategy and repository.
- [x] Implement a Repository contract with stateless and SQLite implementations.
- [x] Add a versioned SQLite migration and indexes.
- [x] Prove through tests that raw message text is not stored.
- [x] Integrate the service into the existing `/api/analyse` path without changing contextual results.
- [x] Verify imports using Render's direct-script execution mode.
- [x] Add an ERD and data-retention boundary.
- [x] Run baseline and contextual evaluation through one controlled strategy-comparison service.
- [x] Add optional relational persistence for versioned evaluation runs and per-case outcomes without source text.
- [x] Raise the branch-coverage gate from 60% to 70% after architecture tests lift measured coverage above 72%.
- [x] Confirm the 70% quality gate on a remote feature-branch CI run.
- [ ] Record the final sprint review and complete the retrospective.

## Demonstrable increment

The same reviewed message can be represented through text, screenshot-OCR or recording-OCR adapters and analysed by interchangeable detector strategies. Live operation remains stateless by default. When `SCAMSHIELD_RESEARCH_DB` is explicitly configured, SQLite stores only a SHA-256 digest, length, source kind, detector version and result metadata—not raw message text or media.

## Risks and controls

- A digest is still a derived identifier, so the optional database is research metadata and must not be presented as anonymous user analytics without further governance.
- SQLite is appropriate for a single-instance academic prototype, not a horizontally scaled service.
- The strategy boundary makes comparison possible; it does not by itself prove one detector is superior.
- Browser OCR quality remains dependent on image quality and user review.

## Review evidence

- Architecture tests compare the new contextual strategy with the existing `explain` result fields.
- Migration tests prove idempotence and inspect the schema for the absence of a raw `text` column.
- A deployment-mode import test simulates `python src/server.py` module resolution.
- The complete local suite contains 47 Python tests, and measured branch coverage is 74.1% against a 70% gate.
- GitHub Actions run [37597825492](https://github.com/ositamichael/Production-project-final-year/actions/runs/37597825492) completed successfully for commit `f06fe54` on the Sprint 2 feature branch.
- Full local suite and remote CI results will be recorded before sprint close.

## Retrospective

To be completed at sprint close.
