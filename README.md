# UniCircle

**Where campus meets connection.** UniCircle is a Level 6 final-year software engineering project for exploring a secure, modular web platform that brings selected campus people, communities, messaging, events, services, opportunities and support journeys into one interface.

UniCircle is an independent academic prototype using fictional or specifically approved data. It is **not** an official Leeds Beckett University service, it does not use live university student records, and it must not be used as a live student-information, emergency or safeguarding system.

The repository previously contained a ScamShield prototype. That work remains recoverable in Git history; the active project direction is UniCircle.

## Problem being investigated

Students may need to use several disconnected channels to find people and campus information, communicate, join activities and arrange selected services. UniCircle investigates whether a controlled, account-owned web prototype can combine representative journeys while retaining university boundaries, privacy, consent and safety controls.

Sector and academic evidence is recorded in the research evidence pack. It supports investigation of the problem; it does not establish a Leeds Beckett problem prevalence, prove improved retention or measure student outcomes.

## Project aims and objectives

The project aims to design, implement and critically evaluate a secure, accessible and modular prototype using fictional or approved campus data.

The working objectives are to:

- create and protect university-scoped student accounts and profiles;
- enforce ownership, role, connection and conversation permissions;
- provide persistent messaging, circles, events, bookings and campus information;
- include consent, blocking, reporting and moderation controls;
- use repeatable migrations and an automated verification strategy;
- preserve traceability from research and requirements to implementation and tests;
- distinguish demonstrated workflows from unverified institutional integrations; and
- record limitations, risks and remaining work honestly.

## Implemented features

The repository contains a working Spring Boot prototype and responsive web interface. Implemented areas include:

- university-scoped profiles, academic context and fictional/demo sign-in;
- invitation-only pilot accounts, password recovery and strong password hashing;
- same-university discovery, connection requests and consent-based private messaging;
- live signed-in WebSocket notifications and conversation updates;
- account blocking, safeguarding reports and staff-review workflows;
- circles, event registration, service booking and an account-owned schedule;
- moderated opportunities and private application tracking;
- moderated lost-and-found reports and private claim workflows;
- campus announcements, support cases and live-learning participation;
- permission-aware Test Campus administration and audit records;
- consent-led, approximate, short-lived location sharing;
- an isolated no-cost campus assistant and private SVG illustration workspace;
- optional public-knowledge and provider-based assistant modes;
- downloadable calendar files and an installable mobile web-app shell;
- privacy, acceptable-use and accessibility pages; and
- security headers including a Content Security Policy, Permissions Policy and strict referrer policy.

These are repository features, not evidence of university approval, production readiness or measured user outcomes. Optional and future integrations are identified in the limitations section.

## Technology stack

| Area | Technology |
| --- | --- |
| Language and runtime | Java 21 |
| Application framework | Spring Boot 4.1.1 |
| Architecture | Spring Modulith 2.1.1 modular monolith |
| Persistence | Spring Data JPA, Flyway, PostgreSQL; H2 for local/test profiles |
| Security | Spring Security, CSRF, OAuth2/OIDC client configuration |
| Real-time features | WebSocket/STOMP; WebRTC call signalling |
| Browser client | HTML, CSS and JavaScript in `src/main/resources/static` |
| Build and verification | Maven Wrapper, JaCoCo |
| Testing | Spring MVC/JPA/Security tests, Modulith and frontend-structure tests |
| Containerisation | Docker multi-stage build and Docker Compose for PostgreSQL |

The Maven parent is Spring Boot 4.1.1 and `pom.xml` sets Java 21.

## Architecture

UniCircle begins as a modular monolith so that a single student project can preserve local transactions and a simple deployment while keeping functional boundaries explicit.

```text
Responsive browser/PWA
        |
        | HTTPS, session cookie and WebSocket
        v
Spring Security
        |
        v
Spring Boot modular monolith
        |
        +-- identity and campus context
        +-- social, communities and safety
        +-- events, bookings and schedule
        +-- information and support
        +-- administration and optional extensions
        |
        v
Flyway-managed PostgreSQL schema
```

The browser never supplies the authoritative university or account identifier. Current identity and tenant context are derived on the server. H2 is used for the default local profile and the pilot-local rehearsal profile; PostgreSQL is the production and hosted-pilot database.

## Repository structure

```text
.github/workflows/       Maven CI quality check
.mvn/wrapper/            Maven Wrapper configuration
docs/                    Architecture, ADR, sprint, QA and research evidence
output/pdf/              Exported assessment evidence packs
scripts/test-calls.mjs   Isolated JavaScript call-logic tests
src/main/java/           Spring application modules
src/main/resources/
  application*.yml       Default, pilot and production configuration
  db/migration/          Flyway SQL migrations
  static/                Browser HTML, CSS, JavaScript and PWA assets
src/test/java/           Maven unit, integration and architecture tests
.env.example             Deployment environment-variable template
compose.yml              Local PostgreSQL Compose service
Dockerfile               Java 21 application image build
pom.xml                  Maven dependencies and quality gates
```

## Prerequisites

- JDK 21 or newer for the Java 21 target.
- Git.
- IntelliJ IDEA for the IDE workflow, or Windows PowerShell for command-line use.
- Docker Desktop only for the optional PostgreSQL Compose database.
- Node.js only to run `scripts/test-calls.mjs`.

The repository includes `mvnw` and `mvnw.cmd`; a separate Maven installation is not required.

## Environment configuration

The default `application.yml` profile runs with an in-memory H2 PostgreSQL-compatible database and does not require PostgreSQL or external provider credentials.

`.env.example` is a deployment template. Do not commit real secrets. Configuration values used by the application include:

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Activates a Spring profile such as `pilot` or `prod` |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | PostgreSQL connection values for the `prod` profile |
| `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET`, `OIDC_ISSUER_URI` | Approved OIDC provider values for production |
| `UNICIRCLE_DEMO_USERNAME`, `UNICIRCLE_DEMO_PASSWORD` | Default-profile fictional demo credentials |
| `UNICIRCLE_ASSISTANT_MODEL`, `UNICIRCLE_ASSISTANT_IMAGE_MODEL` | Optional assistant provider model names |
| `UNICIRCLE_MEETING_PROVIDER_BASE_URL` | Optional provider boundary for meeting rooms |
| `UNICIRCLE_STUN_URLS` | Comma-separated WebRTC STUN URLs |
| `UNICIRCLE_TURN_URLS`, `UNICIRCLE_TURN_USERNAME`, `UNICIRCLE_TURN_CREDENTIAL` | TURN provider pair |
| `UNICIRCLE_TURN_SHARED_SECRET`, `UNICIRCLE_TURN_CREDENTIAL_TTL_SECONDS` | Optional coturn shared-secret credential mode |
| `UNICIRCLE_CALL_ICE_TRANSPORT_POLICY` | `all` or `relay` |
| `UNICIRCLE_PROFILE_PHOTOS_DIR`, `UNICIRCLE_LOST_FOUND_PHOTOS_DIR` | Local upload storage directories |
| `UNICIRCLE_BOOTSTRAP_ADMIN_EMAIL` | Optional pilot bootstrap administrator |
| `PILOT_INVITE_CODE` | Required random code for the `pilot` profile |
| `PORT` | Hosted-server port, default `8080` |

Optional assistant provider variables documented in `.env.example` include `OPENAI_API_KEY`, `UNICIRCLE_XIRSYS_IDENT`, `UNICIRCLE_XIRSYS_SECRET` and `UNICIRCLE_XIRSYS_CHANNEL`. Provider credentials remain server-side secrets.

## Database configuration

### Default local database

The default profile uses:

```text
H2 in-memory database
URL: jdbc:h2:mem:unicircle;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
Schema management: Flyway validate
```

No local PostgreSQL installation is required. Restarting the application resets this database.

### Local PostgreSQL

`compose.yml` starts only PostgreSQL:

```powershell
docker compose up -d postgres
docker compose ps
```

The compose service uses the database name and credentials declared in `compose.yml`: `unicircle`, `unicircle` and `local-development-only`, exposed on port `5432`. These values are local-development values, not production credentials.

To activate PostgreSQL with a Spring profile, set matching environment variables and use the `prod` profile. The `prod` profile expects `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET` and `OIDC_ISSUER_URI`; do not substitute placeholder OIDC credentials for a live provider.

### Invitation-only local pilot

Set `PILOT_INVITE_CODE` to a random value of at least 32 characters and activate `pilot,pilot-local`. The pilot-local profile uses a persistent file in `unicircle-pilot.mv.db`; do not commit that database file and do not expose the local profile to the public internet.

## How to run UniCircle in IntelliJ IDEA

1. Open the repository's `pom.xml` as a Maven project.
2. Select JDK 21 or newer as the project SDK.
3. Allow IntelliJ to import Maven dependencies.
4. Open `UniCircleApplication`.
5. Run with the default configuration to use the in-memory H2 profile.
6. Open <http://localhost:8080>.

For the default demo sign-in, use the profile-configured demo account only in local/demo use:

```text
Email: student@demo.unicircle.test
Password: CampusDemo!2026
```

These credentials are fictional. `UNICIRCLE_DEMO_USERNAME` and `UNICIRCLE_DEMO_PASSWORD` may override them. Never enter a real university password or Authenticator code.

For a local invitation pilot, create a run configuration with active profiles `pilot,pilot-local` and set `PILOT_INVITE_CODE`. Use separate browser profiles or devices for multi-account journeys.

## How to run it from Windows PowerShell

From the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

Open <http://localhost:8080>.

To build and verify first:

```powershell
.\mvnw.cmd clean verify
java -jar .\target\unicircle-0.0.1-SNAPSHOT.jar
```

The packaged JAR name is `unicircle-0.0.1-SNAPSHOT.jar`.

For an invitation-only local pilot:

```powershell
$env:PILOT_INVITE_CODE = "<private-random-code-at-least-32-characters>"
$env:SPRING_PROFILES_ACTIVE = "pilot,pilot-local"
.\mvnw.cmd spring-boot:run
```

Clear the pilot variables after the run if needed:

```powershell
Remove-Item Env:PILOT_INVITE_CODE
Remove-Item Env:SPRING_PROFILES_ACTIVE
```

## How to confirm the application is working

1. Start the application.
2. Open <http://localhost:8080>.
3. Check <http://localhost:8080/actuator/health>; the expected response contains `"status":"UP"`.
4. Open the sign-in page and use only the documented fictional/demo account or a private invitation-code pilot account.
5. Confirm the page does not request a university password or Microsoft/Authenticator credential.
6. Exercise a relevant positive journey and a negative access attempt rather than only checking the home page.

Production and hosted-pilot verification must use HTTPS, managed PostgreSQL and explicit provider configuration. The H2 happy path is not a deployment or security result.

## How to run automated tests

Maven suite and coverage gate:

```powershell
.\mvnw.cmd clean verify
```

PowerShell wrapper:

```powershell
.\mvnw.cmd test
```

Git Bash/Linux/macOS:

```bash
./mvnw clean verify
```

The suite includes Spring application, security, controller, persistence, frontend-structure and Spring Modulith architecture checks. JaCoCo enforces at least 75% line coverage.

Run the isolated JavaScript call-logic tests where Node.js is available:

```powershell
node --test scripts/test-calls.mjs
```

This script uses simulated media and RTC objects. It is not evidence of real microphones, speakers, physical devices or a live TURN relay.

Browser/device acceptance is not covered by an automated Playwright or Cypress suite in this repository. Use the recorded manual test checklists and report browser/device limitations separately.

## Research and requirements evidence

- [Literature review](docs/research/literature-review.md)
- [Source evaluation log](docs/research/source-evaluation-log.md)
- [Requirements and scope baseline](docs/assessment/requirements-and-scope.md)
- [Requirements traceability](docs/assessment/requirements-traceability.md)
- [UniCircle research and requirements evidence pack (PDF)](output/pdf/UniCircle_Research_and_Requirements_Evidence_Pack.pdf)

These documents contain research synthesis, scope decisions and requirement mappings. They do not report participant findings or approval that has not been recorded in the repository.

## Architecture and design evidence

- [Architecture](docs/architecture.md)
- [Architecture evidence](docs/assessment/architecture-evidence.md)
- [Production manifest reconciliation](docs/assessment/production-manifest-reconciliation.md)
- [ADR 0001: platform and language](docs/adr/0001-platform-and-language.md)
- [ADR 0002: modular monolith](docs/adr/0002-modular-monolith.md)
- [ADR 0003: identity and tenancy](docs/adr/0003-identity-and-tenancy.md)
- [Campus pilot and deployment guide](docs/friends-pilot.md)
- [Calling across networks](docs/calling-across-networks.md)
- [Release-readiness review](docs/qa/release-readiness-2026-10-14.md)
- [Test Campus evidence](docs/qa/test-campus-evidence-2026-10-14.md)

Sprint records and implementation evidence are listed by numbered document from [`docs/sprints/01-foundation.md`](docs/sprints/01-foundation.md) through [`docs/sprints/20-test-campus-administration.md`](docs/sprints/20-test-campus-administration.md).

## Known limitations and remaining work

- Live Leeds Beckett or third-party institutional identity, records and schedules are not implemented or approved.
- H2 is useful for development and tests; managed PostgreSQL, migrations, backup and restore require formal rehearsal.
- Core single-instance WebSocket and session behaviour needs a shared broker and horizontal-scaling design before a larger deployment.
- Call success across physical devices and restrictive networks requires real TURN/relay testing.
- Location, messaging and optional AI features need real-device and cross-network privacy and safety evaluation.
- Independent accessibility, penetration and performance testing have not established conformance or production thresholds.
- Participant research must not begin before the required ethical approval and approved data-collection plan.
- Production push notifications, secure institutional media storage, retention, deletion and fully governed moderation remain future work.
- A production licence has not been selected.

Do not present implemented code or browser-emulation screenshots as proof of accessibility conformance, security certification, performance, research outcomes or university endorsement.

## Ethics, privacy and safety boundaries

- Demonstration users and campus data are fictional.
- UniCircle is not an official Leeds Beckett University service.
- The application must not request real university passwords or Authenticator codes.
- It does not use live university student records.
- Participant research must not begin before favourable ethical approval and a valid approved participant/data plan.
- University data, account data and participant data must remain purpose limited and access controlled.
- Location sharing is off by default and is described as voluntary, approximate and short-lived.
- The system must not replace emergency services, official support or institutional safeguarding operations.
- Research evidence and public claims must include methods, scope and limitations.

No academic-integrity declaration is added or modified here. It must remain the author's university/assessment responsibility.

## Deployment information

The Docker build uses Maven verification, Java 21 and a non-root Java 21 runtime. `compose.yml` provides PostgreSQL for local development only.

The hosted-pilot guide describes a one-instance staging rehearsal. Set the `pilot` profile, `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `PILOT_INVITE_CODE` and any reviewed TURN settings on the hosting platform. The application health endpoint is `/actuator/health`.

Production OIDC configuration is an integration boundary, not evidence of university approval. Before institutional or public deployment, the project requires formal identity and data agreements, managed secrets, HTTPS, monitoring, backups, operational ownership, independent security and accessibility review, and approved data-governance processes.

## Author and academic-project notice

**Author:** Michael Osita

This repository is a Level 6 final-year production project. The source, evidence and limitations should be treated as an academic engineering artefact in active development, not as an endorsed, certified or live university service.
