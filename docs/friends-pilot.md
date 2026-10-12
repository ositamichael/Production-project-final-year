# UniCircle campus pilot

This is an independent, invitation-only test. It is **not** Leeds Beckett sign-in, a verified university directory, an official timetable or an emergency location service. Never ask friends to enter their university password or Authenticator code.

## What invited participants can test

Each participant creates a separate account using a private invitation code and their own password, then adds a self-declared course and year of study. They can find other pilot members, send and accept connection requests, exchange persistent messages, make live voice or video calls, upload a profile photo, block or report contact, and voluntarily share a short-lived location with accepted connections who are also sharing. They can also browse test campus events, register for an event, book a test service slot, explore verified fictional opportunities and record private application progress. Calls are permitted only between accepted connections, are not recorded by UniCircle, and work only while both people have UniCircle open. The location is rounded, expires five minutes after the last update, and can be deleted immediately. Browsers require HTTPS for camera, microphone and location permission outside localhost.

Course, module, event, service and opportunity information in this independent pilot is created specifically for testing. It is not supplied or verified by Leeds Beckett University or another institution. Official timetables, university authentication and staffed services still require approved integrations and governance. Do not represent pilot records as live university services.

## Local rehearsal

Use a 32-character-or-longer random invitation code kept out of Git. In IntelliJ, set the active profiles to `pilot,pilot-local`, set `PILOT_INVITE_CODE`, and start `UniCircleApplication`. Visit `http://localhost:8080/sign-in`. The local rehearsal uses a persistent H2 database file so accounts and messages survive application restarts. The cookie is not marked Secure in this local-only profile; **never expose this local profile to the public internet**.

Open two separate browser profiles or devices on the same private network to test different accounts. Do not distribute a `127.0.0.1` link: that address points to each person's own device.

## Hosted Render pilot

Create one Render web service from the `unicircle` directory with the included Dockerfile, and one managed PostgreSQL database. Keep one web-service instance for the pilot because live WebSocket subscriptions and login sessions are currently held in server memory. Set these environment variables in Render, not in Git:

- `SPRING_PROFILES_ACTIVE=pilot`
- `PILOT_INVITE_CODE` — generate a unique random secret of at least 32 characters and share it privately with invited testers only.
- `DATABASE_URL=jdbc:postgresql://<internal-database-host>:5432/<database-name>` — a **JDBC URL**, not Render's `postgresql://` connection string.
- `DATABASE_USERNAME` and `DATABASE_PASSWORD` — from the Render PostgreSQL service.
- `UNICIRCLE_STUN_URLS` — comma-separated STUN addresses used by WebRTC.
- `UNICIRCLE_TURN_URLS`, `UNICIRCLE_TURN_USERNAME` and `UNICIRCLE_TURN_CREDENTIAL` — credentials from a separate TURN service. TURN is required for dependable calls across restrictive mobile, campus and home networks; Render hosts UniCircle and its WebSocket signalling but is not itself a TURN relay.

Use `/actuator/health` as the health-check path. Render provides an HTTPS public address; this is the link friends can open. Invite them one at a time and test sign-in, request/accept, chat, calls, photo, and location on two devices before increasing the pilot group. Do not put real student records, official university content or sensitive documents into this independent pilot.

The current pilot reads TURN credentials from environment variables. Do not commit them. A university-scale deployment should replace long-lived shared TURN credentials with short-lived credentials issued by a trusted backend, add abuse monitoring, and complete a communications privacy and security review.

The Render account is only the owner's private hosting and billing control panel. Testers must never receive its password. Every tester instead opens the same public UniCircle address and creates a separate pilot account using the private invitation code. After signing in, they complete their own profile, use **Find friends** to locate other pilot members, send a connection request and wait for acceptance. Accepted connections can open a private conversation and exchange messages from separate devices.

Profile data and messages are stored in PostgreSQL. Email addresses and passwords are not shown in the member directory. Location is off by default and is not background tracking: a tester must open the map, approve the browser permission and switch sharing on. A position is visible only while it is fresh and only to an accepted connection who has also chosen to share. Closing the browser or leaving the map stops regular updates; turning sharing off deletes the saved position.

## Test checklist

1. Register two different invited people. A wrong invitation must fail. Neither can use the fictional demo sign-in.
2. Sign in on separate devices, find each other, send and accept a request, then message in both directions. Refresh and sign in again to confirm messages persist.
3. From an accepted conversation, make one voice call and one video call in both directions. Confirm the receiver can accept or decline, mute works, the camera can be switched off, and either person can end the call. Repeat once with the two devices on different networks to verify TURN relay operation.
4. Try calling a pending or blocked account and confirm the server refuses it. Close UniCircle on the receiving device and confirm the caller receives an unavailable message rather than a false connection.
5. Upload a profile photo; verify it is still present after a server restart.
6. Confirm location is initially off, a pending connection cannot view it, and one-sided sharing shows nothing. With an accepted connection and both sharing, each should see the other's fresh location.
7. Turn sharing off and confirm the other person can no longer see the position. Wait over five minutes without refreshing and confirm it disappears.
8. Block a user and confirm messaging, calling and location no longer work for that pair.
9. Check mobile layout, camera/microphone permission denial, network interruption/reconnection and logout on both devices.

## Before a university deployment

This pilot must not be treated as production approval. University identity federation, verified student eligibility, consent and safeguarding process, staff moderation, accessibility testing, penetration testing, backups and recovery drills, a privacy notice, data retention/deletion, and official data agreements all require review and work with the institution. A successful friends test is evidence of the core social journey, not proof that all campus modules are ready.
