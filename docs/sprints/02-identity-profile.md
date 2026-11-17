# Sprint 2 — Identity and profile foundation

## Goal

Create the first real account-backed feature without pretending that university single sign-on has already been approved or connected.

## Included in this slice

- University and user-account persistence mapped to the versioned relational schema.
- A second Flyway migration for biography, course, study year, profile privacy and photograph references.
- A local demonstration identity fixture that is never loaded under the production profile.
- Authenticated profile read and update endpoints.
- Authenticated PNG/JPEG photograph upload with a 5 MB limit.
- Clickable profile and university-account controls in the sidebar and top bar.
- Verified university and email details replace the signed-out placeholders after an authenticated profile is loaded.
- File-content decoding rather than trusting the filename or browser media type.
- Dimension checks from 128×128 to 4096×4096 pixels.
- Image re-encoding to remove embedded metadata before storage.
- Random server-generated storage keys and path-traversal protection.
- Automated tests for valid, disguised, undersized and unsafe-path inputs.

## Security and deployment boundaries

- Profile endpoints remain authenticated and CSRF-protected. The public prototype dashboard cannot alter a profile anonymously.
- Production identity must come from an approved university OIDC provider. UniCircle never receives a university password.
- The local demonstration university and student are fixtures, not real Leeds Beckett University records.
- Local disk storage is suitable for development only. A production deployment requires encrypted managed object storage, malware scanning, retention rules and backup policy.
- Profile photographs are personal data. A production deployment requires a privacy notice, deletion workflow, access controls and moderation/reporting process.

## Acceptance checks

- Both Flyway migrations apply to the local test database.
- JPA validates the schema without creating or changing it automatically.
- Non-image content is rejected even when named as a PNG or JPEG.
- Images below 128×128, above 4096×4096 or larger than 5 MB are rejected.
- Saved files cannot escape the configured storage directory.
- The sidebar profile, university-account card and top-bar avatar all open the same protected profile panel and are keyboard accessible.
- The full project verification build succeeds.
