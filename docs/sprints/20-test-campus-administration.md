# Sprint 20 — Complete Test Campus administration

## Aim

Make UniCircle usable as one controlled Test Campus where separate real testers can exercise connected journeys before any university supplies official identity or operational data. The result must provide evidence of working behaviour, not simply display pages.

## Implemented

- Added a university-scoped administrator dashboard for tester accounts, roles, suspension, events, bookable services, availability slots, assigned live sessions, opportunities, support requests, safety reports and an audit trail.
- Added a configurable pilot administrator bootstrap using `UNICIRCLE_BOOTSTRAP_ADMIN_EMAIL`. The value must be the email address of an existing pilot account; no administrator password or account is committed to Git.
- Added immediate account-status enforcement on every protected request so a suspended account cannot continue using an existing browser session.
- Added organiser ownership to newly managed Test Campus events, services, meetings and opportunities.
- Added assigned live sessions to each recipient's personal schedule and created a persistent notification for every assignment.
- Added persistent audience notifications for published announcements and support-request creation.
- Added a university-scoped search service covering permitted people, events, services, circles and opportunities while excluding private or blocked accounts.
- Added a 20-second visible-page refresh fallback so announcements, schedules, events, services, opportunities and notifications reach an open browser even if no dedicated WebSocket event is emitted.
- Kept every administrator-created public summary visibly labelled `[TEST CAMPUS]` and stated in the interface that records do not reserve external university facilities or contact official teams.

## Permission model

- Every administrator API derives the active university from the authenticated account.
- An active `ADMIN` role is required server-side; hiding a navigation item is not treated as access control.
- An administrator cannot suspend or remove their own administrator access.
- The final active administrator in a Test Campus cannot be demoted.
- Assigned people, verified publishers, support cases and safety reports must belong to the same university.
- Suspended accounts are rejected on all protected feature requests.

## Verification

Automated integration tests cover administrator/student separation, university-scoped user lists, event creation, registration, schedule propagation, assignment of a live session, notification persistence, immediate suspension and permission-aware search. Existing suites continue to cover separate pilot registration, friend requests, consent-based messaging, WebRTC signalling permissions, short-lived mutual location, transactional booking capacity, circles, opportunities, support, Lost and Found, announcements, live-class participation, Ask UniCircle and mobile structure.

The final readiness matrix is in [`../qa/test-campus-evidence-2026-10-14.md`](../qa/test-campus-evidence-2026-10-14.md). Physical iPhone/Android tests, cross-network calls with a configured TURN relay, institutional sign-in and external university systems remain explicitly outside the automated local evidence.

