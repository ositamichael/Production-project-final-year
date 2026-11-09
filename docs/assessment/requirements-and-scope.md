# UniCircle requirements and scope baseline

**Baseline:** v1.0

**Date:** 5 November 2026

**Status:** Engineering baseline for supervisor review and final-scope control

**Project:** UniCircle - a secure digital campus platform for university life

## 1. Purpose and Leeds Beckett alignment

This document converts the verified research in `../research/literature-review.md` and `../research/source-evaluation-log.md` into precise, prioritised and testable engineering requirements. It supports the Production Project requirements to specify, justify and plan a substantial product, research its requirements, synthesise evidence into a design, and later test and evaluate the result.

The 2026/27 assessment brief required Component 1 to contain a project specification, initial literature search, MoSCoW requirements and risk register. It also states that significant changes after Component 1 should be agreed with the supervisor and may require revised aims, objectives, product description or risk material. This baseline therefore does not silently rewrite the submitted Component 1 scope. It records changes transparently and identifies decisions that need supervisor confirmation.

This is an engineering specification, not proof that every requirement has been met. Implementation and test evidence are mapped separately in `requirements-traceability.md`.

## 2. Evidence keys

| Key | Evidence source | What it justifies |
| --- | --- | --- |
| LBU-PP | Production Project module handbook, assessment brief and official Software Engineering course description | Level 6 research, specification, planning, product, evaluation and critical-reflection expectations |
| R-JISC | Jisc 2024/25 student digital-experience findings and 2023 intelligent-campus guide | Investigating clear access to selected campus information, connection, activities and services while retaining user benefit and ethical limits |
| R-ENG | Kahu and Nelson (2018); Thomas (2012); Thomas et al. (2017) | Relevance of connection, interaction, engagement and belonging; not proof that UniCircle improves retention |
| R-EVAL | Davis (1989); DeLone and McLean (2003); Brooke (1996); Bangor, Kortum and Miller (2008) | Mixed evaluation of usefulness, ease, quality, task performance and perceived usability |
| S-WCAG | W3C WCAG 2.2 | Accessibility acceptance criteria |
| S-PRIV | ICO data protection by design/default and data-minimisation guidance | Purpose limitation, minimisation, retention, access control and user choice |
| S-SEC | OWASP Top 10:2025 and ASVS 5.0.0 | Threat-led, verifiable application-security requirements |
| S-ID | OpenID Connect Core 1.0 errata set 2 | Future institutional identity boundary; not approval for a live university connection |
| S-INT | 1EdTech Edu-API candidate material | Adapter-based integration direction; not a claim of certification or universal adoption |
| T-SPRING | Spring Modulith and Spring Security WebSocket documentation | Proportionate modular-monolith structure and secure real-time design mechanisms |

## 3. Problem, aim and success position

### 3.1 Qualified problem statement

Students may use multiple channels to discover university information, communicate, participate in activities and arrange selected services. Published sector evidence makes this a reasonable area to investigate, but does not establish the scale of the problem at Leeds Beckett. UniCircle will therefore test selected journeys rather than claiming that all university systems are fragmented or deficient.

### 3.2 Aim

To design, implement and critically evaluate a secure, accessible and modular web prototype that brings a controlled set of university-life journeys into one interface using fictional or specifically approved data.

### 3.3 Success position

The project succeeds academically by producing a justified and traceable engineering process and honestly evaluated product. Success does not require proving that UniCircle should replace institutional systems. Negative findings, unmet targets and rejected design decisions remain valid evidence when analysed critically.

## 4. Stakeholders and authorised roles

| Stakeholder/role | Required capability | Boundary |
| --- | --- | --- |
| Student | Manage permitted profile data; discover people, circles, events and services; form consensual connections; communicate privately; make and cancel bookings; use safety controls | Cannot use staff/admin actions or access another account's private records |
| Staff | Publish or manage authorised campus content and respond to appropriate service activity | Access is limited by role, university context and assigned responsibility |
| Service provider | Manage only assigned service-related activity where this role is enabled | Not a general administrator |
| Administrator/moderator | Manage authorised users/content, review defined reports and inspect audit evidence | Privileged operations must be university scoped and auditable |
| University service owner | Review any future identity, timetable, booking, support or governance integration | No live connection exists without approval and contracts |
| Research participant | Complete only the activities and data collection approved in Worktribe | No recruitment or collection before Favourable Opinion |
| Assessor/supervisor | Inspect decisions, source evidence, code, tests, limitations and reflection | Repository claims must be repeatable and not depend on screenshots alone |

The implemented role enumeration currently includes `STUDENT`, `STAFF`, `SERVICE_PROVIDER` and `ADMIN`. Product documentation and tests must use the same names or explicitly map user-facing labels to them.

## 5. Scope control

### 5.1 Core assessed release - Must

The final assessed core is deliberately smaller than every feature currently present in the repository.

1. Fictional/test identity, secure sign-in/out and server-enforced role separation.
2. Account-owned profile and privacy-controlled campus context.
3. Same-university discovery, connection request/decision, block and report controls.
4. Persistent private messaging restricted to authorised conversation members.
5. Circles with university isolation and explicit join/leave membership.
6. Events and service/appointment discovery, capacity-aware booking and cancellation.
7. Personal dashboard/schedule/notifications built from authorised records.
8. Staff/admin publishing or moderation operations with audit evidence and negative-access tests.
9. Relational persistence, versioned migrations, validation and repeatable automated verification.
10. A responsive interface for agreed desktop and mobile viewports.
11. Evidence for security, privacy, accessibility, performance and evaluation requirements.

### 5.2 Planned supporting capability - Should

1. Temporary opt-in approximate location sharing with mutual permission, immediate revocation and automatic expiry.
2. Search and filtering across the agreed core catalogue.
3. Support/service discovery and private support cases with explicit non-emergency boundaries.
4. Standards-based calendar export.
5. Account-data export/deletion-request workflow, or a clearly documented implementation gap if not completed.

### 5.3 Optional extensions - Could

The repository already contains several of these. They may be demonstrated only when stable, tested and clearly separated from the core success claim:

- opportunities and private application tracking;
- moderated lost-and-found reporting;
- announcements and live-learning participation;
- WebRTC voice/video calling;
- campus assistant and sourced public-knowledge features;
- private illustration generation;
- richer test-campus administration; and
- installable/offline progressive-web-app behaviour.

Optional extensions must not consume the time reserved for accessibility, security, performance, evaluation, report writing and critical reflection. An implemented optional feature may be removed from the final demonstration if it introduces unresolved risk or distracts from the research aim.

### 5.4 Excluded from this project - Will not

- Live student records, marks, finance, HR or safeguarding case systems.
- A claim of production Leeds Beckett single sign-on without identity-provider approval.
- Real payments or financial transactions.
- Permanent/background movement tracking or a location history.
- Facial recognition, emotion detection or automated medical, disciplinary, admissions or grading decisions.
- Replacement of the VLE, emergency services, official support or institutional systems of record.
- Institution-wide deployment, legal compliance certification or production-readiness claims without formal governance and security review.
- Unapproved collection of participant or real student data.

## 6. Functional requirements

All Must requirements require automated acceptance evidence unless the verification method is explicitly manual. “User” means an authenticated account in the permitted environment.

| ID | Requirement and acceptance criterion | Priority | Rationale/source | Verification |
| --- | --- | --- | --- | --- |
| FR-01 | Permitted accounts can authenticate and sign out; unauthenticated requests to protected endpoints are denied without returning private content. | Must | S-SEC, S-ID | Authentication and protected-endpoint integration tests |
| FR-02 | Each protected record is restricted by account ownership, conversation/membership permission, role and university context as applicable. Cross-account, cross-role and cross-university attempts are denied. | Must | S-PRIV, S-SEC | Negative authorisation and tenant-isolation tests |
| FR-03 | A student can view and update only the permitted profile fields; invalid values and unsupported media are rejected without partial unsafe persistence. | Must | S-PRIV, S-WCAG | Validation, ownership, persistence and media tests |
| FR-04 | A student can find eligible same-university accounts and send one valid connection request. Only the recipient can accept or decline it, and blocking prevents private interaction. | Must | R-ENG, S-PRIV, S-SEC | State-transition, duplicate, block and negative-access tests |
| FR-05 | Accepted participants can exchange persistent messages and receive live updates. A non-member cannot list, read, send to or subscribe to that conversation. | Must | R-JISC, S-SEC, T-SPRING | Persistence, membership, WebSocket and reconnect tests |
| FR-06 | A user can discover permitted circles and join/leave them. Circle data and membership are isolated by university. | Must | R-ENG, S-PRIV | Membership and university-isolation tests |
| FR-07 | Authorised publishers can create/publish agreed events or service slots; ordinary students cannot call privileged publishing operations. | Must | R-JISC, S-SEC | Role-positive and role-negative integration tests |
| FR-08 | A student can discover published events/services, make one valid reservation and cancel it. Duplicate, overlapping, closed and full-capacity reservations are rejected consistently. | Must | R-JISC, S-SEC | Transaction, concurrency, duplicate, overlap and cancellation tests |
| FR-09 | The personal dashboard/schedule shows only authorised timetable fixtures, registrations, confirmed bookings and notifications for the signed-in account. | Must | R-JISC, S-PRIV | Account-isolation and composition tests |
| FR-10 | A user can block and report an account. Only an authorised staff/admin workflow can inspect or update the appropriate report, and the action is auditable. | Must | R-ENG, S-PRIV, S-SEC | Safety, role-negative and audit tests |
| FR-11 | Approved admin operations are scoped to the administrator's university and create an audit record containing actor, action, target and time without exposing unnecessary sensitive content. | Must | S-PRIV, S-SEC | Admin isolation and audit-persistence tests |
| FR-12 | Search returns only permitted, published and university-relevant records and does not reveal private or moderation-pending data. | Should | R-JISC, S-PRIV | Search visibility and isolation tests |
| FR-13 | Approximate location sharing is off by default, requires an accepted relationship and mutual active sharing, can be stopped immediately, and is hidden/purged after expiry. | Should | S-PRIV, R-JISC | Consent, coordinate, block, expiry and purge tests |
| FR-14 | A user can export agreed registered events and confirmed bookings using a standards-based calendar representation without exporting another user's data. | Should | R-JISC, S-PRIV | Format and account-isolation tests |
| FR-15 | A user can open and track a private support request routed to a fictional/approved service while emergency and official-service boundaries remain visible. | Should | R-JISC, S-PRIV | Ownership, routing, status and boundary-content tests |
| FR-16 | If retained in the final release, each optional extension must have a named owner/permission model, validation, negative-access tests and a visible fictional/non-institutional boundary. | Could | LBU-PP, S-PRIV, S-SEC | Feature-specific acceptance evidence |

## 7. Non-functional requirements

Thresholds marked **project target** are engineering decisions, not Leeds Beckett rules or externally certified limits. They must be reported with the environment, method and limitations.

| ID | Requirement | Measurable acceptance criterion | Source/status |
| --- | --- | --- | --- |
| NFR-01 Security | Critical application paths must be deny-by-default and tested against a recorded subset of OWASP ASVS 5.0.0 Level 1 controls. | All selected ASVS controls mapped; all automated negative-access tests pass; no unresolved Critical/High dependency finding accepted without documented risk treatment. | S-SEC; control selection still required |
| NFR-02 Privacy | Development and demonstration use fictional or specifically approved data; personal data is purpose limited, minimised and access controlled. | Data inventory records purpose, fields, access, retention and deletion route; no real credentials committed; location meets FR-13. | S-PRIV |
| NFR-03 Accessibility | Agreed core journeys target WCAG 2.2 AA. | Automated scan records zero unresolved critical/serious findings on named pages; manual keyboard, focus, labels, status, error, zoom/reflow and contrast checks are completed with defects and retests recorded. No whole-product conformance claim without complete evidence. | S-WCAG |
| NFR-04 Usability | Representative approved users should understand and complete the agreed core tasks. | **Project target:** at least 90% core-task completion and mean SUS at least 70, reported with participant count, distribution, errors, assistance and sampling limitations. No data before Favourable Opinion. | R-EVAL; approval dependent |
| NFR-05 Performance | Core HTTP and message journeys remain responsive under a declared controlled workload. | After baseline measurement, record environment, dataset, concurrency, warm-up, repetitions, p50/p95/p99 and failure rate. Provisional **project targets:** HTTP p95 <= 1,000 ms and message update p95 <= 2,000 ms with <1% request failures under the stated pilot load. | Engineering target; performance run outstanding |
| NFR-06 Reliability | The product and schema can be rebuilt repeatably from source. | `./mvnw clean verify` passes; all Flyway migrations validate from an empty database; failed transactions do not leave duplicate/partial booking state. | LBU-PP, T-SPRING |
| NFR-07 Maintainability | Functional modules and database changes are explicit and verifiable. | Spring Modulith verification passes; migrations are versioned; architecture decisions and public module boundaries are documented; no circular module dependency is accepted without rationale. | T-SPRING |
| NFR-08 Testability | Critical business and security rules have repeatable automated tests. | Line-coverage gate remains at least 75%; coverage is not used alone; every Must rule has positive and negative evidence or a recorded justified gap. | LBU-PP, S-SEC |
| NFR-09 Responsiveness | Core tasks remain operable on the agreed phone and desktop layouts. | Named checks at 390x844 and 1440x900 show no inaccessible control, unintended horizontal page scroll or content-hidden-by-navigation defect in core journeys. | S-WCAG; project target |
| NFR-10 Auditability | Privileged and moderation decisions can be attributed and reviewed without logging secrets or unnecessary message content. | Actor, action, target, result and timestamp retained for selected admin/moderation events; password, token and sensitive payload logging tests/checks pass. | S-PRIV, S-SEC |
| NFR-11 Deployability | Environment-specific values and secrets are externalised and the supervised pilot can expose a health signal. | No production secret in Git; documented environment variables; container/application starts in the declared rehearsal environment; health check reports expected state. | LBU-PP |
| NFR-12 Evidence integrity | Claims must be reproducible and dated. | Each final metric records command/tool version, environment, date and retained output; outdated results are not reported as current. | LBU-PP |

## 8. Core acceptance journeys

| Journey | Start state | Successful outcome | Principal requirements |
| --- | --- | --- | --- |
| J-01 Secure access | Signed out | User signs in with a fictional/approved account, reaches only authorised data, then signs out | FR-01, FR-02, NFR-01 |
| J-02 Connection and safety | Two separate same-university accounts | Request, recipient decision, permitted interaction, block/report and resulting denial behave correctly | FR-04, FR-10 |
| J-03 Private messaging | Accepted connection | Messages persist and update live; a third account is denied | FR-05, NFR-01 |
| J-04 Circle participation | Signed-in student | User discovers, joins and leaves a university-scoped circle | FR-06, FR-12 |
| J-05 Event registration | Published event with capacity | Registration appears in schedule; duplicate/full/cancel cases are correct | FR-07, FR-08, FR-09 |
| J-06 Service booking | Available and conflicting slots exist | Valid booking succeeds; overlap/duplicate/capacity conflicts fail atomically; cancellation updates schedule | FR-08, FR-09, NFR-06 |
| J-07 Privileged administration | Student and admin accounts | Student is denied; scoped admin action succeeds and is audited | FR-02, FR-11, NFR-10 |
| J-08 Accessible responsive use | Named phone/desktop browsers | Core journeys remain keyboard operable, visible, understandable and responsive | NFR-03, NFR-09 |

## 9. Assumptions and constraints

- The assessed prototype uses fictional Test Campus data unless Worktribe and institutional governance explicitly authorise otherwise.
- Favourable Opinion permits only the approved participant method and data plan; it does not approve production deployment.
- Live institutional APIs, identity-provider configuration, email, safeguarding operations and service ownership are unavailable unless separately confirmed.
- The product is built by one Level 6 student within the module schedule, so a modular monolith is preferred over distributed microservices.
- Java 21, Spring Boot 4.1.1, Spring Modulith 2.1.1, PostgreSQL/Flyway and a responsive web client are the current implementation baseline; version changes require compatibility and risk review.
- The final submission must preserve the required source and non-original-code acknowledgements.

## 10. Scope-change record requiring supervisor visibility

| Change after Component 1 | Current repository position | Scope decision | Required action |
| --- | --- | --- | --- |
| Service-provider role made explicit | Implemented in `UserRole` | Retain only for bounded service operations | Confirm role wording with supervisor and update diagrams/tests |
| Opportunities and lost-and-found expanded | Implemented | Classify as Could extensions | Do not make core success dependent on them |
| Voice/video calling added | Implemented | Could extension | Demonstrate only if permissions, network limitations and tests are documented |
| Announcements, support and live learning expanded | Implemented | Support is Should; announcements/live learning are Could | Freeze the exact final demonstration subset |
| Campus assistant and optional external-provider modes added | Implemented | Could extension and separate study area | Supervisor should confirm relevance to the approved aim; record provider, cost, privacy, source and fallback boundaries |
| Test-campus administration expanded | Implemented | Retain only the controls required for testing, moderation and evidence | Avoid presenting fictional operator controls as institutionally production ready |

The project specification already anticipated several of these as future or lower-priority ideas, but their implemented breadth is a material scope change. This register must be discussed with the supervisor rather than retrospectively presenting every feature as part of the original Must scope.

## 11. Definition of Stage 2 completion

Requirements and Scope is complete enough to move into final architecture/implementation reconciliation when:

1. the supervisor has seen the material scope-change record;
2. the final Must/Should/Could/Will-not boundaries are frozen and dated;
3. every Must requirement has a source/rationale and measurable acceptance method;
4. each Must requirement is mapped to design, code and current test evidence;
5. unresolved gaps are recorded rather than relabelled as complete;
6. Worktribe status and participant restrictions remain accurate;
7. optional features cannot prevent completion of testing, evaluation, report or reflection; and
8. any later change is entered in this document with date, reason, impact and approval/decision.

## 12. Immediate next engineering action

Reconcile FR-01 to FR-16 and NFR-01 to NFR-12 against the current source code and automated tests. The output must identify **implemented and evidenced**, **implemented but weakly evidenced**, **partially implemented**, and **not implemented** requirements. No requirement will be marked complete merely because a screen or README statement exists.
