# Sprint 9 — Moderated lost and found

## Goal

Replace the static lost-and-found mock-up with a secure university-scoped workflow that supports reporting, discovery and private ownership claims without publishing the details that should be used to verify an owner.

## Included in this slice

- Approved fictional lost and found reports separated by university tenant.
- Search across item name, colour, description and campus location.
- Lost/found type filters, category filters, My reports and My claims views.
- Student report creation with public and private information deliberately separated.
- Pending moderation for every new student report.
- Optional validated PNG/JPEG item photographs re-encoded without embedded metadata.
- Private ownership claims with account-specific status tracking.
- Notifications to the original reporter when a private claim is submitted.
- Report withdrawal by the student who created it.
- Responsive catalogue, report form and secure details dialog.

## Demonstration journey

1. Sign in as fictional student Michael Carter.
2. Open **Lost and Found** and search or filter the moderated catalogue.
3. Open a found-item report and submit private ownership evidence.
4. Select **My claims** to confirm the claim is account-specific.
5. Create a lost or found report, optionally attach a valid photograph and submit it.
6. Select **My reports** and confirm the new report is pending moderation and absent from the public catalogue.
7. Withdraw the report and confirm its state changes without deleting audit history.

All items, locations, photographs, reports and claims used in the demonstration are fictional development records.

## Engineering and safety decisions

- The server derives account and university identifiers from the authenticated session.
- Public catalogue queries return only approved reports from the signed-in university.
- New reports are pending by default and cannot self-publish.
- Private identifying details are stored separately and are not selected into catalogue responses.
- Ownership claims accept private evidence only and cannot be submitted against the claimant’s own report.
- Duplicate claims are rejected by both a database uniqueness rule and an application conflict response.
- State-changing requests require authentication and CSRF protection.
- Photographs are decoded, dimension checked, re-encoded and stored with generated keys to prevent metadata leakage and path traversal.
- The interface directs users to campus security or an approved service desk rather than arranging unsafe private handovers.

## Verification evidence

Integration tests cover authentication, search, filters, moderation visibility, private report history, ownership claims, duplicate claims, own-report rejection, notifications, withdrawal and CSRF protection. Storage tests cover safe image re-encoding, fake files, low-resolution files and unsafe storage keys. Frontend structure tests verify the working catalogue, forms, photograph control, privacy language and removal of the old prototype-only action.

## Deliberately deferred

- Staff moderation, ownership-verification and collection dashboards.
- Production object storage, malware scanning and automatic deletion policies.
- Live university security-desk or lost-property integrations.
- Email, SMS or native push notifications.
- Automated visual matching between lost and found items.
- Identity-document handling or publication of claimant evidence.
