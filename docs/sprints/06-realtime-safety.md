# Sprint 6 — Realtime communication, notifications and safety

## Goal

Make the consent-based social workflow behave like a genuine multi-user service. Separate fictional students must be able to use different signed-in browser sessions, receive message and connection updates without refreshing, understand what is unread, and stop or report unsafe contact.

## Included in this slice

- Four distinct fictional student sign-ins for Michael, Aisha, James and Sophie.
- Session identity resolution that maps each local sign-in to the correct database account.
- An authenticated WebSocket endpoint that is bound to the server-verified account identity.
- Automatic refresh of conversations, unread counts and notifications after a relevant live event.
- Database-backed notifications for requests, accepted connections, messages and safety actions.
- A working notification panel with account-specific unread state and a mark-all-read action.
- Unread message totals in the navigation and individual conversation list.
- Immediate blocking of an account, including removal of private messaging access.
- Confidential reporting with constrained reasons, optional details and an auditable review status.
- A responsive conversation-safety panel with a deliberate two-step block action.

## Demonstration journey

1. Open one normal browser profile and one private browser window.
2. Sign in as Michael in one session and Aisha in the other using only the fictional password.
3. Open **Messages** in both sessions.
4. Send a message from either account. The other session receives a live event and refreshes the conversation and notification badges.
5. Open the bell panel to inspect database-backed account notifications.
6. In a conversation, choose **Safety** to demonstrate confidential reporting or the two-step block control.

All identities, messages and reports in this development journey are fictional.

## Security and privacy boundaries

- The WebSocket connection uses the existing authenticated Spring Security session.
- A browser cannot provide or switch the recipient account used for event delivery.
- Live events contain only an event type and resource identifier; protected content is fetched through authorised APIs.
- Conversation reads and writes continue to require membership and an accepted connection.
- Blocking changes the server-side connection state, so a hidden or manipulated browser cannot continue messaging.
- A blocked student is not told who blocked them.
- Reports are stored separately from ordinary social data and are intended for authorised safeguarding review.
- State-changing actions retain CSRF protection.
- User-provided content continues to be rendered as text rather than executable HTML.

## Verification evidence

Automated tests cover:

- notification authentication;
- recipient-specific message notifications;
- unread notification data;
- immediate loss of messaging access after a block;
- validated and persisted safeguarding reports;
- previous connection, membership and CSRF protections; and
- the presence of the realtime, notification and safety interface controls.

The complete Maven verification build and JavaScript syntax check must pass before the sprint is committed.

## Deliberately deferred

- University-approved identity provisioning and real student records.
- Native iOS/Android push notifications when the app is closed.
- Staff moderation dashboards, safeguarding service-level agreements and escalation rotas.
- Evidence retention/deletion policy, legal review and subject-access workflows.
- Group messaging, media attachments, voice/video calls and end-to-end encryption.
- Production load, penetration, accessibility and disaster-recovery testing.
