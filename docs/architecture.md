# UniCircle architecture

## System shape

UniCircle is a Spring Boot modular monolith backed by PostgreSQL. A responsive web client is served by the application during the early sprints. Later mobile clients can consume the same versioned API.

```text
Responsive web / future mobile clients
                |
          HTTPS + WebSocket
                |
      Spring Boot modular monolith
 identity | community | messaging | campus information | learning
                |
        PostgreSQL + Flyway
                |
Approved university APIs and OIDC identity provider
```

## Module responsibilities

| Module | Responsibility | Important boundary |
| --- | --- | --- |
| Identity | OIDC sign-in, account lifecycle, roles | UniCircle never receives a university password |
| Tenancy | University membership and data isolation | Every protected record is scoped to a university |
| Connections | Requests, acceptance, blocking, visibility | Messaging and location require explicit relationship rules |
| Messaging | Direct/group conversations and delivery | Only members can access a conversation |
| Campus | Schedule, events, services and map | Data comes from approved integrations, not screen scraping |
| Information | University, course, placement and internship announcements | Audience targeting and publisher identity are enforced by the server |
| Live Learning | Scheduled classes, attendance, participants, chat, Q&A and hand raising | Multiparty media is delegated to an approved meeting provider |
| Bookings | Appointments, rooms and facilities | Availability and conflicts are validated transactionally |
| Opportunities | Jobs, placements and volunteering | Publisher identity and moderation are required |
| Lost and Found | Moderated item reports, photographs and ownership claims | Public descriptions are separated from private ownership evidence |
| Support | Private problem reports, wellbeing routes and urgent help | Account isolation and clear escalation; not a substitute for emergency services |
| Assistant | Retrieval of approved campus information | Responses need sources, boundaries and failure handling |

The identity, tenancy, connections, messaging, campus-event, transactional-booking, moderated-opportunity, lost-and-found, fictional staff-review, targeted-announcement, support-case and live-learning foundations now run in the development sandbox. Approved staff publishing, real university email, multiparty media, approved-information retrieval and live university integrations remain staged institutional work.

## Data isolation

The first migration adds `university_id` to user, connection, conversation and message records. Application services must derive the active university from the authenticated identity rather than trusting a university identifier supplied by the browser. Future integration tests will attempt cross-tenant access and verify denial.

## Deployment direction

The same build should run locally, in a university test environment and in a managed production environment. Secrets are environment variables. Production uses PostgreSQL, TLS, university OIDC and central logging with personal-data minimisation. App Store and Play Store clients are later deliverables and must pass platform review, moderation and account-deletion requirements.
