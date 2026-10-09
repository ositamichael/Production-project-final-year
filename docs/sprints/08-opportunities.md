# Sprint 8 — Verified opportunities and personal application tracking

## Goal

Replace the static opportunities mock-up with a university-scoped workflow that helps a signed-in student discover trusted fictional jobs, placements and volunteering, save useful listings and privately organise application progress.

## Included in this slice

- Verified fictional publishers with university, employer and students’ union publisher types.
- Moderated opportunities covering jobs, placements and volunteering.
- Search across title, summary, publisher and skills.
- Opportunity-type filtering and a private “My tracker” view.
- Detailed descriptions, skills, eligibility, location, work pattern, compensation wording and closing date.
- Account-specific save and unsave actions.
- A private tracker with Planning, Applied, Interview, Offer and Closed stages.
- Dynamic dashboard opportunity summaries and a responsive details dialog.

## Demonstration journey

1. Sign in as fictional student Michael Carter.
2. Open **Opportunities** and search or filter the approved catalogue.
3. Open a listing to inspect its verified publisher, eligibility, skills and deadline.
4. Save the listing and update its private application stage.
5. Select **My tracker** to see saved and actively tracked opportunities.
6. Return to the dashboard and confirm that verified opportunity summaries are loaded from the database.

All publishers, vacancies, deadlines and student tracker records are fictional development data.

## Engineering and safety decisions

- The server derives the user and university from the authenticated session.
- Catalogue queries are restricted to the signed-in university tenant.
- A listing is visible only when both its moderation state is approved and its publisher is verified.
- Suspended or pending publishers cannot appear in the student catalogue.
- Saving and tracking require CSRF-protected authenticated requests.
- Tracker records are account-specific and are never presented as employer application submissions.
- Closing dates are enforced by the server so expired listings cannot be newly saved or tracked.
- The UI states clearly that applying must use a separately verified university or employer route.

## Verification evidence

Integration tests cover authentication, university-scoped catalogues, searching, filtering, publisher suspension, saving, unsaving, tracking, stage updates, invalid stages and CSRF protection. Frontend structure tests verify the search, catalogue, details dialog and private tracker controls.

## Deliberately deferred

- Live employer, careers-service or jobs-board feeds.
- Staff publisher onboarding and moderation dashboards.
- Direct employer applications or transmission of CVs and personal documents.
- Deadline reminders by email, SMS or native push notification.
- Automated skills matching or recommendation ranking.
- Production fraud checks, retention rules and publisher appeal processes.
