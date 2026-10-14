# UniCircle release-readiness review

**Review date:** 14 October 2026  
**Candidate:** `feat/unicircle-friends-pilot`  
**Environment:** local Spring Boot staging run with the `pilot,pilot-local` profiles and an isolated H2 QA database  
**Browser used:** Chrome responsive viewport testing on macOS

## Recommendation

UniCircle is suitable for a **controlled, invitation-only staging pilot and final-year-project demonstration** after the automated suite passes in the target build environment. It is **not ready for an institutional production launch**. Production approval still depends on real university identity and data agreements, a managed PostgreSQL service, production monitoring and recovery, independent security and accessibility assessment, and real-device/multi-user acceptance testing.

This distinction is deliberate. The current build provides functioning application workflows with fictional or self-declared pilot records; it does not claim to be connected to Leeds Beckett University or any other institution.

## Work completed in this review

- Replaced misleading personal guest data with explicit signed-out states.
- Made the signed-in dashboard update consistently for circles, messages and assistant guidance.
- Replaced the misleading “Campus Map” name with **Friend Location** and added clear consent, expiry and non-emergency wording.
- Corrected stale hard-coded dates and added a London-local current date and greeting.
- Improved mobile layout, touch targets, page-specific mobile presentation, dialogs and navigation focus return.
- Corrected 320 px sign-in clipping and made the pilot forms fit the smallest supported width.
- Removed a duplicate HTML identifier and checked visible controls for accessible names.
- Kept the assistant honest about local guidance, optional public-source retrieval and local SVG illustration generation.
- Added durable regression checks for guest privacy, location wording and assistant source labels.

## Responsive viewport matrix

| CSS viewport width | Signed-out dashboard | Signed-in dashboard | Horizontal overflow | Navigation result |
|---:|---|---|---|---|
| 320 px | Pass | Pass | None found | Bottom navigation and More drawer usable |
| 375 px | Pass | Pass | None found | Bottom navigation and More drawer usable |
| 390 px | Pass | Pass | None found | Bottom navigation and More drawer usable |
| 430 px | Pass | Pass | None found | Bottom navigation and More drawer usable |
| 768 px | Pass | Pass | None found | Tablet layout usable |
| 1024 px | Pass | Pass | None found | Desktop navigation usable |
| 1440 px | Pass | Pass | None found | Full desktop layout usable |

Every main page was additionally checked at 390 px and 1440 px. This matrix is browser emulation evidence, not a substitute for testing on physical iOS and Android devices.

## Page-by-page QA

| Page | Guest state | Signed-in state | Mobile 390 px | Desktop 1440 px | Review outcome |
|---|---|---|---|---|---|
| My Campus | Honest guest summary | Pilot records and actions load | Pass | Pass | Pass |
| Today | No personal timetable exposed | Fictional pilot timetable clearly labelled | Pass | Pass | Pass |
| Campus Updates | Sign-in/unavailable state | Pilot announcements load | Pass | Pass | Pass |
| Live Classes | Sign-in/unavailable state | Fictional sessions load | Pass | Pass | Pass |
| Campus Circles | Sign-in state | Catalogue and membership actions load | Pass | Pass | Pass |
| Messages | Protected signed-out state | Account identity and conversations workspace load | Pass | Pass | Pass |
| Events | No invented personal dates | Current pilot catalogue loads | Pass | Pass | Pass |
| Book a Service | No personal slots shown | Pilot services and slots load | Pass | Pass | Pass |
| Opportunities | Sign-in state | Verified pilot catalogue loads | Pass | Pass | Pass |
| Friend Location | Consent explanation only | Sharing remains off until the user acts | Pass | Pass | Pass |
| Wellbeing & Support | General support information | Request workflows load | Pass | Pass | Pass |
| Lost and Found | Safe catalogue state | Moderated reporting workflow loads | Pass | Pass | Pass |
| Ask UniCircle | Basic guidance available | Saved conversations and workspaces load | Pass | Pass | Pass |

## Functional evidence

- A self-declared local QA account was created and authenticated through Spring Security.
- Signed-in identity, circles, events, services, opportunities and other account-aware panels loaded without guest wording.
- Ask UniCircle answered a basic computing question and saved the conversation to history.
- Image Studio generated a private, downloadable SVG illustration and stated that the prompt was not sent to an external image service.
- Friend Location was verified as off by default and described as a voluntary five-minute share between accepted connections who are also sharing.
- Mobile navigation opened, closed with Escape and returned keyboard focus to the More button.
- Privacy, acceptable-use and accessibility pages loaded in the local staging service.

## Accessibility checks

- No duplicate HTML identifiers remained after correction.
- No images without `alt` attributes were found in the rendered application.
- No visible unnamed links or buttons were found.
- Visible form controls had an associated label or accessible name.
- Focus-return behaviour was verified for the mobile navigation drawer.
- The profile form and sign-in forms fit and scroll at mobile widths.
- Reduced-motion support and visible focus styles are present in the CSS.

This is an in-project engineering check, not an independent WCAG conformance audit. Screen-reader combinations, 200–400% zoom/reflow, physical devices and disabled-user testing remain required before a university launch.

## Automated verification

- Final Maven clean build and test suite: **121 tests passed; 0 failures; 0 errors; 0 skipped**.
- JavaScript syntax: `node --check src/main/resources/static/js/app.js` passed after the final UI corrections.
- Database: all **15 Flyway migrations** applied successfully to the isolated QA database.

The packaged Spring Boot JAR was produced successfully after the final source, evidence and accessibility-statement changes.

## Security and privacy position

Implemented controls include Spring Security sessions, CSRF-protected writes, account-scoped data, accepted-connection messaging, consent-led expiring location sharing, upload constraints, moderation workflows, legal pages and staging/production separation. The UI does not ask for a university password or Authenticator code.

Institutional production still requires:

- an approved OIDC/SSO registration and formal university identity/data agreement;
- managed secrets, HTTPS-only cookies and environment-specific configuration;
- production-grade PostgreSQL, encrypted backup, restore rehearsal and retention rules;
- rate limiting, abuse monitoring, incident response and operational alerting;
- TURN infrastructure and cross-network call testing;
- penetration testing, privacy review/DPIA and independent accessibility assessment;
- verified ownership, moderation and support processes operated by authorised staff.

## Known blockers and unverified areas

| Area | Current position | Required before institutional production |
|---|---|---|
| University sign-in/data | Fictional or self-declared pilot identity only | Approved institutional OIDC and data contracts |
| Multi-user messaging/calls | Covered by application tests; browser UI checked with one QA account | Two or more physical devices, separate networks and TURN testing |
| Location | Consent controls and default-off state checked | Physical-device permission, accuracy, revocation and expiry tests |
| Email/meetings | Interfaces and safe fallback states exist | Approved providers, credentials, webhooks and failure testing |
| Database | Isolated H2 QA database | Managed PostgreSQL, migration rehearsal, backup and restore evidence |
| Accessibility | In-project structural and responsive checks | Independent WCAG 2.2 AA audit and user testing |
| Browser coverage | Chrome responsive viewport testing | Current Safari/iOS, Chrome/Android, Edge and Firefox |
| Operations | Health foundation exists | Monitoring, alerting, logs, runbooks, support ownership and SLA |

Three non-failing engineering warnings should be tracked: Flyway reports that H2 2.4.240 is newer than its latest verified H2 2.3.232; Mockito reports that self-attaching will change in a future JDK; and the optional OpenAI gateway uses a deprecated API. These warnings did not cause a test failure, but they should be resolved during dependency maintenance.

## Visual evidence

- [Signed-in mobile dashboard at 390 px](screenshots/mobile-dashboard-390.jpg)
- [Signed-in mobile Messages page at 390 px](screenshots/mobile-messages-390.jpg)
- [Desktop Ask UniCircle workspace at 1440 px](screenshots/desktop-assistant-1440.jpg)
- [Mobile local illustration result at 390 px](screenshots/mobile-assistant-image-390.jpg)

## Rollback and recovery

No production deployment was performed during this review. The code changes can be rolled back with a normal Git revert of the release-candidate commits. A hosted staging rollback should select the previous known-good commit in the hosting dashboard. Database rollback must use a tested forward-fix migration or restored backup; destructive manual schema reversal is not recommended.
