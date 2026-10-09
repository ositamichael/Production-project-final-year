# Sprint 4 — Authenticated demonstration journey

## Goal

Allow assessors and test users to experience the protected student journey before any university approves a live identity or academic-data connection.

## Included in this slice

- A premium responsive sign-in page dedicated to the fictional development sandbox.
- A password-protected Spring Security session for one fictional student.
- Clear separation between the sandbox and the future university-managed Microsoft OIDC journey.
- A single `/sign-in` entry point that selects the fictional login during development and the approved OIDC provider in production.
- Session restoration so the dashboard displays the authenticated student's identity after returning or refreshing.
- Secure sign-out using a CSRF-protected POST request.
- Protected profile and academic-context requests return HTTP 401 when no student session exists.
- The authenticated profile displays the fictional programme, modules and permitted coursemates created in Sprint 3.

## Demonstration account

- Username: `student@demo.unicircle.test`
- Password: `CampusDemo!2026`
- Student: Michael Carter, a fictional Year 3 Computer Science student.

The credentials can be overridden through development environment variables. The account is created only outside the `prod` profile and is not a substitute for university authentication.

## Security boundaries

- The sandbox page tells users never to enter a Leeds Beckett password or Authenticator code.
- Production does not create the demonstration account or enable form login.
- Production `/sign-in` redirects to the configured university OIDC provider.
- Authentication establishes identity; it does not itself grant access to timetable, directory or student-record systems.
- Academic data remains fictional until an institution approves and supplies a connector.
- Spring Security rotates the session identifier during successful authentication and requires CSRF protection for logout and profile changes.

## Acceptance checks

- Anonymous profile and academic-context requests receive HTTP 401.
- Correct fictional credentials create an authenticated student session.
- The authenticated session can load the fictional profile, three modules and three coursemates.
- Incorrect credentials are rejected.
- Logout removes authentication and returns the user to the campus preview.
- The sign-in page visibly states that it does not contact Leeds Beckett, Microsoft or Authenticator.
