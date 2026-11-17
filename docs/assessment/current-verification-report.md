# UniCircle current verification report

**Verification date:** 17 November 2026

**Commit tested:** `9c6f864038869d74359dd714a80438ee90cb91f7`

**Branch:** `feat/unicircle-friends-pilot`

**Environment:** Linux, Temurin/OpenJDK 21-compatible runtime, Maven Wrapper

## Commands executed

```bash
./mvnw --batch-mode clean verify
node --test scripts/test-calls.mjs
java -jar target/unicircle-0.0.1-SNAPSHOT.jar
curl -i http://127.0.0.1:8080/
curl -i http://127.0.0.1:8080/actuator/health
```

## Maven result

- **Build:** success
- **Tests discovered and executed:** 158
- **Passed:** 158
- **Failed:** 0
- **Errors:** 0
- **Skipped:** 0
- **Maven wrapper:** `./mvnw`
- **Packaging:** `target/unicircle-0.0.1-SNAPSHOT.jar`
- **JaCoCo quality gate:** passed; minimum 75% line coverage is enforced
- **JaCoCo instruction coverage:** 83%
- **JaCoCo line coverage:** 81.7%
- **JaCoCo branch coverage:** 56.3%
- **JaCoCo method coverage:** 84.7%
- **Flyway migrations:** current migration set is exercised by the application/test build; the migration set is V1–V18

## JavaScript result

- **Tests:** 16
- **Passed:** 16
- **Failed:** 0
- **Skipped:** 0
- **Method:** isolated VM tests using simulated media and peer objects
- **Scope:** call configuration, playback, track cleanup, signalling buffers and permission timeout behaviour
- **Limitation:** no real microphone, speaker, browser permission, physical device or live TURN relay was exercised

## Application smoke test

The packaged JAR started and returned:

- `/`: HTTP 200
- `/actuator/health`: HTTP 200
- Health status: `{"groups":["liveness","readiness"],"status":"UP"}`

This demonstrates local application startup and health exposure. It is not an institutional deployment or PostgreSQL rehearsal.

## Warnings and known limitations

- No controlled HTTP/WebSocket load test was executed.
- No independent WCAG 2.2 AA audit was executed.
- No physical-device or cross-network WebRTC relay test was executed.
- No managed PostgreSQL backup/restore rehearsal was executed.
- No participant study or ethical recruitment has been performed.
- No live university SSO or student-record integration is configured or approved.
- Coverage is descriptive and does not prove absence of defects.

## Reproduction instructions

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
node --test scripts/test-calls.mjs
```

Linux/macOS/Git Bash:

```bash
./mvnw clean verify
./mvnw spring-boot:run
node --test scripts/test-calls.mjs
```
