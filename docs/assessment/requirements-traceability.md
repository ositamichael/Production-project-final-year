# UniCircle assessment requirements traceability

Updated: 5 November 2026

## Purpose

This document maps the qualities expected of a strong final-year production project to evidence that can be inspected in the UniCircle repository. It is an engineering traceability record, not a substitute for the official module brief or a guarantee of a particular mark. The official brief and supervisor feedback take precedence over third-party or AI-generated advice.

## Current verification baseline

Run:

```bash
./mvnw clean verify
```

Verified on 27 October 2026:

- Build result: **success**
- Automated tests: **158 passed, 0 failed, 0 errors, 0 skipped**
- Flyway migrations: **18 validated**
- JaCoCo instruction coverage: **83.3%**
- JaCoCo line coverage: **81.7%**
- JaCoCo branch coverage: **56.3%**
- JaCoCo method coverage: **84.7%**
- Enforced quality gate: **at least 75% line coverage**
- Coverage report: `target/site/jacoco/index.html`

These figures must be regenerated before submission; screenshots are supporting evidence, while the repeatable command and source-controlled tests are the primary evidence.

## Requirements baseline v1.0 reconciliation

This section reconciles the IDs in `requirements-and-scope.md` with the repository inspected on 5 November 2026. The status is based on source and existing test inspection. The full suite was **not** re-run for this documentation update, so the 27 October verification figures above remain historical rather than a claim about the current working tree.

Status meanings:

- **Implemented and evidenced** - relevant implementation and repeatable automated tests were identified.
- **Implemented but weakly evidenced** - implementation exists, but a required negative, race, audit or end-to-end case is missing or unclear.
- **Partially implemented** - only part of the measurable requirement is evidenced.
- **Not yet evidenced** - no valid completion evidence exists; a plan or target is not a result.

### Functional requirements

| ID | Status | Principal repository evidence | Gap/action |
| --- | --- | --- | --- |
| FR-01 | Implemented and evidenced | `DemoSignInJourneyTests`; `PilotFriendsJourneyTests`; protected controller tests | Re-run the complete suite and retain final authentication results. |
| FR-02 | Implemented and evidenced | `CampusContextControllerTests`; `AdminControllerTests`; `SocialControllerTests`; `LostFoundStaffReviewControllerTests` | Add one consolidated cross-role/cross-university security evidence table for the report. |
| FR-03 | Implemented and evidenced | `ProfileControllerTests`; `LocalProfilePhotoStorageTests` | Confirm production media-storage and retention boundaries remain explicit. |
| FR-04 | Implemented and evidenced | `SocialControllerTests`; `PilotFriendsJourneyTests` | Retain state-transition evidence in the final appendix. |
| FR-05 | Implemented and evidenced | `SocialControllerTests`; authenticated real-time gateway; call/signalling tests provide additional permission evidence | Add measured concurrent WebSocket behaviour only if claimed as a performance result. |
| FR-06 | Implemented and evidenced | `CommunityControllerTests`; community module and migrations | Add an explicit cross-university negative test if not already covered indirectly by fixtures. |
| FR-07 | Implemented and evidenced | `AdminControllerTests`; `InformationControllerTests`; engagement publication paths | Clarify which publisher role owns each retained final feature. |
| FR-08 | Implemented but weakly evidenced | `EngagementControllerTests` covers registration, booking, duplicates, availability, cancellation and schedule | Add a genuine simultaneous final-place booking race test; sequential availability is not sufficient concurrency evidence. |
| FR-09 | Implemented and evidenced | `EngagementControllerTests`; `AdminControllerTests`; account-specific information tests | Record one complete schedule-composition demonstration. |
| FR-10 | Implemented but weakly evidenced | `SocialControllerTests` covers blocking and report creation; staff review/audit patterns exist in lost-and-found | Add or identify the exact authorised safety-report review tests and audit transition. |
| FR-11 | Implemented but weakly evidenced | `AdminControllerTests` demonstrates role/campus scope; audited moderation exists in `LostFoundStaffReviewControllerTests` | Demonstrate that every retained privileged mutation, not only selected moderation actions, has the required audit record. |
| FR-12 | Implemented and evidenced | `SearchControllerTests`; feature catalogue visibility tests | Add explicit moderation-pending/private-record search denial where not already covered. |
| FR-13 | Implemented and evidenced | `PilotFriendsJourneyTests`; `LocationSharingServiceTests` | Add retained proof of configured expiry/precision and document the retention rationale. |
| FR-14 | Implemented and evidenced | `EngagementControllerTests.personalScheduleCanBeDownloadedAsAStandardsBasedCalendar` | Validate the exported file manually in at least one named calendar client if interoperability is claimed. |
| FR-15 | Implemented and evidenced | `InformationControllerTests.supportRequestsAreTrackedPrivatelyPerAccount`; public safety/boundary content | Participant understanding of boundaries remains ethics dependent. |
| FR-16 | Conditional | Extensive optional-feature controller, structure and integration tests | Select the final extensions first; exclude unstable or irrelevant extensions from the assessed release rather than claiming all of them. |

### Non-functional requirements

| ID | Status | Principal repository evidence | Gap/action |
| --- | --- | --- | --- |
| NFR-01 | Partially implemented | Spring Security configuration, CSRF tests, negative access tests, security headers and validation tests | Select/map ASVS 5.0.0 controls; run dependency and application-security review; triage findings. |
| NFR-02 | Partially implemented | Fictional-data labels, privacy page, ownership tests, short-lived location design | Produce a data inventory covering purpose, fields, access, retention and deletion; decide the account data-export/deletion gap. |
| NFR-03 | Partially implemented | Semantic/structure checks, responsive styles, accessibility information | Run named automated scans and the complete manual WCAG 2.2 AA core-journey checklist, fix defects and retest. |
| NFR-04 | Not yet evidenced | Target and proposed method exist only | Wait for Favourable Opinion; then retain approved task data, SUS calculations and limitations. |
| NFR-05 | Not yet evidenced | No controlled current load report identified | Create repeatable HTTP/WebSocket load scripts and record environment, dataset, percentiles and failures. |
| NFR-06 | Implemented; current run pending | Maven/Flyway build and historical 27 October result; booking transaction tests | Re-run from a clean state and add the simultaneous booking race test. |
| NFR-07 | Implemented and evidenced | `ArchitectureTests.modularBoundariesAreValid`; ADRs; Flyway migrations | Generate final diagrams from the implemented structure and evaluate coupling/trade-offs. |
| NFR-08 | Implemented but reconciliation incomplete | JUnit/MockMvc tests and JaCoCo 75% line gate | Link every Must acceptance criterion to a named test and add missing negative/race cases. |
| NFR-09 | Implemented but weakly evidenced | `FrontendStructureTests`, responsive CSS and retained screenshots | Perform named browser/viewport manual checks; structure assertions do not prove visual operability. |
| NFR-10 | Partially implemented | Audited moderation/admin records and safe exception tests | Define the final privileged-event audit set and verify secrets/sensitive payloads are not logged. |
| NFR-11 | Implemented and evidenced for rehearsal | Dockerfile, external configuration, production profile and health endpoint | Rehearsal evidence is not institutionally approved production evidence; record the final environment. |
| NFR-12 | Partially implemented | Dated QA documents, sprints, build command and traceability records | Replace stale metrics with final dated outputs and archive tool versions/environment details. |

## Assessment mapping

| Assessment quality | UniCircle evidence | Status | Remaining evidence before submission |
| --- | --- | --- | --- |
| Clear real-world problem | `research-and-requirements.md`, project specification, sprint records | Evidence-based problem statement written | Complete approved primary research and analyse the results without fabricating evidence |
| Requirements analysis | `requirements-and-scope.md`, `../research/literature-review.md`, `../research/source-evaluation-log.md`, sprint acceptance criteria, `docs/friends-pilot.md`, this traceability matrix | Research-derived functional and non-functional baseline established | Obtain supervisor confirmation of the final core scope and material post-Component-1 changes |
| Functional requirements | Identity, profiles, connections, messages, calls, circles, events, bookings, opportunities, updates, support, lost and found, assistant and administration | Implemented and tested | Keep the submission scope explicit; do not claim unavailable institutional integrations |
| Non-functional requirements | Security headers, CSRF, access control, validation, rate limits, short-lived location data, responsive frontend, health endpoint | Implemented | Add measured load-test results and formal accessibility audit evidence |
| Architecture and justification | `docs/architecture.md`, ADRs, modularity test, architecture diagrams | Implemented | Include selected diagrams and trade-off discussion in the dissertation |
| Normalised data design | 18 Flyway migrations with keys, constraints, tenancy and join tables | Implemented | Export a final ERD image from the migration-backed schema |
| Secure authentication | Development demo identities, invitation-only pilot accounts, production OIDC | Implemented by environment | Complete university identity-provider approval before any institutional deployment |
| Authorisation | Student, staff and administrator roles; university scoping; server-side ownership checks | Implemented and tested | Include negative-access test evidence in the appendix |
| Password protection | Delegating password encoder and stored password hashes in the pilot | Implemented | State the selected encoder and upgrade path in the report |
| Real-time communication | Authenticated WebSocket event gateway plus persistent database messages | Implemented and tested | Add a measured concurrent-connection test if required by the official brief |
| Privacy-preserving location | Opt-in, accepted connections only, mutual sharing, rounded coordinates, five-minute expiry, immediate deletion and scheduled purge | Implemented and tested | Document privacy threat analysis and participant consent |
| Data persistence | PostgreSQL production profile, Flyway schema, JPA/JDBC services, H2 test/local profiles | Implemented | Demonstrate backup and restore for a production-readiness claim |
| Input and media validation | Jakarta validation, server-side ownership rules, type/size checks for uploaded media | Implemented and tested | Add malware-scanning integration only if real public uploads enter scope |
| Moderation and safeguarding | Blocking, reporting, staff lost-and-found review, verified publishers and audit records | Implemented and tested | Agree operational moderation and safeguarding ownership with the institution |
| Automated testing | JUnit 5, MockMvc, architecture, security, validation, persistence and signalling tests | Implemented: 158 passing | Maintain the 75% line-coverage gate and add tests with every defect fix |
| Coverage measurement | JaCoCo report and Maven verification gate | Implemented | Archive the final HTML report and console result in submission evidence |
| CI/CD | GitHub Actions runs `clean verify`; Dockerfile and Render deployment configuration are present | Implemented | Add deployment approval gates if the pilot becomes a production service |
| Critical evaluation | Honest boundaries in README, architecture and pilot documentation | In progress | Write measured limitations, failed approaches, trade-offs and future work in the final report |
| Accessibility | Semantic labels, accessibility page, keyboard/mobile structure tests | Partially evidenced | Run WCAG audit and moderated testing with representative users |
| Performance | Efficient indexed schema and bounded queries are present | Partially evidenced | Record repeatable response-time and concurrency measurements against a controlled environment |
| Institutional production readiness | OIDC direction, data boundaries and governance requirements are documented | Not yet approved | Security review, DPIA/privacy review, penetration test, backups, retention, safeguarding and data agreements |

## Deliberate architecture decisions

### Session security instead of a browser-managed JWT

The generic checklist suggests JWT for every deployment. UniCircle deliberately uses Spring Security sessions for the invitation-only pilot and OIDC login for the production profile. This avoids storing a bearer token in browser JavaScript and lets Spring Security apply CSRF protection to state-changing requests. The final report should compare this choice with stateless JWT and explain why the selected design fits a server-rendered modular monolith.

### Modular monolith instead of microservices

The project uses a Spring Modulith modular monolith. This is proportionate for one student and one deployment while still enforcing module boundaries. It reduces distributed-system and operational risk without preventing later extraction of high-load modules.

### PostgreSQL as the authoritative store

Core identity, messaging, booking, moderation and campus records share transactional relationships. PostgreSQL and Flyway provide referential integrity and reproducible schema evolution. A second NoSQL database is not added merely to appear complex; that would increase operational risk without a demonstrated requirement.

### Honest pilot boundaries

The pilot must continue to state that its campus data is fictional and that it is not an official university service. Removing those disclosures or claiming a live university connection would weaken the ethical and academic quality of the submission.

## Evidence rules

1. Claim only behaviour that is implemented and repeatably tested.
2. Prefer source code, automated tests, migrations, logs and measured results over screenshots alone.
3. Keep secrets, passwords and invitation codes outside Git.
4. Do not use real student data without approval, consent and a defined retention process.
5. Record limitations and unsuccessful decisions; critical evaluation earns more credibility than inflated claims.
6. Re-run `./mvnw clean verify` immediately before every release and before collecting final submission evidence.

## Highest-priority remaining work

1. Review the final Must/Should/Could boundaries and post-Component-1 scope changes with the supervisor, then record the decision.
2. Produce controlled load-test results for HTTP and WebSocket journeys.
3. Complete an accessibility audit and record fixes.
4. Perform a dependency and application-security review, then record findings and remediations.
5. Export final ERD, component and sequence diagrams for the dissertation.
6. Write the critical evaluation using measured outcomes rather than aspirational statements.
