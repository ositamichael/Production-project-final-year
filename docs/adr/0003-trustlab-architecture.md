# ADR 0003: Use Strategy, Adapter and Repository boundaries for TrustLab

- **Status:** Accepted
- **Date:** 7 October 2026

## Context

TrustLab must compare detection methods, process several evidence forms and replay cases against model versions. Adding these features directly to the existing modules would increase coupling and make evaluation harder to isolate.

## Decision

- Use a Detector Strategy contract for comparable models.
- Use Evidence Adapters to create one validated evidence representation.
- Use an Analysis Repository contract with a stateless default and SQLite as the optional research implementation.
- Keep raw private media outside persistent storage unless consent and deletion requirements are designed and approved.

## Evidence and consequences

Sprint 2 implements the contracts in `src/evidence.py`, `src/detectors.py`, `src/services.py` and `src/repository.py`. The versioned migration is under `migrations/`; the ERD and retention boundary are in `docs/data_model.md`; tests cover strategy equivalence, adapter validation, migration idempotence and absence of raw message storage.

The additional modules increase structure and testing surface, but remove server coupling and make future controlled comparisons possible. SQLite remains disabled unless explicitly configured and is not suitable for horizontally scaled production deployment.
