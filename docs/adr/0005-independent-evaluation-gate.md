# ADR 0005 — Gate the final evaluation with a frozen manifest

**Status:** Accepted
**Date:** 7 October 2026

## Context

The repository contains a 12-message held-out file and a 24-message development challenge file. The first is too small for a general reliability claim; the second has already influenced debugging and is therefore not independent. Generating more author-written messages and calling them final would preserve the same validity problem.

## Decision

The final evaluator is fail-closed. It runs only when a candidate CSV is accompanied by a manifest that records:

- the final-evaluation role and version;
- the exact SHA-256 digest and row count;
- the freeze date;
- the label policy and adjudication status;
- confirmation that the frozen data did not influence rules, prompts or threshold selection.

The gate additionally requires at least 50 cases per class and rejects duplicate or near-duplicate overlap with training, challenge and prior held-out data. The result includes deterministic bootstrap intervals. Until the gate passes, the repository reports “final evaluation pending” and publishes no final metric.

## Consequences

- A visually complete dashboard cannot be used to imply final evidence exists.
- Dataset collection and review become explicit assessed work rather than an invisible spreadsheet step.
- The 100-case minimum improves on the current 12 cases but still does not establish population-level reliability; that limitation remains mandatory.
- A detector change after the freeze requires a new model version and a separately versioned evaluation, not silent replacement of the first result.
