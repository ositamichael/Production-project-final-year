# Sprint 11 — Invitation-only campus pilot

## Goal

Expand the working friends pilot into a broader, honest campus pilot that invited students can test on desktop and mobile before any university integration is attempted.

## Delivered

- Added course and year-of-study fields to invited account registration and persisted them in each pilot profile.
- Added a pilot university-context gateway that shows self-declared course details, generic test modules and privacy-filtered coursemates without claiming access to official university records.
- Seeded pilot-only events, campus services, bookable time slots and verified fictional opportunities.
- Enabled the existing event registration, service booking, opportunity saving and application-tracking workflows in the pilot profile.
- Removed the earlier pilot restriction that exposed only friends, messages and location.
- Added a fixed five-item phone navigation bar for My Campus, Circles, Messages, Events and More, including an unread-message badge and access to the complete menu.
- Updated the mobile header to show the UniCircle identity, account greeting, notifications and profile controls clearly.
- Preserved consent-based messaging, live calls, blocking, reporting and short-lived mutual location sharing.

## Verification

- The automated suite verifies invitation protection, separate accounts, course context, campus catalogues, messaging, calls, location safety, profile photographs and the mobile navigation structure.
- Responsive browser checks were completed at a 390 × 844 phone viewport.
- Each bottom-navigation destination was confirmed to display its correct workspace, and the More control was confirmed to open the full real-icon menu.

## Boundaries

All course, module, event, service and opportunity information in this pilot is test data. University single sign-on, official student records, timetables, room systems, safeguarding operations and verified institutional content require written approval, data agreements, security review and production integration work.
