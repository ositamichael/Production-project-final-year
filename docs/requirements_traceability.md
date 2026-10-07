# Requirements traceability matrix

This matrix connects the final-year expectations to deliverables and verifiable evidence. Status values are **implemented**, **in progress** or **planned**; they are not marks or claims of quality.

| Expectation | ScamShield response | Evidence | Status |
|---|---|---|---|
| Version control | Incremental Git history; feature-branch and pull-request workflow from Sprint 1 | Merged PR #1, Git history, `CONTRIBUTING.md`, pull-request template | Implemented |
| Unit testing | Python detector, dashboard and evaluation tests; JavaScript conversation tests | `tests/`, CI workflow | Implemented |
| Debugger use | Repeatable launch configurations and issue-based debugging records | `.vscode/launch.json`, `docs/debugging_log.md` | In progress |
| Agile methodology | Four time-boxed sprints with acceptance criteria, review and retrospective | `docs/sprints/` and GitHub project evidence | In progress |
| Language justification | Python, JavaScript and web-platform choices compared with alternatives | `docs/software_engineering_plan.md` | Implemented |
| Design patterns | Strategy for detectors, Adapter for evidence and Repository for persistence | `src/detectors.py`, `src/evidence.py`, `src/repository.py`, architecture tests | Implemented |
| Working artefact | Scanner, Evidence Lab and Research Dashboard | Deployed application, source and verification screenshots | Implemented |
| Artificial intelligence | Transparent TF-IDF baseline plus contextual analysis; future controlled comparison | Model card, evaluation exports and TrustLab | In progress |
| Data engineering | Versioned SQLite schema, migration register, relational evaluation runs, privacy boundary and ERD | `migrations/`, `docs/data_model.md`, repository tests | Implemented |
| Dashboard | Measured results separated from synthetic demonstrations and public context | Dashboard source and versioned JSON | Implemented |
| Method comparison | Baseline and contextual model on the same frozen evaluation source | `src/evaluate.py`, dashboard and verification script | Implemented with small-data limitation |
| Technical depth | Robustness transformations and versioned evidence replay | TrustLab Sprint 3 evidence | Planned |
| Professional quality | CI, branch coverage, accessibility checks, ADRs and release evidence | Workflow, reports, logs and sprint records | In progress |

## Current priority gaps

1. The independent evaluation set is too small for a general reliability claim.
2. Historical development predates the formal branch and sprint workflow.
3. Automated accessibility and end-to-end browser checks are not yet present.
