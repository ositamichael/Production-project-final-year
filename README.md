# UniCircle

**Where campus meets connection.** UniCircle is a multi-university campus platform designed to bring people, schedules, communities, messaging, events, appointments, opportunities and trusted support into one coherent experience.

This repository is being rebuilt as a Level 6 final-year software engineering project. It previously contained the ScamShield prototype. That work remains recoverable in Git history and the `archive/scamshield` reference; the active product direction is now UniCircle.

## Current status

Sprint 1 established the testable engineering foundation, Sprint 2 added the account-backed identity and profile layer, Sprint 3 established the university-integration boundary, Sprint 4 added a complete authenticated demonstration journey, Sprint 5 added the first real social workflow, Sprint 6 added live multi-session communication and safety controls, Sprint 7 added transactional campus events and service bookings, Sprint 8 added moderated opportunities and personal application tracking, Sprint 9 added secure lost-and-found reporting, Sprint 10 added a fictional staff review workflow, Sprint 11 expanded the invitation-only pilot into a testable campus experience, Sprint 12 added trusted campus information, private support and live-learning participation, Sprint 13 added structured multi-user acceptance evidence, Sprint 14 added a responsible AI study and campus assistant, Sprint 15 hardened the application for a supervised deployment rehearsal, Sprint 16 added a useful no-cost basic assistant, Sprint 17 broadened it with opt-in sourced public knowledge, Sprint 18 added a private illustration creator and a phone-style navigation sheet, Sprint 19 added focused mobile workspaces and persistent Ask UniCircle conversations, and Sprint 20 adds permission-aware Test Campus administration and linked multi-user schedule evidence:

- Spring Boot 4.1.1 with Java 21 as the target language level.
- A responsive desktop, tablet and mobile dashboard prototype.
- Spring Security with production OIDC configuration for university single sign-on.
- A multi-university relational schema using explicit `university_id` boundaries.
- A searchable same-university student directory for new and existing permitted profiles.
- Persisted connection requests with recipient-only accept and decline decisions.
- Human-to-human private messaging that activates only after connection consent.
- Persisted direct conversations and messages with server-enforced membership checks.
- Authenticated WebSocket events that refresh messages and notifications across signed-in browser sessions.
- Account-specific, database-backed notifications and unread counts.
- A university-scoped Test Campus administrator dashboard for tester roles, suspension, activities, availability, support, safety and audit records.
- Administrator-created events, services, opportunities and assigned live sessions with persistent notifications and personal-schedule propagation.
- Immediate server-side suspension enforcement, including sessions that were already signed in.
- Permission-aware campus search across people, events, services, circles and opportunities.
- Immediate account blocking that removes private messaging permission.
- Confidential safeguarding reports with validated reasons and an auditable review state.
- A university-scoped event catalogue with account-specific registration, cancellation, remaining-capacity calculation and schedule integration.
- Service offerings and bookable time slots for appointments, facilities and campus support.
- Transactional booking controls that reject duplicate, overlapping and fully occupied requests.
- Account-specific booking history, cancellation and confirmation notifications.
- A composed personal schedule that combines registered events with confirmed service bookings.
- University-scoped jobs, placements and volunteering from verified fictional publishers.
- Server-enforced moderation and publisher-verification gates that prevent unapproved listings appearing.
- Account-specific saved opportunities and a private application-stage tracker.
- Search, type filtering, detailed eligibility, skills, working pattern and deadline information.
- University-scoped lost-and-found reports with type, category and text search.
- Moderation-gated student reports that remain private until approved.
- Private identifying details and ownership evidence that never appear in public catalogue responses.
- Account-specific report history, private claim tracking, withdrawal and reporter notifications.
- Validated PNG/JPEG item photographs that are re-encoded to strip embedded metadata.
- A fictional service-desk reviewer with a staff-only queue, audited approve/reject decisions and private reporter feedback.
- Four separate fictional student sign-ins for demonstrating requests and chat between people.
- Flyway migrations, PostgreSQL production configuration and an H2 local profile.
- Health monitoring, modular architecture checks, unit tests and CI.
- Honest prototype labels wherever live university data is not yet connected.
- Database-backed profile details and a protected profile API.
- Validated PNG/JPEG profile-photograph processing that strips embedded metadata before local development storage.
- A protected academic-context API for programmes, modules and privacy-filtered coursemates.
- Fictional university fixtures that are visibly separated from future approved institutional data.
- A development-only fictional student sign-in with a real Spring Security session and secure logout.
- A dedicated sign-in experience that explicitly prevents the use of Leeds Beckett or Microsoft credentials.
- API requests return a clear unauthorised response rather than leaking protected profile information or returning a login page.
- Separate invitation-only pilot accounts with strong password hashing and database-backed profiles.
- Persistent pilot profile photos, messages and short-lived consent-based locations.
- Live WebRTC voice and video calls inside accepted private conversations, with authenticated signalling, consent prompts, mute/camera controls and no application recording.
- Location sharing that is off by default, mutual between accepted connections, rounded and automatically expired.
- A Docker deployment path and a focused campus-pilot test guide for a single-instance Render rehearsal.
- A five-destination mobile navigation bar matching the mobile product design: My Campus, Circles, Messages, Events and More.
- A scrollable phone-style More sheet with a close control, backdrop, safe-area spacing and access to every workspace without a desktop sidebar covering the screen.
- Self-declared course and year onboarding, privacy-filtered pilot coursemates and clearly labelled non-institutional academic context.
- Pilot event, service and opportunity catalogues with account-specific registration, booking, saving and tracking workflows.
- University-scoped announcements for urgent notices, modules, placements and internships, with verified-publisher context and per-student read state.
- A targeted Campus Updates workspace with category filters and direct routes into support, events, opportunities and live classes.
- Private support cases routed to the appropriate fictional university team, with reference codes, status tracking and account isolation.
- Scheduled lectures, project meetings and careers drop-ins with persisted attendance and participant lists.
- A live-class workspace with class chat, Q&A, raise/lower hand controls, agendas and provider-aware video-room status.
- A configurable boundary for an approved meeting provider through `UNICIRCLE_MEETING_PROVIDER_BASE_URL`.
- An account-specific Pilot Test Centre covering profiles, discovery, connections, messaging, calls, consent-based location, safety, announcements, bookings, opportunities, support, live classes, lost property and mobile access.
- Persisted pass, fail, blocked and not-tested results with device and non-sensitive evidence notes for supervised staging evaluation.
- Five assistant workspaces for broad questions, current university research, guided study, code support and original image generation.
- Server-side OpenAI Responses API integration with web search, visible source links, code execution and image generation tools.
- Input moderation, per-account rate limiting, secure server-side credentials and a clear setup state when AI is not enabled.
- A deterministic no-cost assistant that answers supported computing, programming, study, project-planning, campus and simple calculation questions without an external AI account.
- A private no-cost SVG illustration creator for downloadable posters, banners and concept graphics. It runs inside the UniCircle server and is deliberately not described as photorealistic generative AI.
- Account- or browser-session-scoped Ask UniCircle conversation history, with a New chat workflow and persisted questions, answers, sources and generated illustrations.
- Focused phone layouts that remove dashboard-only greeting and search content from feature workspaces and give an open private conversation the available app screen.
- An optional public-knowledge fallback for wider factual questions, with a visible Wikipedia source and no payment or provider key.
- Explicit per-question public-lookup consent, shortened search topics and sensitive-input blocking before any external request.
- Honest boundaries for unsupported questions: no-cost mode does not claim unrestricted reasoning, official live university data, arbitrary code execution or unlimited photorealistic image generation.
- Academic-integrity instructions that support explanation, practice, feedback and debugging without impersonating a student or fabricating research.
- Database-backed campus circles with university isolation, search, filtering, creation and real join/leave membership.
- Downloadable standards-based calendar files for registered events and confirmed bookings.
- An installable mobile web-app shell with an honest offline screen that never caches private API data.
- Public privacy, acceptable-use and accessibility information for the staging pilot.
- Content Security Policy, Permissions Policy and strict referrer controls on browser responses.

The dashboard now contains working local social, event-registration, booking, opportunity-tracking, lost-and-found, fictional staff-review, announcement, support-case and live-learning demonstrations, but it is not evidence of a live university integration. Real student identity, official timetables, staff publishing, university email, booking systems, employer vacancy feeds, staffed support services, multiparty video and location require institutional approval, integration contracts, security review and deployment infrastructure. The pilot records information in its own database; it does not falsely report that an email was sent or a video room was opened when those approved services are absent. Production push notifications, institutionally governed moderation and safeguarding operations, secure media storage and retention policies remain future work.

## Run in IntelliJ IDEA

1. Open this repository's `pom.xml` as a project.
2. Set the project SDK to Java 21 or newer. IntelliJ's bundled Java 25 runtime can compile the Java 21 target.
3. Allow IntelliJ to import the Maven project.
4. Run `UniCircleApplication`.
5. Open `http://localhost:8080`.
6. Open the profile and choose **Sign in to fictional sandbox**. The local-only test account is:
   - Email: `student@demo.unicircle.test`
   - Password: `CampusDemo!2026`

The sign-in page also offers fictional Aisha, James and Sophie student accounts and fictional service-desk reviewer Alex Morgan using the same development-only password. Use separate browser profiles or a private window to demonstrate connection decisions, live messaging and staff review.

These credentials are deliberately fictional and exist only in the non-production profile. They can be overridden with `UNICIRCLE_DEMO_USERNAME` and `UNICIRCLE_DEMO_PASSWORD`. Never use a real university password or Authenticator code in this form.

No local PostgreSQL installation is required for the default profile. It uses an in-memory H2 database configured to behave like PostgreSQL for early development. Production uses PostgreSQL and environment variables documented in `.env.example`.

## Ask UniCircle assistant setup

Ask UniCircle works without an account, payment or external AI key. Its reviewed local engine provides deterministic guidance for common computing concepts, beginner Java, C#, Python, SQL, HTML/CSS/JavaScript, Spring Boot, REST APIs, debugging, testing, Agile, MoSCoW, risk registers, final-year-project planning, UniCircle use and simple arithmetic. Because these answers run inside the application, they remain fast and predictable on the small Render service.

For a wider stable factual question, the user may explicitly enable **Use public sources**. UniCircle screens the text for obvious secrets and personal identifiers, shortens it to a search topic, retrieves a current English Wikipedia introduction and displays the page as a source. The control is off by default, the full conversation is not sent and a source-free failure never becomes an invented answer. This is retrieval, not a claim that Wikipedia is always correct or that UniCircle contains a perfect general-purpose language model.

No-cost mode is deliberately limited. It does not analyse arbitrary large programs, verify changing university information, execute code or produce unlimited photorealistic images. Admissions, fees, visas, policies, deadlines and other high-stakes or current facts must still be checked on an official source.

The existing advanced integration remains optional. If a future approved deployment receives a valid `OPENAI_API_KEY`, web research, broader answers, code execution and image generation become available automatically. Add the key only as a private server-side environment variable; never place it in JavaScript, commit it to Git or ask a student to enter it. Guest requests receive the same validation and session-based usage limit as signed-in requests; private profiles, messaging and university data remain account-protected.

The default main model is `gpt-6.1-sol`, selected as a strong balance of reasoning quality and cost, and the default image tool is `gpt-image-2.5-flare`. Both can be changed without code edits using `UNICIRCLE_ASSISTANT_MODEL` and `UNICIRCLE_ASSISTANT_IMAGE_MODEL`. Provider usage is chargeable and should be protected with account limits, budget alerts and institutional approval before a wider pilot.

When advanced mode is enabled, Ask UniCircle can search the public web for general and university-research questions and display returned source links. It never has automatic access to a university's private systems and cannot guarantee that every answer is correct.

For the invitation-only staging-account flow within the complete UniCircle application and Render rehearsal instructions, see [`docs/friends-pilot.md`](docs/friends-pilot.md). Pilot participants must never enter university credentials or Authenticator codes.

The staging sign-in page includes a password-recovery form. Recovery requires a high-entropy, single-use code issued by the Test Campus operator, a matching pilot email address and a new password of at least 12 characters. Codes are stored only as SHA-256 hashes, expire, are consumed after one successful change and remain protected by CSRF validation. This operator-assisted staging recovery is not a substitute for institution-approved email delivery in a production deployment.

## Command-line checks

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

## Product boundaries

- People messaging is human-to-human. The campus information assistant is a separate, clearly labelled feature.
- Location sharing will be off by default and limited to people explicitly accepted by the user.
- UniCircle will never ask users to type their university password into the application. Production sign-in redirects to the university identity provider through OIDC.
- The project does not claim that Leeds Beckett University or any other institution has approved, deployed or endorsed it.
- Fixtures shown in the dashboard are demonstration data, not live records.

## Architecture

UniCircle begins as a modular monolith. This keeps deployment manageable for a final-year project while enforcing boundaries that can later support independent services. See:

- [`docs/architecture.md`](docs/architecture.md)
- [`docs/adr/0001-platform-and-language.md`](docs/adr/0001-platform-and-language.md)
- [`docs/adr/0002-modular-monolith.md`](docs/adr/0002-modular-monolith.md)
- [`docs/adr/0003-identity-and-tenancy.md`](docs/adr/0003-identity-and-tenancy.md)
- [`docs/sprints/01-foundation.md`](docs/sprints/01-foundation.md)
- [`docs/sprints/02-identity-profile.md`](docs/sprints/02-identity-profile.md)
- [`docs/sprints/03-university-context.md`](docs/sprints/03-university-context.md)
- [`docs/sprints/04-demo-sign-in.md`](docs/sprints/04-demo-sign-in.md)
- [`docs/sprints/05-connections-messaging.md`](docs/sprints/05-connections-messaging.md)
- [`docs/sprints/06-realtime-safety.md`](docs/sprints/06-realtime-safety.md)
- [`docs/sprints/07-events-bookings.md`](docs/sprints/07-events-bookings.md)
- [`docs/sprints/08-opportunities.md`](docs/sprints/08-opportunities.md)
- [`docs/sprints/09-lost-found.md`](docs/sprints/09-lost-found.md)
- [`docs/sprints/10-lost-found-staff-review.md`](docs/sprints/10-lost-found-staff-review.md)
- [`docs/sprints/11-campus-pilot.md`](docs/sprints/11-campus-pilot.md)
- [`docs/sprints/12-campus-information-live-learning.md`](docs/sprints/12-campus-information-live-learning.md)
- [`docs/sprints/13-staging-acceptance-evidence.md`](docs/sprints/13-staging-acceptance-evidence.md)
- [`docs/sprints/14-responsible-ai-assistant.md`](docs/sprints/14-responsible-ai-assistant.md)
- [`docs/sprints/15-deployment-hardening.md`](docs/sprints/15-deployment-hardening.md)
- [`docs/sprints/16-no-cost-basic-assistant.md`](docs/sprints/16-no-cost-basic-assistant.md)
- [`docs/sprints/17-sourced-public-knowledge.md`](docs/sprints/17-sourced-public-knowledge.md)
- [`docs/sprints/18-mobile-shell-and-private-illustrations.md`](docs/sprints/18-mobile-shell-and-private-illustrations.md)
- [`docs/sprints/19-focused-mobile-workspaces.md`](docs/sprints/19-focused-mobile-workspaces.md)
- [`docs/sprints/20-test-campus-administration.md`](docs/sprints/20-test-campus-administration.md)
- [`docs/qa/test-campus-evidence-2026-10-14.md`](docs/qa/test-campus-evidence-2026-10-14.md)

## Branching and assessment evidence

Each sprint is developed on a feature branch and merged through a pull request. Commits, tests, architecture decisions, sprint records and verification evidence are retained so the project demonstrates professional software engineering practice rather than only a polished interface.

## Licence and data

No production licence has yet been selected. Do not deploy the project with real personal data until the university has approved the privacy, information-security, accessibility, retention and safeguarding arrangements.
