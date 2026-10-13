# Sprint 15 — Deployment hardening

## Goal

Turn remaining presentation-only controls into testable workflows and make the hosted pilot safer, more transparent and more usable on phones before inviting a larger test group.

## Completed engineering work

- Replaced the static Campus Circles cards with university-scoped database records.
- Added authenticated circle creation, membership joining and leaving, ownership rules, duplicate protection, search and category filters.
- Added a Flyway migration so the feature is created consistently in local H2 and hosted PostgreSQL databases.
- Added a calendar download that exports registered events and confirmed bookings as a standard `.ics` file.
- Connected wellbeing, advice and disability-support cards to the real private support-request form.
- Added a web-app manifest, service worker and an offline page so the HTTPS pilot can be installed from a supported phone browser.
- Kept private APIs, sign-in, messages, profiles and live account data out of offline caches.
- Added staging privacy, acceptable-use and accessibility information without claiming university approval or independent certification.
- Added Content Security Policy, referrer and device-permission headers to reduce browser attack surface.
- Added automated controller, migration, CSRF, tenant-isolation, public-resource and security-header tests.

## Acceptance checks

1. Sign in as two separate pilot users on a laptop and phone.
2. Create a circle with one account and confirm it appears only inside the same staging university.
3. Join and leave it with the second account; confirm the member count changes and the owner cannot leave accidentally.
4. Register for an event or book a service, download the calendar, and open the file in a calendar application.
5. Open each support route and confirm it prepares the correct private-request category.
6. Install UniCircle from a supported mobile browser and verify that the offline page appears when the connection is removed.
7. Confirm that refreshing after reconnecting returns to current server data rather than a cached private account page.
8. Check the privacy, acceptable-use and accessibility pages without signing in.
9. Confirm the complete automated test suite and continuous-integration workflow pass.

## External gates before a university deployment

This sprint makes the independent staging pilot more complete; it does not make the project an approved university production system. These items require third-party or university action:

- register production OIDC single sign-on and approve access to student, course, module and timetable data;
- complete data-protection, safeguarding, accessibility, penetration-test and incident-response reviews;
- configure managed backups, monitoring, alerting, retention/deletion processes and approved media storage;
- configure a TURN relay before claiming reliable calls across mobile, home and campus networks;
- approve a multiparty meeting provider for live classes;
- approve an email or Microsoft Graph connection before reporting that support emails were sent;
- add a private `OPENAI_API_KEY`, usage budgets and governance before enabling live AI;
- publish institution-owned privacy, acceptable-use and accessibility statements with real support contacts;
- complete App Store and Play Store packaging, signing and review if native store distribution is required.

## Evidence

The implementation is traceable through migration `V14__campus_circles.sql`, the `community` module, frontend structure tests, community controller tests, security-response tests and this sprint record. The hosted Render pilot remains a supervised acceptance environment using self-created accounts and fictional campus records.
