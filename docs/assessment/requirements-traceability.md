# UniCircle assessment requirements traceability

Updated: 27 October 2026

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

## Assessment mapping

| Assessment quality | UniCircle evidence | Status | Remaining evidence before submission |
| --- | --- | --- | --- |
| Clear real-world problem | `research-and-requirements.md`, project specification, sprint records | Evidence-based problem statement written | Complete approved primary research and analyse the results without fabricating evidence |
| Requirements analysis | `research-and-requirements.md`, sprint acceptance criteria, `docs/friends-pilot.md`, this traceability matrix | Numbered functional and non-functional baseline established | Map every official module outcome and mark descriptor when the formal assessment brief is supplied |
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

1. Obtain the official module brief and map each learning outcome and mark descriptor to this table.
2. Produce controlled load-test results for HTTP and WebSocket journeys.
3. Complete an accessibility audit and record fixes.
4. Perform a dependency and application-security review, then record findings and remediations.
5. Export final ERD, component and sequence diagrams for the dissertation.
6. Write the critical evaluation using measured outcomes rather than aspirational statements.
