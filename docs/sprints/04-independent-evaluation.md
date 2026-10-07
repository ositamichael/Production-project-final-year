# Sprint 4 — Independent evaluation and submission evidence

**Start:** 7 October 2026
**Status:** In progress
**Branch:** `feat/sprint-4-independent-evaluation`

## Sprint goal

Produce final evidence that is reproducible, clearly separated from development and honest about uncertainty. Complete accessibility and submission checks without turning the existing 12-message demonstration into a reliability claim.

## Backlog and acceptance criteria

- [x] Add a machine-enforced gate for dataset freeze state, provenance and label review.
- [x] Reject exact and near-duplicate overlap with training, challenge and earlier held-out sources.
- [x] Require at least 50 phishing and 50 legitimate cases before the final run.
- [x] Record the dataset byte digest and reject a stale manifest.
- [x] Preserve fixed model version and decision threshold during the final run.
- [x] Add deterministic 95% bootstrap intervals for contextual metrics.
- [x] Add CI behaviour that reports “pending” while no final dataset exists and fails on partial evidence.
- [ ] Obtain and document the independently sourced candidate cases.
- [ ] Apply the written label policy and record label adjudication.
- [ ] Freeze the CSV and manifest before opening final predictions.
- [ ] Run the contextual and TF-IDF methods once on the frozen data.
- [ ] Record subgroup counts, failure cases and limitations without tuning on the result.
- [ ] Complete automated and manual accessibility checks on desktop, tablet and mobile.
- [ ] Capture scanner, conversation, Evidence Lab and dashboard submission screenshots.
- [ ] Complete the final retrospective and release checklist.

## Current demonstrable increment

`src/final_evaluation.py` provides the independent-evaluation gate and deterministic confidence-interval calculation. `scripts/verify_final_evaluation.py` protects the repository against two misleading states:

1. no final dataset yet — CI reports an explicit pending state and no metric is invented;
2. only part of the evidence exists — CI fails because the CSV, freeze manifest and result export must appear together.

The templates in `data/` describe the required case-level provenance and freeze metadata. The final CSV is intentionally absent at this stage.

## Independence protocol

1. Complete detector changes and freeze `MODEL_VERSION` plus `DECISION_THRESHOLD`.
2. Collect candidate messages from documented sources without copying development examples.
3. Remove personal data and assign opaque case identifiers.
4. Label against the written policy before running the model.
5. Resolve disagreements and record reviewer count and method.
6. Run the leakage gate against training, challenge and prior held-out files.
7. Freeze the exact CSV bytes and place their SHA-256 digest in the manifest.
8. Run the final evaluation once; do not tune on failures and rerun under the same version.

## Evidence boundary

- The 12-message held-out result remains a small prototype evaluation.
- TrustLab remains development robustness evidence.
- Templates and test fixtures are not evaluation evidence.
- Only a gate-approved frozen dataset may populate final metrics.
