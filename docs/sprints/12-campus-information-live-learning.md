# Sprint 12 — Campus information and live learning

## Goal

Make UniCircle more informative and useful during everyday university life by bringing trusted announcements, placements, internships, private problem reporting and scheduled online learning into the authenticated campus experience.

## Delivered

- Added a university-scoped announcement model with category, audience, author, publisher role, urgency, pinned state, publication time and optional action.
- Added personal read records so one student's action does not mark an announcement as read for another student.
- Added a Campus Updates workspace and dashboard preview for university, module, placement, internship and urgent information.
- Added private support cases with reference numbers, team routing, status, personal history and replies.
- Kept support cases isolated to the signed-in account and made the current in-app delivery mode explicit.
- Added scheduled lectures, project meetings and careers drop-ins with university scope, host, module, agenda and lobby time.
- Added persisted attendance, participant presence and raise-hand state.
- Added class chat and Q&A streams that only accept messages from students who have joined the session.
- Added a responsive classroom interface with session list, teaching stage, agenda, participant list, chat, Q&A and participation controls.
- Added an optional provider link controlled by `UNICIRCLE_MEETING_PROVIDER_BASE_URL`; the interface clearly reports when no approved video provider is connected.
- Added Flyway migration `V12` and automated controller and structure tests.

## Verification

- The automated tests verify authentication, university targeting, personal read state, private support-case isolation, attendance, participant state, hand raising and joined-only class questions.
- Desktop browser checks confirmed the Campus Updates feed, filters, verified publisher information and announcement actions.
- Desktop browser checks confirmed class selection, join/leave controls, participant updates, class chat, Q&A and hand raising.
- Responsive checks at 390 × 844 confirmed that Campus Updates and Live Classes fit without horizontal overflow and remain reachable through the mobile interface.
- The interface continues to use real SVG interface icons; student profile areas remain reserved for user photographs.

## Important boundaries

The development records are fictional and do not come from a university system. Production announcements need approved staff roles and official course/audience data. Sending email needs an approved SMTP or Microsoft Graph connection. Real multiparty audio/video needs an approved meeting service or a securely operated media server; UniCircle currently owns class identity, access, attendance and participation but does not pretend that video exists when that service is absent.

## Next institutional work

1. Agree staff publisher roles, announcement approval and emergency-communications policy.
2. Agree support-team ownership, service levels, retention and escalation routes.
3. Select and security-review a meeting provider, then configure signed or controlled room access.
4. Map official programme, module and timetable identifiers through approved APIs.
5. Complete accessibility, privacy, safeguarding, penetration and load testing before real student use.
