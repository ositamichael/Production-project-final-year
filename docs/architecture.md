# UniCircle architecture

## System shape

UniCircle is a Spring Boot modular monolith backed by PostgreSQL. A responsive web client is served by the application during the early sprints. Later mobile clients can consume the same versioned API.

```text
Responsive web / future mobile clients
                |
          HTTPS + WebSocket
                |
      Spring Boot modular monolith
  identity | community | messaging | campus services
                |
        PostgreSQL + Flyway
                |
Approved university APIs and OIDC identity provider
```

## Planned modules

| Module | Responsibility | Important boundary |
| --- | --- | --- |
| Identity | OIDC sign-in, account lifecycle, roles | UniCircle never receives a university password |
| Tenancy | University membership and data isolation | Every protected record is scoped to a university |
| Connections | Requests, acceptance, blocking, visibility | Messaging and location require explicit relationship rules |
| Messaging | Direct/group conversations and delivery | Only members can access a conversation |
| Campus | Schedule, events, services and map | Data comes from approved integrations, not screen scraping |
| Bookings | Appointments, rooms and facilities | Availability and conflicts are validated transactionally |
| Opportunities | Jobs, placements and volunteering | Publisher identity and moderation are required |
| Support | Wellbeing routes and urgent help | Clear escalation; not a substitute for emergency services |
| Assistant | Retrieval of approved campus information | Responses need sources, boundaries and failure handling |

## Data isolation

The first migration adds `university_id` to user, connection, conversation and message records. Application services must derive the active university from the authenticated identity rather than trusting a university identifier supplied by the browser. Future integration tests will attempt cross-tenant access and verify denial.

## Deployment direction

The same build should run locally, in a university test environment and in a managed production environment. Secrets are environment variables. Production uses PostgreSQL, TLS, university OIDC and central logging with personal-data minimisation. App Store and Play Store clients are later deliverables and must pass platform review, moderation and account-deletion requirements.
