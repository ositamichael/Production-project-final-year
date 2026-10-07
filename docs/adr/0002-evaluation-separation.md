# ADR 0002: Keep measured, development, synthetic and public data separate

- **Status:** Accepted
- **Date:** 7 October 2026

## Context

The application contains a small measured held-out set, a development challenge set, synthetic demonstrations and published public context. Combining these would create misleading performance claims.

## Decision

Maintain separate versioned sources and visible labels for:

1. held-out measured evaluation;
2. development/regression cases;
3. synthetic demonstrations;
4. published public background context.

Evaluation verification compares model version, threshold, dataset metadata, metrics and case-level decisions. A generated date is metadata and is excluded from reproducibility equality.

## Consequences

- Dashboard claims can be traced to a specific source.
- Development fixes cannot quietly be presented as independent confirmation.
- Future dataset and model changes require reviewed export changes and documented provenance.
