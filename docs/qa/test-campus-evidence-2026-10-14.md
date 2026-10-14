# UniCircle Test Campus — feature evidence and readiness record

**Evidence date:** 14 October 2026  
**Candidate branch:** `feat/unicircle-friends-pilot`  
**Purpose:** supervised testing by real people using separate accounts and fictional Test Campus records

## Decision

This build is intended for a controlled staging pilot and final-year-project evaluation. It must not be presented as an institutional production system. Passing automated checks demonstrates that the application rules work in the tested environment; it does not replace physical-device testing, university approval, privacy and safeguarding governance, penetration testing, accessibility assessment, backup rehearsal or approved integrations.

## Feature-by-feature position

| Feature | Status | Evidence in this repository | Remaining manual or external evidence |
|---|---|---|---|
| Separate account creation and sign-in | Working | `PilotFriendsJourneyTests` verifies registration, validation and separate authenticated sessions | Repeat on the hosted PostgreSQL service with two real testers |
| Profile and photograph | Working | Pilot journey and media-storage tests verify saved profile photographs and another member's permitted access | Physical iPhone/Android photograph selection and deletion policy review |
| Student discovery and friend requests | Working | Multi-account pilot journey verifies discovery, request and recipient acceptance | Hosted two-device acceptance test |
| Private messaging | Working | Multi-account journey verifies a persisted message read by the other account; WebSocket tests cover authenticated event delivery | Hosted two-device reconnect and longer disconnection test |
| Voice and video calls | Working with deployment dependency | `CallSignallingTests` verifies accepted-connection signalling and denial rules | Microphone/camera test on physical iPhone and Android; TURN credentials and separate-network call test |
| Circles | Working | `CommunityControllerTests` covers university isolation, creation and membership | Real-tester usability and moderation review |
| Events and registration | Working | Engagement and administrator tests cover publishing, capacity, registration, cancellation, notifications and schedule inclusion | Hosted concurrent-user trial |
| Services, appointments and resource slots | Working for Test Campus records | Engagement tests cover locking, duplicate/overlap/full-capacity rejection, booking, cancellation and schedule inclusion | Does not reserve an external university facility; hosted concurrent-user trial still required |
| Personal schedule and calendar download | Working | Schedule composition tests cover registered events, confirmed bookings and administrator-assigned sessions; ICS output is tested | Calendar import check on target phone calendar apps |
| Announcements | Working | Information tests cover staff-only publishing, audience visibility, per-user read state and persistent recipient notifications | University publisher governance and any email/push connector |
| Live sessions and online class interaction | Working inside UniCircle | Information and administrator tests cover assignment, attendance, participant list, hand raising, chat and questions | Approved external meeting provider or full media-room implementation for institutional use |
| Opportunities | Working for verified Test Campus publishers | Opportunity tests cover moderation, search, saving and private progress; admin API restricts publisher selection | External vacancy feeds and publisher verification process |
| Support requests | Working as in-app Test Campus tracking | Information tests cover account isolation, reference, replies and honest delivery mode; administrator queue is university scoped | No request is delivered to a real university team without an approved connector and staffed process |
| Lost and Found | Working with fictional moderation | Lost & Found suites cover private evidence, moderation, claims, photographs, withdrawal and notifications | Approved physical handover and retention process |
| Notifications | Working while signed in | Persistent database records, unread counts, WebSocket updates for social activity and a 20-second visible-page refresh fallback for campus updates | Browser-closed push notification service is not implemented |
| Search | Working | `SearchControllerTests` cover authentication, minimum query length and permitted destinations; queries are same-campus and exclude blocked/private people | Relevance and load testing with a larger dataset |
| Ask UniCircle | Working within documented limits | Assistant controller tests cover local answers, persisted chats, source retrieval controls and private SVG illustration output | It is not an unlimited or infallible general AI; optional advanced provider features require an approved paid service |
| Friend-location sharing | Working server-side | Pilot journey verifies mutual opt-in, accepted connection, immediate block/stop and invisibility after expiry; server enforces five-minute freshness and same-campus access | Physical-device permission, accuracy and expiry observation on iPhone/Android |
| Administrator dashboard | Working in local automated and browser evidence | Admin tests cover role denial, account management, events, services, availability, opportunities, assigned sessions, schedule propagation, suspension and audit records. The administrator UI was also checked at desktop and 390 × 844 phone viewport sizes. | Hosted account must be promoted using `UNICIRCLE_BOOTSTRAP_ADMIN_EMAIL`; repeat the acceptance journey on Render |
| Persistence across refresh/sign-out | Working by database design and controller tests | All listed user records are database-backed; repository tests reload records in later requests | Hosted server-restart and PostgreSQL restore rehearsal still required |
| iPhone and Android verification | Blocked pending devices | Responsive structure and browser viewport checks exist | Physical iPhone and Android devices were not available to this automated run |
| Official university SSO/data | Not implemented | Production OIDC boundary and tenant model exist | University registration, contracts, mappings, DPIA and security approval are required |

## Required hosted acceptance journeys

1. Create two ordinary tester accounts and one nominated administrator account on separate devices.
2. Set `UNICIRCLE_BOOTSTRAP_ADMIN_EMAIL` to the nominated account and restart the staging service.
3. Create an event, service slot, assigned live session, opportunity and announcement in the administrator/staff interfaces.
4. Confirm the second account receives the update, sees published records and has only assigned or self-booked activity in its personal schedule.
5. Send, accept and decline friend requests; exchange messages; refresh and sign out/in; verify the history remains.
6. Test audio and video while both accounts have the app open. Record failure honestly if a TURN relay is not configured or either browser denies permission.
7. Enable location on both accepted accounts, observe the active-sharing indicator, stop one share immediately, then repeat and verify server expiry after five minutes.
8. Attempt a second booking for a one-capacity slot and record the rejected request.
9. Block an accepted account and verify messages, calls and location are no longer available.
10. Restart the Render service and verify profiles, connections, messages, created activities, bookings, schedules and acceptance evidence remain.

## Honest blockers

- No official university identity, student record, timetable, room, finance, email, careers or safeguarding system is connected.
- No browser-closed mobile push service is implemented.
- Dependable calls across restrictive networks require a TURN relay; Render alone is not that relay.
- The staging administrator is an application role, not proof of institutional authority.
- Physical iPhone, Android and multi-network tests require the user and invited testers; they cannot be truthfully claimed from a desktop-only automated run.

## Local browser acceptance journey

Using the fictional administrator and student accounts on 14 October 2026:

1. The administrator opened the role-gated Test Campus dashboard on desktop and at a 390 × 844 phone viewport.
2. The administrator published **Acceptance Test Workshop** for 23 October 2026.
3. Michael Carter signed in through a separate session state and could see the activity in the Events catalogue.
4. Michael received a persistent **New Test Campus event** notification.
5. Michael registered for the event and the action changed to **Cancel registration**.
6. The event then appeared in Michael's **Upcoming campus commitments** schedule.
7. The browser console contained no error or warning entries during this journey.

This is local acceptance evidence. It does not replace the hosted, physical-device and multi-network checks listed above.

## Automated verification result

- Maven clean verification: **130 tests passed; 0 failures; 0 errors; 0 skipped**.
- Database migration verification: **16 Flyway migrations applied**, including the Test Campus administration schema.
- JavaScript syntax: `src/main/resources/static/js/app.js` passed the Node syntax check.
- Patch quality: the Git whitespace/error check passed.

These results cover the repository build and server-side journeys. They do not convert the manual and external items above into passes.
