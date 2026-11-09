# UniCircle research and requirements baseline

Updated: 5 November 2026

## Purpose and academic position

Leeds Beckett University's description of the 40-credit Production Project asks the student to research the product requirements, synthesise information and practice into a design, implement a professional-quality product, and evaluate both the product and personal performance. This document makes the first of those stages explicit and traceable for UniCircle.

This is a living requirements baseline, not retrospective decoration. It distinguishes:

- evidence already obtained from published sources and the implemented pilot;
- assumptions that still require primary research;
- requirements accepted into the project scope;
- ideas intentionally deferred or excluded;
- the tests or evaluation evidence needed to claim success.

No interview, survey, usability score or institutional approval may be claimed unless the corresponding raw or anonymised evidence exists.

## 1. Problem definition

### 1.1 Problem statement

University students may need to move between separate channels to find campus information, connect with peers, discover events and opportunities, arrange support, and manage bookings. Fragmentation can increase the number of steps needed to complete ordinary tasks and make it harder to identify which information or contact route is trustworthy. At the same time, combining these activities creates material privacy, safeguarding, accessibility and authorisation risks.

UniCircle investigates whether a single, well-organised and responsive interface can reduce that friction for a defined set of university-life tasks while preserving consent, institutional boundaries and user control. It is a test-campus prototype, not a claim to replace a university's student-record system, virtual learning environment, emergency services or official identity provider.

### 1.2 Why the problem is worth investigating

The project specification identifies four complementary evidence areas:

1. Jisc student digital-experience research reports continued demand for usable platforms, clear communication and accessible digital support.
2. Research on engagement and belonging provides a theoretical basis for studying peer connection and participation rather than treating the product as a generic social network.
3. Information-systems success literature provides evaluation dimensions including system quality, information quality, service quality, use, satisfaction and benefits.
4. Official security, privacy and accessibility guidance establishes constraints that are as important as functional convenience.

These sources support an investigation; they do not prove that UniCircle itself is useful. That claim must come from evaluated tasks and participant feedback.

### 1.3 Research questions

**RQ1.** Can a unified interface reduce the steps and time required to complete selected university-life tasks compared with the participant's current process?

**RQ2.** Can students understand and complete the core connection, messaging, event and service-booking journeys without assistance?

**RQ3.** Do the permission, consent and expiry controls prevent one test user from accessing another user's private or location information?

**RQ4.** Does the responsive interface remain understandable and operable across desktop and mobile layouts, including keyboard-only use?

**RQ5.** Which architectural and scope decisions were effective, and which limitations prevent the pilot from being described as institutionally production-ready?

## 2. Evidence and research method

### 2.1 Secondary research already recorded

The detailed, critically appraised evidence base is maintained in `../research/literature-review.md`, with the reproducible search and appraisal record in `../research/source-evaluation-log.md`. The Component 1 project specification contains the original search-term template and bibliography. Together they cover:

- student digital experience and the intelligent campus;
- student engagement and sense of belonging;
- information-system usefulness and success;
- modular Spring application design and WebSocket security;
- web application risk and identity standards;
- WCAG 2.2 accessibility;
- consent and location-data privacy;
- System Usability Scale interpretation.

AI-assisted suggestions are recorded separately in the specification's Generative AI Log. They are treated as prompts for investigation, not academic evidence.

### 2.2 Primary research still required

The specification proposes stakeholder feedback and a small approved usability study. Until that work is completed, the following remain hypotheses:

- that the selected tasks are among users' highest-friction university tasks;
- that one interface reduces effort compared with current practice;
- that labels, navigation and privacy explanations are understood;
- that the prototype achieves at least 90% core-task completion;
- that the mean SUS score is at least 70.

A suitable, approval-dependent study should record:

| Method | Purpose | Evidence to retain |
| --- | --- | --- |
| Semi-structured stakeholder interviews or an approved survey | Validate the problem, terminology, priorities and institutional constraints | Participant information/consent, anonymised notes, question set, coded themes |
| Moderated task-based usability test | Measure whether representative users can complete core journeys | Task script, completion, time, errors, assistance and observations |
| SUS questionnaire after task testing | Provide a comparable perceived-usability measure | Anonymised responses, calculation method, mean and distribution |
| Keyboard and accessibility review | Find operability, focus, labelling, contrast and error-identification issues | Tool versions, manual checklist, findings, fixes and retest results |
| Controlled performance test | Measure response time and behaviour under a stated load | Environment, dataset, script, concurrency, percentiles, failures and date |

Ethics or supervisor approval must be obtained before recruiting participants. Participation must be voluntary; only necessary data should be collected; raw data must be stored and retained according to the approved plan.

### 2.3 Evidence quality rules

1. Prefer peer-reviewed research, standards and first-party technical guidance.
2. Record author, title, publisher, date, URL/DOI, access date and relevance.
3. Separate observed findings from interpretation and design preference.
4. Report negative and inconclusive results as well as successful results.
5. Do not generalise a small convenience sample to all university students.
6. Do not use test-campus behaviour as proof of an external university integration.

## 3. Stakeholders and needs

| Stakeholder | Primary need | Principal risk or constraint |
| --- | --- | --- |
| Student | Find relevant information, people, events, opportunities and support with fewer steps | Privacy, harassment, exclusion, confusing or misleading information |
| Staff member | Publish or manage appropriate campus activity and respond to authorised requests | Excess access, workload, unclear ownership, unofficial records |
| Administrator/moderator | Manage accounts, content, safety reports and auditable actions | Privileged-account misuse, cross-campus leakage, incomplete audit trail |
| University service owner | Integrate only reviewed and authorised services | Identity, data governance, retention, service availability and contracts |
| Accessibility user | Complete equivalent journeys with keyboard or assistive technology | Inoperable controls, poor focus, contrast, labelling or status messages |
| Project assessor | Inspect a justified engineering process and repeatable evidence | Unsupported claims, weak traceability or evaluation based only on screenshots |

The student, staff and administrator roles are implemented. Other stakeholder views remain research and governance inputs rather than fully implemented production roles.

## 4. Scope and prioritisation

### 4.1 Must have

- Responsive desktop and mobile web interface.
- Fictional student, staff and administrator identities with server-side access control.
- Personal dashboard for relevant schedule items, bookings and notifications.
- Profiles, circles, memberships and consent-based connection requests.
- Private messaging available only to authorised conversation members.
- Event/service publication, booking, cancellation and capacity protection.
- Blocking, reporting, moderation and auditable administrator actions.
- Relational persistence, input validation, privacy controls and repeatable migrations.
- Automated verification and documented evaluation evidence.

### 4.2 Should have

- Temporary opt-in location sharing with immediate revocation and automatic expiry.
- Search, filtering and useful notifications.
- Support/service discovery and private support cases.
- Calendar export and explicit integration boundaries.
- Account-data download/deletion-request workflow; this remains a documented gap where not implemented.

### 4.3 Could have

- Lost and found, opportunities, live learning, richer campus map functions and additional facility booking.
- Responsible study and campus assistant where its evidence boundaries are clear.

### 4.4 Out of scope

- Live student records, real payments or unapproved production single sign-on.
- Permanent background location tracking or location history.
- Facial recognition, emotion detection or automated high-impact decisions.
- Replacement of learning, finance, HR, safeguarding or emergency systems.
- Claims of institutional approval or production readiness without formal review.

## 5. Functional requirements and acceptance criteria

| ID | Requirement | Priority | Acceptance evidence |
| --- | --- | --- | --- |
| FR-01 | A user can authenticate in the permitted environment and sign out safely. | Must | Successful and rejected authentication tests; protected endpoints return unauthorised when signed out |
| FR-02 | A user sees only records for the correct account and university context. | Must | Cross-account and tenant-isolation integration tests |
| FR-03 | A student can view and update permitted profile fields. | Must | Validation, persistence and ownership tests |
| FR-04 | A student can request a connection; private interaction is enabled only after acceptance. | Must | Pending, accepted, declined/blocked and unauthorised-path tests |
| FR-05 | Conversation members can exchange persistent messages and receive live updates; outsiders cannot read them. | Must | Message persistence, membership denial and WebSocket tests |
| FR-06 | A user can join/leave permitted circles and view relevant circle activity. | Must | Membership and access tests |
| FR-07 | A user can discover published events/services, reserve capacity and cancel. | Must | Catalogue, booking, cancellation, duplicate and capacity tests |
| FR-08 | A user can block/report an account and the appropriate staff/admin role can review authorised cases. | Must | Safety and role-negative tests plus audit evidence |
| FR-09 | Staff/admin users can publish permitted campus content; ordinary users cannot use privileged operations. | Must | Role and campus-scoped administrator tests |
| FR-10 | A user can share an approximate location only by opting in; visibility requires accepted connection and mutual active sharing. | Should | Mutual-consent, invalid-coordinate, block and stop-sharing tests |
| FR-11 | Expired locations are hidden and physically purged; current locations are retained. | Should | Expiry API test and scheduled-purge service test |
| FR-12 | A user can discover verified opportunities and privately track an application stage. | Could | Publisher, listing and account-private tracker tests |
| FR-13 | A user can submit and search lost-and-found reports while protected data remains controlled. | Could | Validation, media and staff-review tests |
| FR-14 | A user can view targeted announcements, meetings and support records appropriate to that account. | Could | Audience, read-state, schedule and private-support tests |

## 6. Non-functional requirements

| ID | Requirement | Measure or acceptance threshold | Current evidence/status |
| --- | --- | --- | --- |
| NFR-01 Security | Deny by default; protect state changes from CSRF; validate access on the server; hash pilot passwords | Negative-access tests, security headers and no plaintext credentials | Implemented and tested; formal penetration testing outstanding |
| NFR-02 Privacy | Minimise location precision and retention; require consent; avoid real data in the pilot | Four-decimal coordinates, five-minute expiry, immediate stop and purge | Implemented; institutional DPIA/retention approval outstanding |
| NFR-03 Reliability | Core build and migration path are repeatable | `./mvnw clean verify` succeeds; all Flyway migrations validate | Implemented and repeatable |
| NFR-04 Maintainability | Modules have explicit boundaries and schema changes are versioned | Spring Modulith verification, ADRs and Flyway migrations | Implemented |
| NFR-05 Testability | Automated tests cover critical rules and regressions | Maven gate requires at least 75% line coverage | Implemented; branch coverage should continue to improve |
| NFR-06 Accessibility | Target WCAG 2.2 AA for selected journeys | Automated scan plus manual keyboard, focus, labels, contrast and status checks | Partially evidenced; formal audit outstanding |
| NFR-07 Usability | Representative users can complete agreed core tasks | Target at least 90% task completion and mean SUS at least 70 | Target defined; study not yet completed |
| NFR-08 Performance | Core requests and live messages remain responsive under a declared controlled load | Record dataset, concurrency, p50/p95/p99, error rate and environment | Not yet measured |
| NFR-09 Responsiveness | Core journeys do not overflow or lose operability on agreed phone and desktop viewports | Visual/interaction checks at named viewports and browsers | Partially evidenced by structure tests and screenshots |
| NFR-10 Deployability | Configuration is externalised and health can be checked | Container build, environment variables, PostgreSQL profile and health endpoint | Implemented for pilot rehearsal; production approval outstanding |

## 7. Traceability into design, implementation and evaluation

```text
Published evidence + approved stakeholder research
                    |
                    v
Problem statement -> research questions -> prioritised requirements
                    |                         |
                    v                         v
             architecture/ERD/UI      acceptance criteria
                    |                         |
                    +----------+--------------+
                               v
                        implementation
                               |
                               v
                 automated + participant evaluation
                               |
                               v
                 findings, limitations and iteration
```

The detailed repository evidence is indexed in `requirements-traceability.md`; design views are in `architecture-evidence.md`; decisions are in `docs/adr`; implementation increments and their acceptance notes are in `docs/sprints`; repeatable evaluation is provided by the automated test suite and `docs/qa`.

The authoritative final-scope and measurable-requirements baseline is `requirements-and-scope.md`. If wording in this older combined baseline conflicts with it, `requirements-and-scope.md` takes precedence after supervisor review.

## 8. Risks that directly affect the requirements

The specification's risk register remains authoritative. The highest-impact requirements risks are scope growth, unauthorised disclosure, unreliable real-time communication, booking races, unavailable institutional integrations, inaccessible interaction, weak research evidence and late testing. Each is linked to a mitigation: frozen Must scope, server-side authorisation, persisted messaging, database transactions, adapters/test data, iterative accessibility review, source-quality rules and continuous tests.

## 9. Definition of research completion

The Research & Requirements pillar can be presented as complete only when:

1. the official module brief and marking descriptors are attached and mapped;
2. the final report cites the original sources using the required referencing style;
3. planned primary research has approval and retained evidence, or is honestly reported as not completed;
4. findings are analysed into themes rather than presented as unsupported opinion;
5. each accepted requirement has a priority, rationale and measurable acceptance criterion;
6. changes to scope are dated and justified;
7. Design, Implementation and Evaluation sections reference the requirement IDs;
8. limitations and conflicting evidence are discussed critically.

## Research evidence and references

The critical synthesis and current Harvard-formatted bibliography are in `../research/literature-review.md`; the search and source-appraisal evidence is in `../research/source-evaluation-log.md`. The original bibliography remains in the submitted Component 1 specification under `docs/specification/`. Where a source version has changed, the literature review records the current version; in particular, ASVS 5.0.0 now provides verifiable security requirements rather than reliance on an OWASP Top 10 awareness list alone.

The Leeds Beckett Software Engineering course page and 2026/27 course handbook identify Production Project as a Level 6, 40-credit core module and describe the research-design-implementation-evaluation cycle. They should be cited as module context, not as evidence that UniCircle solves the identified problem:

- Leeds Beckett University (2026) *BSc (Hons) Software Engineering: modules*. Available at: <https://www.leedsbeckett.ac.uk/courses/software-engineering-bsc/?modules=true> (Accessed: 27 October 2026).
- Leeds Beckett University (2026) *Software Engineering BSc (Hons), 2026/27 course handbook*. Available at: <https://www.leedsbeckett.ac.uk/student-information/course-information/course-handbooks?campus=LBP&course=bsoft-c381&level=6&template=%7BAEF16A47-392E-4BBA-8B8D-2247E6A77B78%7D&year=2026> (Accessed: 27 October 2026).
