# Sprint 1 — Engineering foundation

**Start:** 7 October 2026  
**End:** 7 October 2026
**Status:** Complete
**Branch:** `feat/sprint-1-engineering-foundation`

## Sprint goal

Make ScamShield's development process reproducible and assessor-visible without changing measured model claims or destabilising the deployed prototype.

## Backlog and acceptance criteria

- [x] Begin work on a dedicated feature branch from the recorded working build.
- [x] Document the methodology, language choices, alternatives and evidence rules.
- [x] Add automated Python and browser-logic tests on pushes and pull requests.
- [x] Add branch-coverage reporting with a documented initial quality gate.
- [x] Verify that checked-in evaluation decisions and metrics reproduce while excluding run-date metadata.
- [x] Add pull-request and definition-of-done controls.
- [x] Add debugger launch configurations and begin an honest debugging log.
- [x] Map university expectations to implementation evidence and current gaps.
- [x] Run the full clean-environment suite locally.
- [x] Confirm the workflow on a remote feature-branch push.
- [x] Record sprint review evidence and retrospective.

## Demonstrable increment

A fresh environment can install declared dependencies, run all automated checks, report branch coverage and detect an unexplained change to versioned evaluation results.

## Risks

- Coverage may expose untested server paths; the initial threshold is deliberately a ratchet and must increase over later sprints.
- The current 12-message held-out set remains too small for a real-world reliability claim.
- Adding process documentation does not substitute for implementing the planned architecture and broader evaluation.

## Review evidence

- A fresh `.venv` installed NumPy, certifi and coverage from the declared requirement files.
- All 32 Python tests passed.
- Browser-side conversation tests passed.
- Measured branch coverage was 65.5%; the CI quality gate is set to 60% as an initial ratchet.
- The first standalone evaluation-verifier run found an import-path defect. The correction and evidence are recorded in `docs/debugging_log.md`.
- GitHub Actions completed successfully for feature-branch commits `9cdb1f7` and `f7758fd`.
- Pull request [#1](https://github.com/ositamichael/Production-project-final-year/pull/1) passed two checks with no merge conflict and was merged into `main` as `63c42f8`.

## Retrospective

The small dependency set made clean-environment reproduction fast, while the evaluation verifier exposed an import-path defect before it reached CI. The initial feature commit still carried historical author metadata from the earlier repository; repository-local identity was corrected before the next commit without rewriting history. The strongest improvement was moving claims into testable commands and versioned records. The next highest-risk item is architectural coupling: the server currently coordinates validation, detection and delivery directly, so Sprint 2 introduces explicit Strategy, Adapter and Repository boundaries while preserving detector outputs.
