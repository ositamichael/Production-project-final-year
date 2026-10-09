# UniCircle

**Where campus meets connection.** UniCircle is a multi-university campus platform designed to bring people, schedules, communities, messaging, events, appointments, opportunities and trusted support into one coherent experience.

This repository is being rebuilt as a Level 6 final-year software engineering project. It previously contained the ScamShield prototype. That work remains recoverable in Git history and the `archive/scamshield` reference; the active product direction is now UniCircle.

## Current status

Sprint 1 established the testable engineering foundation, Sprint 2 added the account-backed identity and profile layer, Sprint 3 established the university-integration boundary, Sprint 4 added a complete authenticated demonstration journey, Sprint 5 added the first real social workflow, and Sprint 6 adds live multi-session communication and safety controls:

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

The dashboard now contains a working local social demonstration, but it is not evidence of a live university integration. Real student identity, timetables, bookings and location require institutional approval, integration contracts, security review and deployment infrastructure. Messages, notifications and reports are stored only in the local development database. Production push notifications, staffed safeguarding operations and retention policies remain future work.

## Run in IntelliJ IDEA

1. Open this repository's `pom.xml` as a project.
2. Set the project SDK to Java 21 or newer. IntelliJ's bundled Java 25 runtime can compile the Java 21 target.
3. Allow IntelliJ to import the Maven project.
4. Run `UniCircleApplication`.
5. Open `http://localhost:8080`.
6. Open the profile and choose **Sign in to fictional sandbox**. The local-only test account is:
   - Email: `student@demo.unicircle.test`
   - Password: `CampusDemo!2026`

The sign-in page also offers fictional Aisha, James and Sophie accounts using the same development-only password. Use separate browser profiles or a private window to demonstrate connection decisions and live messaging between two people.

These credentials are deliberately fictional and exist only in the non-production profile. They can be overridden with `UNICIRCLE_DEMO_USERNAME` and `UNICIRCLE_DEMO_PASSWORD`. Never use a real university password or Authenticator code in this form.

No local PostgreSQL installation is required for the default profile. It uses an in-memory H2 database configured to behave like PostgreSQL for early development. Production uses PostgreSQL and environment variables documented in `.env.example`.

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

## Branching and assessment evidence

Each sprint is developed on a feature branch and merged through a pull request. Commits, tests, architecture decisions, sprint records and verification evidence are retained so the project demonstrates professional software engineering practice rather than only a polished interface.

## Licence and data

No production licence has yet been selected. Do not deploy the project with real personal data until the university has approved the privacy, information-security, accessibility, retention and safeguarding arrangements.
