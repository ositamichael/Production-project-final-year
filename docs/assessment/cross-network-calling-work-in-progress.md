# Cross-network voice and video calling: work-in-progress evidence

**Recorded:** 26 November 2026

**Live pilot:** <https://unicircle-friends-pilot.onrender.com/>

**Status:** The software support is implemented and automatically tested. The live pilot still needs a real TURN relay account and private deployment settings before calls can be proven across different networks.

## The problem in plain English

UniCircle voice and video calls can work when two devices have an easy direct route to each other. They can fail when the devices use different internet connections, for example:

- a laptop on university Wi-Fi and a phone on mobile data;
- two different home or public Wi-Fi networks;
- users in different countries; or
- a network with a strict firewall or carrier-grade NAT.

The call can ring and be accepted while the sound or video still fails to connect. This is because ringing uses UniCircle's normal HTTPS/WebSocket server, but the live sound and video use a separate WebRTC media connection between the two devices.

This is not a fault unique to eduroam. The same problem can occur with any pair of networks that cannot create a direct WebRTC path.

## Why the original approach failed

The pilot currently has a public STUN server configured. STUN helps each device discover the public internet address that its router presents. It does **not** carry the call when routers or firewalls refuse the direct connection.

An everyday comparison is:

- **STUN is like finding somebody's address.**
- **TURN is like using a trusted meeting point when neither person can reach the other's address directly.**

The missing production dependency is a TURN relay. When a direct connection fails, both devices connect to the relay and it passes the encrypted WebRTC media between them.

Render hosts the UniCircle website, API and signalling WebSocket. A normal Render web service does not automatically become a TURN server, because TURN needs its own public relay infrastructure, network ports, bandwidth and operational monitoring.

## Why there can be a cost

Relaying live audio and especially video consumes network bandwidth. A provider must pay for the servers and data transfer, so a dependable production relay is normally a paid infrastructure service.

The project has been prepared for Xirsys because it offers standard WebRTC TURN endpoints and temporary credentials:

- On 26 November 2026, its free Developer account advertised a 30-day production trial.
- After the trial, the free account advertised one testing region and a hard limit of 500 MB of TURN traffic per month.
- When that free allowance is exhausted, relay-dependent calls stop until the monthly reset.
- Its advertised production plans started at US $39 per month on the date above.

Pricing and allowances can change, so the current provider page must be checked before making a budget decision: <https://xirsys.com/pricing>.

The free account is suitable for a limited university demonstration, but it cannot honestly guarantee continuous production availability. The alternatives are a paid managed TURN plan or a securely operated coturn server, which still has hosting, bandwidth and maintenance costs.

## How I handled the problem

I did not put a permanent relay password in the browser or source code. The implementation uses the following safer design:

1. The browser asks the authenticated UniCircle backend for call configuration immediately before a call.
2. The backend requests short-lived TURN credentials from Xirsys, or creates temporary coturn-compatible credentials when a shared secret is configured.
3. Only temporary ICE server details are returned to the signed-in browser.
4. The browser first attempts a direct connection and automatically falls back to TURN when the networks require it.
5. A relay-only test setting is available to prove that calls really pass through TURN instead of accidentally succeeding directly.
6. Provider failures return a clear unavailable response rather than pretending the call is protected by a working relay.

The main implementation evidence is:

| Evidence | Purpose |
| --- | --- |
| `src/main/java/uk/ac/unicircle/social/CallConfigurationController.java` | Builds the authenticated STUN/TURN configuration and temporary coturn credentials. |
| `src/main/java/uk/ac/unicircle/social/XirsysTurnClient.java` | Requests temporary Xirsys credentials on the server without exposing the account secret. |
| `src/main/resources/static/js/app.js` | Supplies the ICE servers and transport policy to `RTCPeerConnection` and explains relay status to the user. |
| `src/main/resources/application.yml` | Maps private deployment environment variables into the calling configuration. |
| `.env.example` | Documents the required variable names without containing real secrets. |
| `docs/calling-across-networks.md` | Contains the detailed technical configuration and physical-device acceptance plan. |
| `src/test/java/uk/ac/unicircle/social/CallConfigurationTests.java` | Tests manual TURN configuration and temporary credentials. |
| `src/test/java/uk/ac/unicircle/social/XirsysTurnClientTests.java` | Tests safe provider responses, validation and failure handling. |
| `src/test/java/uk/ac/unicircle/social/CallSignallingTests.java` | Tests authenticated call signalling and configuration access. |
| `scripts/test-calls.mjs` | Tests important browser call setup, media, audio and cleanup behaviour with simulated WebRTC objects. |

## Verification completed on 26 November 2026

The focused backend and browser tests were run with:

```text
.\mvnw.cmd -q '-Dtest=CallConfigurationTests,XirsysTurnClientTests,CallSignallingTests' test
node --test scripts/test-calls.mjs
```

Both commands completed successfully. These tests prove that the application prepares and handles relay configuration correctly. They do not prove that a real relay account, real firewall, physical phone, microphone or camera is working.

## What still needs to be done on the live deployment

For the Xirsys route, the project owner must create or approve the provider account and accept its terms. A channel such as `UniCircle` must then be created. The following values must be stored as private environment variables on the Render service, never committed to GitHub:

```text
UNICIRCLE_XIRSYS_IDENT=<Xirsys account ident>
UNICIRCLE_XIRSYS_SECRET=<Xirsys API secret>
UNICIRCLE_XIRSYS_CHANNEL=UniCircle
UNICIRCLE_CALL_ICE_TRANSPORT_POLICY=all
```

All manual `UNICIRCLE_TURN_*` variables must remain empty when the Xirsys method is used. Render must then redeploy the service.

## Acceptance evidence required before calling it complete

After the relay is configured, the following evidence must be recorded without exposing credentials or private session data:

1. Place voice and video calls in both directions between a laptop on Wi-Fi and a phone with Wi-Fi disabled and mobile data enabled.
2. Repeat with different Wi-Fi providers and, where practical, users in different locations.
3. Temporarily set `UNICIRCLE_CALL_ICE_TRANSPORT_POLICY=relay` and confirm in browser WebRTC diagnostics that the selected candidate type is `relay` and media byte counters increase in both directions.
4. Test UDP plus TCP/TLS fallback, including TURN over TLS on port 443 when the provider supplies it.
5. Record device, browser, network type, test time, connection result, sound/video result and any failure message.
6. Restore `UNICIRCLE_CALL_ICE_TRANSPORT_POLICY=all` after the forced-relay test so direct calls remain available and TURN bandwidth is used only when needed.

## Honest presentation conclusion

The failure was investigated and traced to missing relay infrastructure rather than the call buttons or signalling code. The secure TURN integration and automated tests are complete. Live cross-network proof remains work in progress because a third-party relay account, deployment secrets and physical-device testing are still required.

No WebRTC service can promise that literally every network worldwide will allow real-time media. TURN with UDP, TCP and TLS fallback is the accepted practical solution for making calls work across the widest reasonable range of internet connections.
