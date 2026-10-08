# UniCircle

**Where campus meets connection.** UniCircle is a multi-university campus platform designed to bring people, schedules, communities, messaging, events, appointments, opportunities and trusted support into one coherent experience.

This repository is being rebuilt as a Level 6 final-year software engineering project. It previously contained the ScamShield prototype. That work remains recoverable in Git history and the `archive/scamshield` reference; the active product direction is now UniCircle.

## Current status

Sprint 1 establishes a testable engineering foundation:

- Spring Boot 4.1.1 with Java 21 as the target language level.
- A responsive desktop, tablet and mobile dashboard prototype.
- Spring Security with production OIDC configuration for university single sign-on.
- A multi-university relational schema using explicit `university_id` boundaries.
- Foundations for consent-based connections and human-to-human conversations.
- Flyway migrations, PostgreSQL production configuration and an H2 local profile.
- Health monitoring, modular architecture checks, unit tests and CI.
- Honest prototype labels wherever live university data is not yet connected.

The dashboard is a working interface prototype, not evidence of a live university integration. Real student identity, timetables, bookings, location and messaging require institutional approval, integration contracts, security review and deployment infrastructure.

## Run in IntelliJ IDEA

1. Open this repository's `pom.xml` as a project.
2. Set the project SDK to Java 21 or newer. IntelliJ's bundled Java 25 runtime can compile the Java 21 target.
3. Allow IntelliJ to import the Maven project.
4. Run `UniCircleApplication`.
5. Open `http://localhost:8080`.

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

## Branching and assessment evidence

Each sprint is developed on a feature branch and merged through a pull request. Commits, tests, architecture decisions, sprint records and verification evidence are retained so the project demonstrates professional software engineering practice rather than only a polished interface.

## Licence and data

No production licence has yet been selected. Do not deploy the project with real personal data until the university has approved the privacy, information-security, accessibility, retention and safeguarding arrangements.
