# Call hardening evidence — 16 October 2026

## Scope and version

Call changes prepared from base commit `1b77296` on `feat/unicircle-friends-pilot`. The public pilot was observed serving script version 40 before publication; this revision uses script version 43, stylesheet version 40 and service-worker shell version 13. Publication to the existing pilot was authorised by the owner. This document is not evidence that a deployment or physical-device call succeeded.

## Verified locally

- Environment: macOS, IntelliJ-bundled Java 25.0.4, Maven compiler release 21; offline dependency cache.
- `JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" ./mvnw -o verify`: **153 tests, zero failures/errors/skips; BUILD SUCCESS**, finished 16 October 2026 at 08:22:42 Europe/London. Reports are in `target/surefire-reports/`.
- `node --test scripts/test-calls.mjs`: **16 tests passed**, no failures/skips. These use simulated media and timers, not real devices or network relays.
- `node --check src/main/resources/static/js/app.js` and `git diff --check`: passed during preparation.

The checks cover authenticated configuration, temporary relay credential generation, optional provider parsing/error handling, accepted-connection signalling, accepting/answer/candidate delivery, caller acknowledgement, permission timeout/denial, aborting stalled configuration requests, candidate ordering, separate audio/video tracks, audio playback retry, and cleanup after cancellation. Invalid/expired configuration is not represented as successful media connectivity.

## User-reported failure and changes

The owner reported that an incoming call appears on a phone using mobile data while the laptop uses Wi-Fi, but tapping Accept does not complete the call. Both use the public Render pilot. This establishes a reported symptom, not a proven device-specific root cause.

Changes provide an immediate answering acknowledgement, a visible permission/setup state, a cancellable answer flow, a 25-second capture-permission deadline, a 10-second configuration-fetch deadline, ordered local ICE candidates, a 45-second setup deadline, scrollable call controls, and explicit audio playback recovery. Late media results from an ended call cannot reset a newer call's audio. Asset versions were increased so a refreshed installed app can receive the changes.

## Not verified / remaining dependencies

- No microphone, camera, physical speaker, headset, phone browser permission or actual cross-network/overseas call has been verified by these tests.
- No TURN account was created, no provider secret supplied, and no payment/subscription/upgrade was enabled by this work. The owner declined relay account setup. Provider adapters remain optional and disabled without private configuration.
- Direct WebRTC may work between some networks; routers, mobile carriers and firewalls may require a working public TURN relay. Code fixes alone do not remove that infrastructure dependency.
- Forced-relay candidate-pair selection, two-way media byte counters, relay reachability/credentials and free-plan quota behaviour still require a live configured service and two-device tests.
- Public deployment delivery must be checked separately. Each device must refresh the public HTTPS app after publication and use separate accepted accounts with the app open. Do not confuse a passing local build with an updated Render deployment.

See [Calling across networks](../calling-across-networks.md) for the acceptance checklist and the owner's zero-cost constraint.
