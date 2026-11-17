# Sprint 1 — Engineering foundation

## Goal

Create a reproducible Spring Boot foundation and a responsive dashboard shell that demonstrates the intended UniCircle experience without pretending that university integrations are already live.

## Included

- Java 21 / Spring Boot project generated from the official Spring Initializr.
- Responsive application shell for desktop, tablet and mobile layouts.
- A My Campus overview dashboard plus dedicated Today, Circles, Messages, Events, Services, Opportunities, Map, Wellbeing, Lost and Found, and campus-guide workspaces.
- A consistent inline SVG icon system for navigation, clubs, events, services, opportunities and lost-property items; profile areas remain reserved for future user-uploaded photographs.
- Keyboard-visible controls, semantic landmarks, reduced-motion support and mobile navigation.
- Prototype interactions for page navigation, searching, joining circles, saving items, booking selection, messaging preview and campus guidance.
- Security boundary with a production OIDC profile.
- Multi-university identity, connection, conversation and message schema.
- Flyway versioned migration, health endpoint and platform-status endpoint.
- Unit, context and architecture tests.
- CI workflow and architecture decision records.

## Excluded from this sprint

- Live Leeds Beckett or other university sign-in.
- Production timetable, room, library, restaurant, payment or student-record integrations.
- Real direct messaging or push notifications.
- Background location tracking.
- App Store or Play Store packages.

## Acceptance checks

- `./mvnw clean verify` succeeds with a suitable JDK and network access for the first dependency download.
- The application starts and serves the dashboard at `/`.
- `/actuator/health` reports the application health without exposing internal details.
- Flyway applies the initial schema successfully.
- The dashboard reflows without horizontal overflow at mobile, tablet and desktop widths.
- Every primary menu item opens its own labelled workspace; My Campus remains the consolidated dashboard.
- Prototype and integration limitations are visible to users and in project documentation.
