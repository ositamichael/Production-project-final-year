# Sprint 5 — Consent-based connections and messaging

## Goal

Turn the static social preview into a real, testable workflow in which a signed-in student can discover permitted students from the same university, request a connection, accept or decline an incoming request, and privately message an accepted connection.

## Included in this slice

- A protected student-directory API that searches permitted same-university profiles by name or course.
- Support for new and existing student profiles: any newly provisioned account becomes eligible for the directory according to its university and privacy setting.
- Persisted connection requests with pending, accepted and declined states.
- Recipient-only accept and decline decisions.
- A canonical connection-pair key that prevents duplicate requests in either direction, including concurrent attempts.
- Direct conversations that can be created only for accepted connections.
- Conversation membership checks on every message read and write.
- Server-side message validation, a 4,000-character limit and CSRF protection.
- A working Messages workspace with conversation search, stored message history and message sending.
- Connection controls in both the profile's coursemate list and the student directory.
- Fictional development messages that demonstrate the complete journey without exposing real student data.

## Demonstration journey

1. Sign in with the fictional development account.
2. Open **Messages** and choose **Find students**.
3. Aisha Khan is already an accepted connection and can be messaged immediately.
4. Sophie Patel has sent an incoming request, which can be accepted or declined.
5. James Wilson is available for a new outgoing connection request.
6. Sent messages are written to the local development database and remain visible for the life of that application run.

All names, accounts, academic records and conversations in this journey are fictional fixtures.

## Security and privacy boundaries

- The browser never chooses a university boundary; the server derives both the account and university from the authenticated session.
- A directory search cannot return private profiles or accounts from another university.
- A student cannot connect to or message their own account.
- A request cannot be duplicated by reversing requester and recipient.
- Only the recipient of a pending request can accept or decline it.
- A direct conversation requires an accepted connection.
- Only conversation members can read or send messages.
- Every state-changing browser request requires a valid CSRF token.
- User-written message text is rendered with `textContent`, not injected as HTML.

## Verification evidence

Automated tests cover:

- anonymous access denial;
- student-directory filtering and connection state;
- duplicate request prevention;
- incoming-request acceptance;
- messaging denial before consent;
- persisted message sending;
- blank-message validation;
- CSRF enforcement; and
- conversation membership isolation.

The full Maven test suite and JavaScript syntax check must pass before this sprint is committed.

## Deliberately deferred

- Real university account provisioning and approved institutional directories.
- WebSocket live delivery and mobile push notifications.
- Blocking, reporting, moderation, retention and safeguarding operations.
- Group conversations, media attachments and end-to-end encryption.
- Durable production object storage and PostgreSQL deployment evidence.

These are separate high-risk capabilities and will be introduced through later tested slices rather than implied by the current demonstration.
