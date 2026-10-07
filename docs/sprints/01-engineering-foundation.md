# Sprint 1 — Engineering foundation

**Start:** 7 October 2026  
**Status:** In progress  
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
- [ ] Confirm the workflow on a remote feature-branch push.
- [ ] Record sprint review evidence and retrospective.

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
- Remote workflow confirmation remains outstanding until this branch is pushed.

## Retrospective

To be completed at sprint close. Record what worked, what did not, what changed and the next highest-risk item.
