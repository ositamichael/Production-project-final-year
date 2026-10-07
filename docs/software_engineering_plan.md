# ScamShield software engineering plan

## Purpose

This plan turns the existing ScamShield prototype into a traceable Level 6 software-engineering project. It records the process from 7 October 2026 onward. It does not rewrite the history of earlier prototype work.

## Methodology

The project uses a lightweight Agile approach with four time-boxed sprints. Each sprint has a goal, acceptance criteria, a demonstrable increment and a retrospective. GitHub branches and pull requests provide the technical evidence; sprint records explain decisions and incomplete work.

### Roles and decision process

The project is individually owned, so the student performs product, development and test responsibilities. Feedback from tutors or reviewers becomes a traceable backlog item. A suggestion is not treated as implemented until code, tests and observed behaviour support it.

### Sprint structure

1. **Engineering foundation:** branch workflow, CI, coverage, debugger workflow, traceability and architecture records.
2. **Architecture and data:** detector strategies, evidence adapters, repository abstraction, SQLite schema and ERD.
3. **TrustLab:** controlled language transformations, detector comparison and versioned replay.
4. **Independent evaluation:** frozen data, final metrics, failure analysis, accessibility verification and submission evidence.

Sprint 4 uses a machine-enforced entry gate. The final evaluator refuses to run unless the dataset is frozen, its digest matches the manifest, labels have been reviewed, both classes meet the minimum sample count and no exact or near-duplicate development overlap is found.

## Language and platform decisions

### Python

Python is retained for model training, contextual analysis, evaluation and the prototype server because its numerical ecosystem supports the existing NumPy classifier and its readable syntax makes research logic auditable. It also permits a dependency-light implementation appropriate to the project timeframe.

### JavaScript

JavaScript is used for interactive browser behaviour, conversation analysis, local media preview, frame sampling and OCR coordination. Keeping privacy-sensitive media handling in the browser supports the project's current capability boundary.

### HTML and CSS

Semantic HTML and CSS provide a lightweight interface with direct control over accessibility, responsive behaviour and reduced-motion support.

### Alternatives considered

- **Java or C#:** strong typing and mature enterprise tooling, but migration would add risk without improving the central research question during the remaining project period.
- **A large Python web framework:** FastAPI or Flask could provide routing and validation as the system grows. The current standard-library server remains suitable for the controlled prototype, but this decision will be reviewed when persistence and user sessions are introduced.
- **A transformer model:** potentially stronger language representation, but it must be evaluated against the transparent baseline rather than assumed to be better. Hardware, explainability, dataset size and reproducibility are constraints.

## Engineering controls

- Short-lived feature and fix branches.
- Pull-request checklist and automated quality checks.
- Unit and integration tests in Python plus browser-logic tests in JavaScript.
- Branch coverage with an initial minimum threshold that is raised as architecture is added.
- Reproducibility check for versioned evaluation exports.
- Debugger configurations and an evidence-based debugging log.
- Architecture Decision Records for consequential choices.
- A requirements traceability matrix linking expectations to implementation and evidence.

## Quality attributes

1. **Correctness:** warnings must be supported by exact input evidence and measured results must reproduce.
2. **Privacy:** raw uploaded media remains local unless a future design explicitly changes the boundary with consent and controls.
3. **Explainability:** classifications expose observable signals and limitations.
4. **Accessibility:** keyboard operation, contrast, labels, status announcements and reduced motion are verified.
5. **Security:** public URL inspection rejects private destinations and never presents reachability as a safety verdict.
6. **Maintainability:** detection, evidence ingestion and persistence are separated through explicit interfaces in the architecture sprint.

## Evidence rules

- Do not claim the historical prototype used branches or formal sprints.
- Do not describe development regression data as independent evaluation.
- Do not convert synthetic demonstrations into performance claims.
- Do not report a check as passed unless its command or manual observation is recorded.
- Record negative results, unresolved defects and scope boundaries.
