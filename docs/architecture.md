# UniCircle architecture

**Audited:** 6 November 2026
**Architecture style:** Spring Boot modular monolith
**Authoritative implementation:** source code, Flyway migrations and automated tests

## 1. Architectural objective

UniCircle must demonstrate a professional but proportionate Level 6 design. One student must be able to build, test, deploy and explain it, while sensitive operations remain separated by explicit functional boundaries. A modular monolith is used because it preserves local transactions and a single deployment without introducing premature distributed-system complexity.

The architecture is not described as institutionally production ready. Live university identity, records, email, meeting providers and support operations require separate approval, contracts, security review and governance.

## 2. System shape

```text
Responsive browser/PWA
        |
        | HTTPS, session cookie and WebSocket events
        v
Spring Security filter chain
        |
        v
Spring Boot modular monolith
        |
        +-- identity and profiles
        +-- social connections, messaging and safety
        +-- communities
        +-- events, services, bookings and schedule
        +-- campus context and information
        +-- administration and evaluation evidence
        +-- optional extensions
        |
        v
PostgreSQL schema managed by Flyway

Optional approved boundaries:
OIDC identity provider | meeting/TURN provider | screened public knowledge/provider APIs
```

## 3. Implemented application modules

The module names below match the direct Java packages under `uk.ac.unicircle`. This corrects the earlier conceptual diagram, which showed several capabilities as separate modules even though they are implemented together.

| Package/module | Implemented responsibility | Allowed Java-module dependencies | Scope position |
| --- | --- | --- | --- |
| `identity` | Current identity, accounts, roles, profiles, profile media, pilot registration/recovery | None declared outside itself | Core Must |
| `campus` | Fictional/adapter-based academic and university context | `identity` | Core Must |
| `social` | Directory, connection state, conversations, messages, notifications, blocking, safety reports, call signalling | `identity` | Core Must; calling is Could |
| `community` | University-scoped circles and membership | `identity` | Core Must |
| `engagement` | Events, service offerings, slots, bookings, schedule and calendar export | `identity` | Core Must |
| `location` | Mutual, short-lived approximate location sharing and purge | `identity` | Should |
| `information` | Announcements, support cases and live-learning meetings | `identity`, `social` | Support is Should; other parts Could |
| `search` | Permission-aware search over selected campus records | `identity` | Should |
| `admin` | Test Campus privileged operations and audit records | `identity`, `social` | Core controls only; expanded console Could |
| `opportunity` | Moderated opportunities and private tracking | `identity` | Could |
| `lostfound` | Moderated reports, private claims, media and staff review | `identity` | Could |
| `assistant` | Local assistant, optional public/provider lookup and conversation history | No domain import identified | Could |
| `evaluation` | Persisted staging scenario results | `identity` | Evidence support; not participant-study proof |
| `platform` | Honest platform/integration status | None | Supporting |
| `config` | Environment security chains, headers, authentication and scheduling | None | Cross-cutting infrastructure |

`ArchitectureTests.modularBoundariesAreValid` asks Spring Modulith to verify package cycles and declared Java dependencies. It does not prove correct authorisation, database ownership, network security or deployability; those require separate tests and review.

## 4. Dependency direction

`identity` is the shared foundation because other modules require the authenticated account and university context. Feature modules should not depend directly on one another unless the dependency is explicit and justified. The two current cross-feature dependencies are:

- `information -> social`, used for real-time notification/event publication; and
- `admin -> social`, used for real-time changes after privileged operations.

These dependencies are now explicit in the Spring Modulith declarations. Future feature-to-feature imports require an ADR or refactoring to an event/public interface.

## 5. Data architecture

PostgreSQL is the authoritative production/rehearsal store and Flyway is the only supported schema-evolution route. H2 is used for local/test profiles with PostgreSQL compatibility where practical.

The schema uses:

- UUID identifiers;
- foreign keys for referential integrity;
- `university_id` on tenant-sensitive root records;
- unique constraints for identities, direct relationships, registrations and bookings;
- check constraints for states, roles, time ranges and coordinates;
- join tables for many-to-many membership; and
- audit tables for selected privileged decisions.

Important limitation: most services use JDBC against one shared schema. Spring Modulith verifies Java package dependencies but cannot prevent one module querying another module's tables. Table ownership is therefore a documented convention rather than a technically enforced boundary. A future improvement is to publish module repositories/interfaces or database views and add architecture rules around SQL ownership.

## 6. Identity, tenancy and authorisation

Three authentication modes share the same server-side authorisation model:

1. **Development:** seeded fictional in-memory users.
2. **Staging pilot:** database-backed fictional/pilot accounts with encoded passwords.
3. **Production profile:** OIDC redirect to an approved provider.

The browser must never provide the authoritative university context. `CurrentIdentity` resolves the signed-in account and university on the server. Services then apply role, ownership, membership, connection and tenant rules before accessing or mutating data.

OIDC configuration demonstrates an integration boundary only. It is not evidence that Leeds Beckett or another institution has registered or approved UniCircle.

## 7. API and browser security design

- Protected APIs return HTTP 401 rather than an HTML sign-in page.
- State-changing browser requests retain Spring Security CSRF protection.
- Session cookies are secure, HTTP-only and SameSite=Lax in the production profile.
- Content Security Policy, Permissions Policy and strict-origin referrer policy are applied.
- Passwords use an upgrade-capable bcrypt encoder.
- Validation occurs at request and domain/database layers.
- Private WebSocket/call activity is authorised against the accepted relationship.
- Sensitive provider credentials remain server side.

These controls support the security requirements but do not constitute a penetration test or complete OWASP ASVS verification.

## 8. Integration boundaries

| Integration | Current implementation | Required before a live institutional claim |
| --- | --- | --- |
| University identity | OIDC client configuration and local/pilot alternatives | Provider registration, issuer/client configuration, attribute/role agreement and security approval |
| Academic context | `UniversityContextGateway` with demo/pilot/unavailable implementations | Authorised API/data contract, retention and data-governance agreement |
| Email/notifications | In-application persisted notifications | Approved mail/push service and delivery/consent rules |
| Meeting/media | Provider-aware boundary and optional TURN configuration | Approved provider, data-processing terms, operational monitoring and network tests |
| Public/AI provider | Local fallback plus explicit opt-in external modes | Relevance to final scope, cost controls, privacy review, source handling and academic-integrity controls |

## 9. Deployment architecture

The container build runs Maven verification and produces a Java 21 runtime image under a non-root user. The production profile requires PostgreSQL and external OIDC/database secrets. TLS termination is expected at the managed platform or reverse proxy, with forwarded headers enabled.

The supplied Compose file starts PostgreSQL only; it is a local database aid, not a complete production topology. Backups, restore testing, central logging, alerting, horizontal scaling, persistent media storage and disaster recovery remain outside the current proven deployment.

## 10. Design quality gates

Architecture is acceptable for the assessed core only when:

1. Spring Modulith verification passes with the explicit dependency policies.
2. every Must requirement maps to a module and acceptance test;
3. schema migrations validate from an empty database;
4. the final ERD is generated from the migration-backed schema;
5. cross-university and cross-role negative tests pass;
6. the real booking race is tested concurrently;
7. external integrations remain visibly fictional/unavailable unless approved; and
8. diagrams and ADRs match the final submitted code rather than the original plan.

## 11. Known architecture risks

- `identity` is a widely used foundation and can become a high-coupling module.
- `social` currently combines connection, messaging, notification, safety and calling responsibilities.
- `information` combines announcements, support and live learning, which may have different governance owners.
- `admin` operates across several schema areas and can become an oversized privileged module.
- shared-schema JDBC access weakens enforceable module data ownership.
- optional assistant/calling features increase external-provider, privacy and demonstration risk.
- single-instance in-memory real-time behaviour may not scale horizontally without a shared broker.

These are not hidden. They are material for the final critical evaluation and future-work section.
