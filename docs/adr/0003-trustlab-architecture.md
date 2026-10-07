# ADR 0003: Use Strategy, Adapter and Repository boundaries for TrustLab

- **Status:** Proposed
- **Date:** 7 October 2026

## Context

TrustLab must compare detection methods, process several evidence forms and replay cases against model versions. Adding these features directly to the existing modules would increase coupling and make evaluation harder to isolate.

## Proposed decision

- Use a Detector Strategy contract for comparable models.
- Use Evidence Adapters to create one validated evidence representation.
- Use a Case Repository contract with SQLite as the first implementation.
- Keep raw private media outside persistent storage unless consent and deletion requirements are designed and approved.

## Acceptance condition

This ADR becomes accepted only after Sprint 2 implements the contracts, ERD, schema migration and tests. Until then, it describes the target architecture rather than current behaviour.
