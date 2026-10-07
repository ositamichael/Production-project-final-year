# Sprint 3 — TrustLab robustness and replay

**Start:** 7 October 2026  
**Status:** Complete — merged in PR #3
**Branch:** `feat/sprint-3-trustlab`

## Sprint goal

Measure how the contextual detector and TF-IDF baseline respond to controlled language variation, while keeping development experiments separate from the final held-out evaluation.

## Backlog and acceptance criteria

- [x] Define deterministic, versioned text transformations with explicit expected-label relationships.
- [x] Test neutral padding, polite language, softened urgency and formatting variation without changing the source label.
- [x] Test quoted-scam and denied-request contexts as deliberate changes to legitimate awareness material.
- [x] Run contextual and TF-IDF strategies on exactly the same transformed cases.
- [x] Report correctness, precision, recall, F1, confusion counts and prediction stability by transformation.
- [x] Reject use of `heldout_messages.csv` as a TrustLab development seed.
- [x] Create a deterministic JSON replay export and CI verification command.
- [x] Add privacy-preserving relational replay storage without raw transformed text.
- [x] Review failures without tuning against the held-out set.
- [x] Record remote CI evidence.
- [x] Complete the sprint retrospective after pull-request review.

## Demonstrable increment

`python -m src.trustlab` creates a replayable robustness report from the 24-message development challenge set. Seven declared transformations produce 144 message variants and 288 paired detector outcomes. The report distinguishes label-preserving transformations from awareness/denial contexts that intentionally change the expected label.

The export records hashes rather than duplicated transformed text. When optional SQLite research storage is enabled, TrustLab runs and case outcomes are linked relationally through migration `003_trustlab_replays.sql`; raw messages and media are excluded.

## Evidence rules

- The challenge set is development regression data, not independent evidence.
- A stable prediction is not automatically a correct prediction.
- Automatic transformations approximate selected language variations; they do not represent real-world prevalence.
- Failures remain in the report and must not be silently removed.
- The frozen held-out set is not used for transformation design or Sprint 3 tuning.

## Remaining review work

The generated failure list must be examined before Sprint 3 closes. Any detector change requires a new model version, updated regression tests and a clear statement that the development suite influenced the change. Final independent evaluation belongs to Sprint 4.

## Initial observed result

The first deterministic replay produced 144 transformed messages and 288 paired detector outcomes:

| Detector | Accuracy | Precision | Recall | F1 | False alarms | Missed scams |
|---|---:|---:|---:|---:|---:|---:|
| Contextual | 0.889 | 0.789 | 1.000 | 0.882 | 16 | 0 |
| TF-IDF baseline | 0.674 | 0.570 | 0.883 | 0.693 | 40 | 7 |

All 16 contextual failures were false alarms in label-changing awareness or denial contexts: 12 in `denied_request_context` and four in `awareness_quotation`. This is a useful failure finding, not evidence of an 88.9% real-world accuracy rate. The label-preserving and per-transformation results remain available in `data/trustlab_results.json` for review.

The full interpretation and decision not to tune against these cases are recorded in [`docs/trustlab_failure_analysis.md`](../trustlab_failure_analysis.md).

## Verification evidence

- 53 Python tests passed locally.
- Branch coverage: 78.5% against the 70% gate.
- Browser-side conversation tests passed.
- Held-out and challenge evaluation exports reproduced unchanged.
- TrustLab replay reproduced exactly.
- GitHub Actions run [37601965643](https://github.com/ositamichael/Production-project-final-year/actions/runs/37601965643) completed successfully for commit `b810fcf` on the Sprint 3 branch.
- Pull request [#3](https://github.com/ositamichael/Production-project-final-year/pull/3) merged to `main` as commit `70c0fa1` after two successful checks.

## Retrospective

**What worked:** explicit metamorphic relationships prevented prediction stability from being mistaken for correctness. Text-free replay records also gave useful evidence without duplicating the messages in storage.

**What was learned:** the strongest contextual detector failure was not missed scams but false alarms on denials and awareness quotations. Because those cases were now visible during development, they were retained as known failures rather than used to tune the final test.

**What changes next:** Sprint 4 adds a hard dataset gate. A final result cannot be generated until the dataset is frozen, labelled under a written policy, checked for overlap with every development source and large enough for both classes.
