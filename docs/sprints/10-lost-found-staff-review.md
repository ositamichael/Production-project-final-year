# Sprint 10 — Lost and Found staff review

## Goal

Allow a fictional university service-desk reviewer to make an accountable decision on a student report before it reaches the public catalogue. Keep private identifying details and reviewer comments away from other students.

## Included in this slice

- A separate fictional staff sign-in, stored as a staff account in the same university tenant.
- A staff-only pending-report queue and private item photograph view.
- A two-step review form requiring an approve/reject choice and a 20–500-character reason.
- Server-side role and university checks for every queue, photograph and decision request.
- A transactional status update, append-only audit record in the application and account-specific notification for each decision.
- A private rejection note shown to the original reporter in **My reports**.
- Protection against changing a photograph after approval or rejection.

## Demonstration journey

1. Sign in as fictional student Michael and submit a report. It remains private and pending.
2. In another browser session, sign in as fictional service-desk reviewer Alex Morgan.
3. Open **Review queue**, inspect the public wording and separately labelled private detail, then record a reason and approve or reject.
4. Return to Michael's account. An approved report appears in the catalogue; a rejected report stays private and displays its review note in **My reports**.
5. Check that the audit record and notification exist and that a second decision is rejected.

## Safety and engineering decisions

- The reviewer's role and university are checked against the stored account, not a role value supplied by the browser.
- Reviewers cannot decide their own reports. A report from another university is not exposed or actionable.
- Pending reports and photographs are served only to staff; the review queue response forbids caching.
- The catalogue continues to select public fields only. Review notes appear only in the reporter's own response.
- Approval, audit and notification are one database transaction. A conditional update prevents double decisions.
- Once moderated, item photographs cannot be replaced without creating a new report.
- This is a fictional workflow, not a university-authorised lost-property service or proof that a claimant owns an item.

## Verification

Integration tests cover staff access, student denial, university separation, self-review denial, approval, rejection, private notes, audit, notifications, CSRF, invalid reasons, duplicate decisions and photograph permissions. Frontend tests cover the separate review page and two-step decision UI. The full Maven build and JavaScript syntax check are run before committing.

## Deliberately deferred

- Claim verification, physical handover and returned-item closure by authorised staff.
- Staff onboarding from a real university identity provider and institutional role governance.
- Production file storage, malware scanning, retention/deletion policies and operational logging.
- Live push alerts to already-open browser sessions when a review decision is recorded.
