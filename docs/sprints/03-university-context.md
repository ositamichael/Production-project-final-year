# Sprint 3 — University integration readiness

## Goal

Prove how UniCircle can receive a signed-in student's permitted academic context without claiming access to a live university system.

## Included in this slice

- A public identity boundary that derives account and university identifiers from the authenticated session rather than browser input.
- Versioned tables for programmes, modules, programme membership and student enrolments.
- A protected `GET /api/campus/me/context` endpoint scoped to the signed-in account's university.
- A university-data gateway that can later be replaced by an approved institutional connector.
- Clearly labelled fictional development fixtures for one programme, three modules and three coursemates.
- Connection states for accepted, pending and available-to-connect coursemates.
- A profile-panel academic section that displays the programme, current modules and permitted coursemates after authentication.
- A production-safe failure mode: production returns “connector not configured” instead of silently using demonstration records.

## Data provenance

All Sprint 3 people, email addresses, programme codes, module codes and enrolments are invented for software testing. The sandbox name is “Leeds Beckett–style Fictional Sandbox” to demonstrate a plausible university tenant while making clear that Leeds Beckett University did not supply or approve the records. Fictional addresses use the reserved-style `unicircle.test` domain and must never be represented as real students.

## Security and privacy boundaries

- Authentication and academic-data access are separate integrations.
- A verified email alone does not authorise directory or enrolment access.
- The endpoint derives tenant scope from the server-side identity boundary.
- Private profiles are excluded from coursemate discovery.
- The browser cannot request another university by changing an identifier.
- Real deployment requires an approved identity provider, an approved academic-data API, data-minimisation review and access auditing.

## Acceptance checks

- Three Flyway migrations apply successfully.
- Anonymous requests cannot access academic context.
- The signed-in development account receives exactly three fictional modules and three fictional coursemates.
- The response labels its source as `FICTIONAL_DEVELOPMENT_FIXTURE`.
- No real Leeds Beckett email address appears in the fixture response.
- The Spring Modulith architecture verification succeeds.
