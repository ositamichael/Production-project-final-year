# UniCircle

**Where campus meets connection.** UniCircle is a multi-university campus platform designed to bring people, schedules, communities, messaging, events, appointments, opportunities and trusted support into one coherent experience.

This repository is being rebuilt as a Level 6 final-year software engineering project. It previously contained the ScamShield prototype. That work remains recoverable in Git history and the `archive/scamshield` reference; the active product direction is now UniCircle.

## Current status

Sprint 1 established the testable engineering foundation, Sprint 2 added the account-backed identity and profile layer, Sprint 3 established the university-integration boundary, Sprint 4 added a complete authenticated demonstration journey, Sprint 5 added the first real social workflow, Sprint 6 added live multi-session communication and safety controls, Sprint 7 added transactional campus events and service bookings, Sprint 8 added moderated opportunities and personal application tracking, Sprint 9 added secure lost-and-found reporting, Sprint 10 added a fictional staff review workflow, and Sprint 11 expands the invitation-only pilot into a testable campus experience:

- Spring Boot 4.1.1 with Java 21 as the target language level.
- A responsive desktop, tablet and mobile dashboard prototype.
- Spring Security with production OIDC configuration for university single sign-on.
- A multi-university relational schema using explicit `university_id` boundaries.
- A searchable same-university student directory for new and existing permitted profiles.
- Persisted connection requests with recipient-only accept and decline decisions.
- Human-to-human private messaging that activates only after connection consent.
- Persisted direct conversations and messages with server-enforced membership checks.
- Authenticated WebSocket events that refresh messages and notifications across signed-in browser sessions.
- Account-specific, database-backed notifications and unread counts.
- Immediate account blocking that removes private messaging permission.
- Confidential safeguarding reports with validated reasons and an auditable review state.
- A university-scoped event catalogue with account-specific registration, cancellation, remaining-capacity calculation and schedule integration.
- Service offerings and bookable time slots for appointments, facilities and campus support.
- Transactional booking controls that reject duplicate, overlapping and fully occupied requests.
- Account-specific booking history, cancellation and confirmation notifications.
- A composed personal schedule that combines registered events with confirmed service bookings.
- University-scoped jobs, placements and volunteering from verified fictional publishers.
- Server-enforced moderation and publisher-verification gates that prevent unapproved listings appearing.
- Account-specific saved opportunities and a private application-stage tracker.
- Search, type filtering, detailed eligibility, skills, working pattern and deadline information.
- University-scoped lost-and-found reports with type, category and text search.
- Moderation-gated student reports that remain private until approved.
- Private identifying details and ownership evidence that never appear in public catalogue responses.
- Account-specific report history, private claim tracking, withdrawal and reporter notifications.
- Validated PNG/JPEG item photographs that are re-encoded to strip embedded metadata.
- A fictional service-desk reviewer with a staff-only queue, audited approve/reject decisions and private reporter feedback.
- Four separate fictional student sign-ins for demonstrating requests and chat between people.
- Flyway migrations, PostgreSQL production configuration and an H2 local profile.
- Health monitoring, modular architecture checks, unit tests and CI.
- Honest prototype labels wherever live university data is not yet connected.
- Database-backed profile details and a protected profile API.
- Validated PNG/JPEG profile-photograph processing that strips embedded metadata before local development storage.
- A protected academic-context API for programmes, modules and privacy-filtered coursemates.
- Fictional university fixtures that are visibly separated from future approved institutional data.
- A development-only fictional student sign-in with a real Spring Security session and secure logout.
- A dedicated sign-in experience that explicitly prevents the use of Leeds Beckett or Microsoft credentials.
- API requests return a clear unauthorised response rather than leaking protected profile information or returning a login page.
- Separate invitation-only pilot accounts with strong password hashing and database-backed profiles.
- Persistent pilot profile photos, messages and short-lived consent-based locations.
- Live WebRTC voice and video calls inside accepted private conversations, with authenticated signalling, consent prompts, mute/camera controls and no application recording.
- Location sharing that is off by default, mutual between accepted connections, rounded and automatically expired.
- A Docker deployment path and a focused campus-pilot test guide for a single-instance Render rehearsal.
- A five-destination mobile navigation bar matching the mobile product design: My Campus, Circles, Messages, Events and More.
- Self-declared course and year onboarding, privacy-filtered pilot coursemates and clearly labelled non-institutional academic context.
- Pilot event, service and opportunity catalogues with account-specific registration, booking, saving and tracking workflows.

The dashboard now contains working local social, event-registration, booking, opportunity-tracking, lost-and-found and fictional staff-review demonstrations, but it is not evidence of a live university integration. Real student identity, official timetables, university booking systems, employer vacancy feeds, staffed lost-property services and location require institutional approval, integration contracts, security review and deployment infrastructure. Messages, registrations, bookings, saved listings, tracker stages, notifications, item reports and claims are stored only in the local development environment. Production push notifications, institutionally governed staff moderation and safeguarding operations, secure media storage and retention policies remain future work.

## Run in IntelliJ IDEA

1. Open this repository's `pom.xml` as a project.
2. Set the project SDK to Java 21 or newer. IntelliJ's bundled Java 25 runtime can compile the Java 21 target.
3. Allow IntelliJ to import the Maven project.
4. Run `UniCircleApplication`.
5. Open `http://localhost:8080`.
6. Open the profile and choose **Sign in to fictional sandbox**. The local-only test account is:
   - Email: `student@demo.unicircle.test`
   - Password: `CampusDemo!2026`

The sign-in page also offers fictional Aisha, James and Sophie student accounts and fictional service-desk reviewer Alex Morgan using the same development-only password. Use separate browser profiles or a private window to demonstrate connection decisions, live messaging and staff review.

These credentials are deliberately fictional and exist only in the non-production profile. They can be overridden with `UNICIRCLE_DEMO_USERNAME` and `UNICIRCLE_DEMO_PASSWORD`. Never use a real university password or Authenticator code in this form.

No local PostgreSQL installation is required for the default profile. It uses an in-memory H2 database configured to behave like PostgreSQL for early development. Production uses PostgreSQL and environment variables documented in `.env.example`.

For the separate invitation-only test-account flow and Render rehearsal instructions, see [`docs/friends-pilot.md`](docs/friends-pilot.md). Pilot participants must never enter university credentials or Authenticator codes.

## Command-line checks

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

## Product boundaries

- People messaging is human-to-human. The campus information assistant is a separate, clearly labelled feature.
- Location sharing will be off by default and limited to people explicitly accepted by the user.
- UniCircle will never ask users to type their university password into the application. Production sign-in redirects to the university identity provider through OIDC.
- The project does not claim that Leeds Beckett University or any other institution has approved, deployed or endorsed it.
- Fixtures shown in the dashboard are demonstration data, not live records.

## Architecture

UniCircle begins as a modular monolith. This keeps deployment manageable for a final-year project while enforcing boundaries that can later support independent services. See:

- [`docs/architecture.md`](docs/architecture.md)
- [`docs/adr/0001-platform-and-language.md`](docs/adr/0001-platform-and-language.md)
- [`docs/adr/0002-modular-monolith.md`](docs/adr/0002-modular-monolith.md)
- [`docs/adr/0003-identity-and-tenancy.md`](docs/adr/0003-identity-and-tenancy.md)
- [`docs/sprints/01-foundation.md`](docs/sprints/01-foundation.md)
- [`docs/sprints/02-identity-profile.md`](docs/sprints/02-identity-profile.md)
- [`docs/sprints/03-university-context.md`](docs/sprints/03-university-context.md)
- [`docs/sprints/04-demo-sign-in.md`](docs/sprints/04-demo-sign-in.md)
- [`docs/sprints/05-connections-messaging.md`](docs/sprints/05-connections-messaging.md)
- [`docs/sprints/06-realtime-safety.md`](docs/sprints/06-realtime-safety.md)
- [`docs/sprints/07-events-bookings.md`](docs/sprints/07-events-bookings.md)
- [`docs/sprints/08-opportunities.md`](docs/sprints/08-opportunities.md)
- [`docs/sprints/09-lost-found.md`](docs/sprints/09-lost-found.md)
- [`docs/sprints/10-lost-found-staff-review.md`](docs/sprints/10-lost-found-staff-review.md)
- [`docs/sprints/11-campus-pilot.md`](docs/sprints/11-campus-pilot.md)

## Branching and assessment evidence

Each sprint is developed on a feature branch and merged through a pull request. Commits, tests, architecture decisions, sprint records and verification evidence are retained so the project demonstrates professional software engineering practice rather than only a polished interface.

## Licence and data

No production licence has yet been selected. Do not deploy the project with real personal data until the university has approved the privacy, information-security, accessibility, retention and safeguarding arrangements.
