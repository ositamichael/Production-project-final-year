# Sprint 13 — Staging acceptance evidence

## Goal

Keep UniCircle as one complete campus application while providing a safe, measurable way to test it with invited people before any university supplies identity or operational data.

## Delivered

- Renamed the active pilot presentation to **UniCircle Staging Campus** so it is not mistaken for a separate friends product.
- Kept the complete desktop and mobile UniCircle navigation available to invited staging accounts.
- Added a Pilot Test Centre inside the main application rather than creating another portal.
- Added 15 end-to-end acceptance scenarios covering accounts, student discovery, connection consent, messaging, calls, location, safety, announcements, bookings, opportunities, support, live learning, lost property and mobile access.
- Added account-specific pass, fail, blocked and not-tested records.
- Added optional device/browser and evidence-note fields with explicit warnings against recording sensitive information.
- Added summary metrics so a tester and supervisor can see progress and unresolved failures.
- Added Flyway migration `V13` and automated endpoint and frontend structure tests.

## Evaluation method

Two invited people use separate accounts and devices against the same hosted staging database. Test university information is realistic but fictional. The people, consent decisions, messages, calls, device permissions and recorded observations are real. A result is marked passed only when its stated expected outcome is observed.

Failed and blocked results remain visible instead of being hidden. The defect is corrected on a feature branch, the automated checks are rerun, and the affected scenario is repeated. This produces a clear audit trail for iterative development and user acceptance testing.

## Important boundaries

- The staging campus is not a university identity provider and must never collect a university password or Authenticator code.
- Self-declared courses and fictional campus records are not evidence of official enrolment or university approval.
- Test notes must not contain health disclosures, genuine support cases, passwords, private student records or other sensitive information.
- A successful friends test demonstrates application behaviour; it does not replace accessibility, privacy, safeguarding, penetration, load, backup and institutional integration reviews.

## Next verification step

Deploy this complete build to the existing staging service, register two fresh invited accounts, and execute the Test Centre scenarios on a laptop and phone. Configure a TURN relay before treating cross-network voice and video calls as reliable.
