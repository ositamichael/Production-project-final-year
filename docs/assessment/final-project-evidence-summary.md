# UniCircle final project evidence summary

**Summary date:** 17 November 2026

**Author:** Michael Osita
**Working branch:** `feat/unicircle-friends-pilot`
**Project status:** Implemented academic prototype with controlled pilot boundaries and remaining external-evidence gaps

## Problem investigated

UniCircle investigates whether selected campus people, communities, messaging, events, service bookings, opportunities and support journeys can be combined in one account-controlled web prototype. The research evidence is sector and academic context; it does not establish the scale or prevalence of a problem at Leeds Beckett University.

## Research completed

The repository contains a literature review and source-evaluation log plus a requirements and scope baseline. These documents define research-derived aims, priority requirements and scope boundaries. They do not contain invented participant findings or claimed ethical approval.

## Requirements defined

The requirements baseline defines 16 functional requirements and 12 non-functional requirements. Traceability distinguishes implemented and evidenced work from weakly evidenced, partial and future work. The 75% JaCoCo line gate, security configuration, social journeys and optional features are linked to source and tests where available.

## Architecture selected

UniCircle uses Java 21, Spring Boot 4.1.1 and Spring Modulith 2.1.1 in a modular monolith. PostgreSQL and Flyway are used by the production/pilot profiles; H2 is used for default local/test profiles. Spring Security provides sessions/CSRF and a production OIDC boundary. ADRs explain the platform, modular-monolith and identity/tenancy decisions.

## Main implementation completed

Implemented areas include fictional/demo and pilot identity, profiles, university-scoped academic context, consent-based connections, private messaging, WebSocket updates, safety controls, circles, events and bookings, opportunities, lost-and-found moderation, support and live-learning features, Test Campus administration and a deterministic or optional assistant.

The implementation is not evidence of institutional approval or a live university service. Demonstration users and records are fictional or self-declared pilot records.

## Testing performed

At commit `9c6f864`, `./mvnw --batch-mode clean verify` passed 158 Maven tests with 0 failures and 0 errors. The build and JaCoCo 75% line-coverage gate passed. `node --test scripts/test-calls.mjs` passed 16 isolated call-logic tests.

The packaged JAR started successfully and `/` and `/actuator/health` returned HTTP 200 in the local smoke test. This does not constitute managed-deployment, PostgreSQL, physical-device or live-network evidence.

## Ethical, privacy and security boundaries

The repository must not request real university passwords or use live student records. Participant research must wait for the required ethical approval and approved data plan. It must not claim accessibility, security, performance or research conformance without measured evidence. The local pilot, support and assistant boundaries are explicitly fictional or non-institutional.

## Deployment status

The Docker build uses Java 21 and runs Maven verification. A Docker Compose file starts PostgreSQL for local use only. Hosted-pilot configuration is documented for a one-instance staging rehearsal. Institutional production requires approved identity/data agreements, governed operations, managed databases, backups, accessibility/security review and formal privacy and safeguarding controls.

## Known limitations

- No live university OIDC or student-record integration is approved or implemented.
- Participant recruitment and usability research remain future work.
- Independent accessibility and penetration testing remain to be completed.
- Controlled HTTP/WebSocket load testing remains outstanding.
- Physical-device and cross-network TURN testing remain outstanding.
- Managed PostgreSQL backup and restore has not been evidenced.
- Single-instance real-time state has not been validated for horizontal scaling.
- Some optional and future features require provider cost controls or institutional governance.

## Evidence locations

- **Evidence index:** [`docs/assessment/evidence-index.md`](evidence-index.md)
- **Commit-history evidence:** [`docs/assessment/commit-history-evidence.md`](commit-history-evidence.md)
- **Current verification:** [`docs/assessment/current-verification-report.md`](current-verification-report.md)
- **Requirements:** [`docs/assessment/requirements-and-scope.md`](requirements-and-scope.md) and [`docs/assessment/requirements-traceability.md`](requirements-traceability.md)
- **Architecture:** [`docs/architecture.md`](../architecture.md) and [`docs/assessment/architecture-evidence.md`](architecture-evidence.md)

## Remaining work

Supervisor review is required for the final assessed scope and post-Component-1 changes. The author or university team must arrange approved primary research, independent accessibility/security/performance assessment and institutional approvals before any live university claim or deployment.
