# UniCircle architecture evidence

These diagrams summarise implemented behaviour. The database migrations and automated tests remain the authoritative technical evidence.

## System context

```mermaid
flowchart LR
    Student[Student browser] -->|HTTPS and WebSocket| App[Spring Boot modular monolith]
    Staff[Staff browser] -->|HTTPS and WebSocket| App
    Admin[Administrator browser] -->|HTTPS and WebSocket| App
    App --> Identity[Identity and tenancy]
    App --> Social[Connections, messaging and calls]
    App --> Campus[Campus information and engagement]
    App --> Safety[Support, reports and moderation]
    App --> Assistant[Ask UniCircle]
    Identity --> DB[(PostgreSQL)]
    Social --> DB
    Campus --> DB
    Safety --> DB
    Assistant --> DB
    App -. production OIDC .-> IdP[Approved university identity provider]
    Assistant -. explicit opt-in only .-> Public[Screened public knowledge service]
```

## Core relational model

```mermaid
erDiagram
    UNIVERSITIES ||--o{ USER_ACCOUNTS : contains
    USER_ACCOUNTS ||--o{ CONNECTIONS : requests
    USER_ACCOUNTS ||--o{ CONNECTIONS : receives
    USER_ACCOUNTS ||--o{ CONVERSATION_MEMBERS : joins
    CONVERSATIONS ||--o{ CONVERSATION_MEMBERS : has
    CONVERSATIONS ||--o{ MESSAGES : contains
    USER_ACCOUNTS ||--o{ MESSAGES : sends
    USER_ACCOUNTS ||--o{ LOCATION_SHARES : controls
    UNIVERSITIES ||--o{ CAMPUS_EVENTS : publishes
    CAMPUS_EVENTS ||--o{ EVENT_ATTENDEES : has
    USER_ACCOUNTS ||--o{ EVENT_ATTENDEES : registers
    UNIVERSITIES ||--o{ SERVICE_SLOTS : offers
    SERVICE_SLOTS ||--o| SERVICE_BOOKINGS : reserves
    USER_ACCOUNTS ||--o{ SERVICE_BOOKINGS : makes
    UNIVERSITIES ||--o{ CIRCLES : contains
    CIRCLES ||--o{ CIRCLE_MEMBERS : has
    USER_ACCOUNTS ||--o{ CIRCLE_MEMBERS : joins
    UNIVERSITIES ||--o{ OPPORTUNITIES : verifies
    USER_ACCOUNTS ||--o{ OPPORTUNITY_TRACKING : tracks
    OPPORTUNITIES ||--o{ OPPORTUNITY_TRACKING : appears_in
    USER_ACCOUNTS ||--o{ LOST_FOUND_REPORTS : reports
    USER_ACCOUNTS ||--o{ SUPPORT_REQUESTS : submits
```

## Accepted-connection messaging sequence

```mermaid
sequenceDiagram
    participant A as Student A
    participant API as Social API
    participant DB as PostgreSQL
    participant RT as WebSocket gateway
    participant B as Student B
    A->>API: POST message with CSRF token
    API->>API: Resolve authenticated identity
    API->>DB: Verify same university, accepted connection and no block
    DB-->>API: Authorised conversation
    API->>DB: Persist message in transaction
    DB-->>API: Commit
    API->>RT: Publish after commit
    RT-->>B: MESSAGE_RECEIVED event
    API-->>A: Persisted message response
```

## Consent-based location sequence

```mermaid
sequenceDiagram
    participant U as Student browser
    participant API as Location API
    participant DB as PostgreSQL
    participant Job as Scheduled purge
    U->>U: User explicitly enables sharing
    U->>API: Rounded position update with CSRF token
    API->>API: Validate coordinates and signed-in identity
    API->>DB: Upsert share with expires_at = now + 5 minutes
    API-->>U: Expiry timestamp
    Note over API,DB: Friends are returned only when the connection is accepted and both users are sharing
    Job->>DB: Every 60 seconds delete expired shares
    U->>API: Disable sharing
    API->>DB: Delete the user's share immediately
```

## Production security boundary

```mermaid
flowchart TD
    Browser --> Headers[CSP, referrer and permissions policies]
    Headers --> Security[Spring Security filter chain]
    Security --> Auth{Environment}
    Auth -->|Development| Demo[Seeded demonstration identities]
    Auth -->|Staging pilot| Pilot[Database accounts and encoded passwords]
    Auth -->|Institutional production| OIDC[OIDC identity provider]
    Security --> CSRF[CSRF protection for state changes]
    Security --> Access[Role, ownership and university-scope checks]
    Access --> Validation[Jakarta and domain validation]
    Validation --> Service[Transactional services]
    Service --> Database[(PostgreSQL)]
```
