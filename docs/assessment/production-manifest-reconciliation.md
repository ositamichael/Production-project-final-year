# Production manifest reconciliation

Updated: 27 October 2026

## Purpose

This document reconciles the supplied **Production-Grade Full-Stack Platform Specification** and subsequent **Production Workspace Manifest & Technical Specification** with the actual UniCircle repository. It prevents aspirational or generated sample code from being presented as implemented evidence.

The supplied manifest is a useful review checklist, but it is not the official Leeds Beckett marking rubric and its example source is not compatible with this codebase. The examples use different package names, entities, endpoint paths and frontend files; place multiple public Java types in one file; contain malformed JSON test strings; disable CSRF; and expect an unauthenticated API request to return `403` when UniCircle deliberately returns `401`. Those examples are therefore assessed requirement-by-requirement rather than copied.

Status meanings:

- **Met** — implemented and backed by inspectable evidence.
- **Partially met** — useful behaviour exists, but the supplied wording overstates or differs from the implemented guarantee.
- **Rejected by design** — a safer or more appropriate architecture is already documented.
- **Not yet evidenced** — the claim needs measured or external evidence before it can be made.

## Latest workspace manifest reconciliation

The later workspace manifest renumbers and revises the requirements. This table maps that exact version to real evidence.

| Workspace ID | Status | Real evidence or limitation |
| --- | --- | --- |
| FR-01 identity boundary | Met | Spring Security returns `401` for unauthenticated API requests; pilot sessions and production OIDC are separated by profile. |
| FR-02 multi-role routing | Met without `@PreAuthorize` | Navigation is role-aware, while service-layer database checks enforce current role, active status, ownership and university scope. The implementation is tested and does not depend on client claims. |
| FR-03 real-time events | Met with correction | Authenticated native WebSocket handshakes and persistent messaging are implemented. The workspace phrase “JWT signature intercepts” is inaccurate because UniCircle deliberately uses the authenticated Spring session. |
| FR-04 location eviction | Met | Five-minute expiry, API invisibility, immediate stop and scheduled physical deletion are implemented and tested. |
| FR-05 Today aggregation | Met | The frontend asynchronously composes assigned meetings, registered events and confirmed bookings into personal Today/schedule views. There is no invented `DashboardSummaryController`. |
| FR-06 bulletin hierarchy | Partially met | Administrators publish pinned, urgent, categorised and targeted bulletins. Granting the same write permission to all staff requires a defined institutional role policy and is not silently assumed. |
| FR-07 classroom coordination | Met | Meeting time ordering, module schedule overlap, audience assignment, attendance, hand raising and participant chat/questions are implemented and tested. |
| FR-08 circle clustering | Met | User-created categorised circles have membership/ownership rules and a per-university unique name constraint. |
| FR-09 resource scheduling | Met | Publication-time ranges reject overlaps; booking-time capacity uses `FOR UPDATE`, transactions, duplicate protection and personal-overlap checks. |
| FR-10 career pipeline | Met | Private opportunity tracking validates Planning, Applied, Interview, Offer and Closed states. |
| FR-11 welfare intake | Met for the pilot | Support cases are private to their reporter, routed by category and available to authorised administration. The feature is not represented as a medical service. |
| FR-12 global fault interception | Met | `ApiExceptionHandler` produces stable validation/rejection responses without trace or exception fields; Boot is also configured never to include messages or stack traces in generic error output. |
| FR-13 audit log | Partially met | Administrator mutations append actor, resource, summary and timestamp records to the database. A separately configured write-once SLF4J audit destination is not implemented and must not be claimed. |
| FR-14 schema migrations | Met | All 18 versioned Flyway migrations are validated and executed by every full integration build. |
| NFR-01 BCrypt 12 | Met | Explicit delegating BCrypt cost 12 is implemented and asserted after pilot registration. |
| NFR-02 75% coverage | Met | The Maven JaCoCo gate fails builds below 75% line coverage. |
| NFR-03 CSRF | Met | CSRF remains enabled and state-changing journeys include negative missing-token tests. |
| NFR-04 injection mitigation | Partially evidenced | Jakarta Validation, prepared JDBC bindings, database constraints, CSP and safe output handling provide layers. “Malicious characters” cannot be universally filtered without context; dynamic security testing remains appropriate. |
| NFR-05 background isolation | Met | Spring's scheduler invokes transactional expiry deletion outside request handling; the endpoint does not wait for the periodic sweep. |
| NFR-06 3NF | Substantially met | Migration-backed relations use explicit keys, junction tables and constraints; the final report still needs a dependency-based normalisation explanation. |
| NFR-07 container runtime | Met for reproducible verification | Multi-stage Java 21 Docker packaging runs the full Maven verification gate and executes as a non-root user. Exact digest pinning remains a production hardening step. |
| NFR-08 location precision | Met with corrected wording | Four-decimal rounding is implemented. It reduces precision but is not a universal 11-metre anonymity guarantee. |
| NFR-09 graceful errors | Met | Structured validation and rejection JSON is tested; internal traces/types are absent. |
| NFR-10 transparent gaps | Met | Performance, accessibility, socket load, backup/restore and institutional approvals remain explicitly classified as unmeasured or external evidence. |

## Original manifest functional-requirements reconciliation

| Manifest ID | Status | UniCircle evidence and decision |
| --- | --- | --- |
| FR-1 Institutional domain validation | Partially met | Production authentication is designed around an approved university OIDC issuer, which is stronger than trusting an email suffix typed by a user. The staging pilot intentionally accepts fictional addresses and must not solicit real university credentials. A production issuer/tenant allow-list remains an institutional configuration task. |
| FR-2 Role-based authorisation | Met | Student, staff, service-provider and administrator roles exist. Navigation is permission-aware, while controllers/services independently re-check identity, role, account status, ownership and university scope. Negative tests cover ordinary and unauthenticated access. |
| FR-3 JWT in HttpOnly cookies | Rejected by design | The pilot uses server-side Spring Security sessions with CSRF protection; production uses OIDC login. Introducing a custom JWT cookie would duplicate identity state and add token rotation/revocation complexity without a requirement for stateless APIs. See the architecture decision in `requirements-traceability.md`. |
| FR-4 bidirectional asynchronous messaging | Met with a different protocol | UniCircle uses authenticated native WebSockets backed by persistent REST/database messages and live event delivery. STOMP is an implementation option, not a user requirement; adding it would not itself improve the current acceptance criteria. Call signalling has rate, membership and payload controls. |
| FR-5 unified schedule | Met | The personal schedule combines registered events, confirmed service bookings and assigned learning meetings. Tests verify propagation and audience isolation. Live institutional timetable/room feeds remain explicitly out of scope without authorised integrations. |
| FR-6 five-minute location deletion | Met | Location shares receive an explicit five-minute `expires_at`, are hidden after expiry and are removed by a scheduled one-minute purge. A dedicated automated test now proves expired rows are deleted while current rows remain. PostgreSQL does not provide a native table-row TTL, so the transactional scheduled purge is the portable design. |
| FR-7 four-decimal location masking | Met with corrected claim | Coordinates are rounded to four decimal places before persistence. The manifest's phrase “maximum accuracy of 11 metres” is not a universal guarantee because longitude distance varies by latitude and device-reported accuracy is separate. The report must describe this as precision reduction, not guaranteed anonymity. |
| FR-8 trusted bulletins | Met | Administrators can publish targeted, pinned, urgent and categorised announcements. Audience rules, notifications, persistence and empty-audience rejection are tested. “Trusted” refers to controlled publisher roles in the test campus, not external institutional endorsement. |
| FR-9 moderation queue | Met | New lost-and-found reports remain hidden until staff review; the review lifecycle and rejected paths are tested. Opportunities also use verified publishers and moderation state. |
| FR-10 academic circles | Partially met | Circles, owners, membership, categories and university scope are implemented. Academic programme/module relationships exist, but users are not silently auto-enrolled into social circles solely from a module match. That behavioural change requires stakeholder and privacy justification. |
| FR-11 concurrency-safe reservations | Met | Event and service-slot rows are selected `FOR UPDATE` inside transactions before capacity is checked and written. Duplicate and overlap rules are also enforced. |
| FR-12 career pipeline | Met | Users can save opportunities and privately track Planning, Applied, Interview, Offer and Closed states. The interface presents the lifecycle without claiming to submit applications to employers. |
| FR-13 wellbeing/support intake | Partially met | Private support cases are routed by category, isolated by account and available to authorised administrators. Transport and managed-database encryption belong to the deployment boundary; the application must not claim field-level encryption that it does not implement. It is not a medical service. |
| FR-14 audit logging | Partially met | Privileged mutations append actor, university, resource, summary and timestamp records to `admin_audit_log`, and the application provides no update/delete workflow for them. “Immutable” requires database permissions or external write-once logging that has not yet been independently demonstrated. |

## Non-functional requirements reconciliation

| Manifest ID | Status | UniCircle evidence and decision |
| --- | --- | --- |
| NFR-1 BCrypt cost 12 | Met | Pilot/development passwords now use delegating BCrypt with an explicit cost of 12. The encoded value retains `{bcrypt}` algorithm identification for migration compatibility. Production OIDC avoids local university-password storage. |
| NFR-2 200 ms responses | Not yet evidenced | Bounded/indexed queries support responsiveness, but a universal 200 ms maximum cannot be claimed without a defined environment, dataset, concurrency and percentile. A controlled performance run remains required. |
| NFR-3 3NF | Substantially met | Eighteen Flyway migrations define separated identity, membership, messages, schedules, bookings, moderation and audit relations with keys and constraints. The final dissertation should provide a dependency-based 1NF/2NF/3NF justification rather than only an ERD. |
| NFR-4 WCAG 2.1 AA | Partially evidenced and updated | UniCircle targets the newer WCAG 2.2 AA, with semantic labels, focus/mobile structure and an accessibility statement. Automated and manual audit results are still required before claiming conformance. Contrast must be measured, not inferred from appearance. |
| NFR-5 75% coverage | Met | Maven verification produces JaCoCo reports and fails the build below 75% line coverage. The latest full run is recorded in `requirements-traceability.md`. Coverage supports test assessment but is not treated as proof of correctness. |
| NFR-6 deterministic container pipeline | Met for build verification | The multi-stage Dockerfile runs the same `clean verify` quality gate before creating a non-root Java 21 runtime image. GitHub Actions also runs `clean verify`. Exact image digest pinning and promotion approvals are future production controls. |
| NFR-7 CORS protection | Met by same-origin boundary | The browser application and API are served from the same origin and do not expose a cross-origin API contract. Spring Security authentication, CSRF, CSP and default cross-origin restrictions remain enabled. Adding permissive CORS headers would weaken this boundary. A future authorised client must introduce an explicit origin allow-list. |
| NFR-8 50 concurrent sockets | Not yet evidenced | The gateway uses concurrent session collections, cleans up closed/error sessions and has automated behavioural tests. Stability for 50 concurrent clients requires a repeatable load test with memory and error measurements. |
| NFR-9 validation/XSS | Partially evidenced | Jakarta Validation, database constraints, length/type bounds, safe DOM `textContent` use and CSP provide layered protection. Input validation cannot “completely block XSS”; untrusted data must also be contextually encoded at every output sink. A focused dynamic security test remains valuable. |
| NFR-10 cloud backup/failover | Not yet evidenced | PostgreSQL production configuration exists, but snapshots, restore objectives and cross-region recovery are provider/institution controls. A backup is not evidence until a restore has been tested and recorded. |

## Changes made from this review

1. Pilot and development password creation now uses BCrypt with an explicit cost of 12 and delegating algorithm identifiers.
2. The password-recovery integration journey asserts that a newly registered credential is stored with the cost-12 BCrypt format.
3. The administrator suite now explicitly verifies that an unauthenticated API request returns `401`, while an authenticated non-administrator returns `403`.
4. The Docker build now runs `mvn --batch-mode clean verify`; it no longer skips tests before producing the runtime image.
5. The existing location-purge test and 75% JaCoCo build gate remain direct evidence for FR-6 and NFR-5.
6. A global API exception handler now returns stable timestamp, status, code, detail and field-error properties without exposing stack traces or exception types.
7. Service availability creation now locks the parent service and rejects any active slot whose time range overlaps the proposed range.
8. Learning-session creation now serialises schedule checks by university and rejects overlapping sessions for the same module code, case-insensitively.
9. Automated tests directly verify the error contract, absence of internal trace fields, service-slot overlap rejection and module-session overlap rejection.

## Actual source map for the workspace manifest

The workspace manifest names React/TypeScript and controller files that do not exist in this Java and progressive-web-app implementation. The final report must cite the real evidence below instead.

| Manifest label | Actual UniCircle evidence |
| --- | --- |
| `DashboardShell.tsx`, `MetricsGrid.tsx` | `src/main/resources/static/index.html`, `src/main/resources/static/js/app.js`, campus schedule/information services |
| `AnnouncementFeed.tsx`, `CampusUpdateController.java` | update views in `index.html`/`app.js`, `InformationController`, `InformationService`, `AdminController`, `AdminService` |
| `ClassroomScheduler.tsx`, `ClassLiveController.java` | live-class views in `index.html`/`app.js`, `InformationController`, `InformationService` |
| `CirclesGrid.tsx`, `CommunityGroupController.java` | circle views in `index.html`/`app.js`, `CommunityController`, `CommunityService` |
| `ChatLayout.tsx`, `ChatSocketHandler.java` | message views in `index.html`/`app.js`, `SocialController`, `SocialService`, `SocialRealtimeGateway` |
| `EventsCatalog.tsx`, `CampusEventController.java` | event views in `index.html`/`app.js`, `EngagementController`, `EngagementService` |
| `BookingGrid.tsx`, `ServiceFacilityController.java` | booking views in `index.html`/`app.js`, `EngagementController`, `EngagementService`, administrator publication methods |
| `CareerHub.tsx`, `CareerTrackerController.java` | opportunity views in `index.html`/`app.js`, opportunity controller/service package |
| `SupportCenter.tsx`, `StudentCareController.java` | support views in `index.html`/`app.js`, `InformationController`, `InformationService` |
| `UpdateSecurityTest.java`, `AdminSecurityIntegrationTest.java` | `AdminControllerTests`, `DemoSignInJourneyTests`, `PilotFriendsJourneyTests` |
| `WebSocketLatencyLoadVerification` | No such test exists; behavioural signalling tests exist, while load measurement remains an evidence gap |
| generated JPA `com.unicircle.pilot.entity.*` examples | Actual schema is the 18 versioned Flyway migrations plus existing identity entities and JDBC services |

The generated examples are not copied because they would create a second, conflicting domain model and falsely suggest implementation evidence that is absent from the repository.

## Evidence still required for a production or first-class claim

- Official module marking grid mapped to report sections and evidence.
- Approved primary research and usability study results.
- WCAG 2.2 automated and manual audit with remediation/retest evidence.
- Controlled HTTP and WebSocket load tests, including latency percentiles, error rate and memory observations.
- Dependency/application security review and recorded remediation.
- Managed PostgreSQL backup and restore rehearsal.
- Institutional OIDC, privacy, safeguarding, retention and operational approvals.
- Database/external controls if the audit log is to be described as tamper-resistant or immutable.

These are evidence gaps, not reasons to invent features or results. The final report should distinguish implemented pilot behaviour, measured results and future institutional controls.
