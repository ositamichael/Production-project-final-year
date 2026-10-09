# Sprint 7 — Campus events, service bookings and personal schedules

## Goal

Replace two prominent prototype-only areas with account-specific, database-backed workflows. A signed-in fictional student can now register for an event, reserve an available service slot, inspect their bookings, cancel safely and see confirmed activity in their personal dashboard schedule.

## Included in this slice

- University-scoped events with category, venue, start/end time, capacity and publication state.
- Event registration and cancellation linked to the server-verified student account.
- Remaining-place calculations based on current active registrations.
- University service offerings for lecturer appointments, study facilities and careers guidance.
- Bookable slots with capacity, availability and account-specific booking status.
- Transactional capacity checks using a locked event or slot row before a place is assigned.
- Duplicate and overlapping service-booking prevention.
- Account-specific booking history and cancellation.
- Event and booking confirmation notifications.
- Personal schedule composition from registered events and confirmed service bookings.
- Responsive Events, Book a Service, dashboard event and dashboard booking interfaces.

## Demonstration journey

1. Sign in with one of the fictional student accounts.
2. Open **Events**, filter the catalogue and register for an available activity.
3. Confirm that the action changes to **Cancel registration** and the activity appears in **Today's Schedule**.
4. Open **Book a Service**, choose an available lecturer, room or careers slot and confirm it.
5. Inspect **Booking activity**, the personal schedule and the new confirmation notification.
6. Cancel the booking and confirm that the place becomes available again.

All event, service, staff, venue and student records in this journey are fictional development data.

## Engineering and safety decisions

- The browser never supplies a user or university identifier; the server derives both from the authenticated session.
- Every event, service and booking query is restricted to the signed-in university tenant.
- Capacity is checked inside a database transaction after locking the relevant event or service slot.
- The unique database constraint prevents the same account receiving duplicate places in one slot.
- A second overlap check prevents one account holding simultaneous service bookings.
- State-changing requests retain CSRF protection.
- Cancellation changes state rather than deleting the audit record.
- The interface continues to state that no live university system is connected.

## Verification evidence

Automated integration tests cover authentication, tenant-scoped catalogue access, category filtering, registration, cancellation, duplicate rejection, availability calculations, CSRF protection, service booking and personal schedule composition. The complete Maven verification build and JavaScript syntax check must pass before the sprint is committed.

## Deliberately deferred

- Connections to official university timetable, room, staff-calendar and event systems.
- Payment-taking events or restaurant orders.
- Staff-side event and availability administration.
- Waitlists, recurring bookings and calendar-file export.
- Email, SMS and native push confirmations.
- Production retention, cancellation-window and no-show policies.
