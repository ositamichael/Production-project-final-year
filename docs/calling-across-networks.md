# Voice and video calls across networks

## Current status

UniCircle supports WebRTC voice/video signalling between accepted connections. The application can supply TURN endpoints and either provider credentials or temporary coturn-compatible credentials. This code change does **not** provision a relay, configure a live hosting account, or prove an international call. Example hostnames are not working infrastructure.

Users in different countries must open the **same public HTTPS UniCircle deployment**, not `localhost` or `127.0.0.1`. The local `pilot-local` profile is not for public exposure. Keep the pilot on one application instance until its in-memory sessions and WebSocket signalling are replaced with a shared design.

TURN carries encrypted WebRTC media when routers or firewalls prevent a direct connection. It cannot guarantee that all networks allow a call or that bandwidth, latency, microphones and speakers will be adequate. Both people currently need UniCircle open and signed in; background/closed-browser incoming calls and push notifications are not implemented.

On 16 October 2026 the owner chose not to proceed with relay account creation. The optional provider adapter remains dormant unless private settings are supplied. Do not activate a provider, create an account, add billing, or claim verified cross-network calls on the strength of the application tests.

The answer flow acknowledges an Accept click before waiting for device permission, keeps End available during capture setup, and explains a permission prompt left pending for 25 seconds. Call-configuration requests time out after 10 seconds. Outgoing ICE candidates are queued until the offer/answer is sent, and late results from an ended call cannot clear a newer call. The media/setup deadline is 45 seconds. The call dialog can scroll within the device viewport. These are application hardening measures, not a replacement for TURN or proof of a fix on a particular physical phone.

## Relay configuration: a deployment dependency

Obtain a managed TURN service, or deploy coturn on an appropriately secured public server. Do not create a paid service without the owner's approval. Ensure its advertised endpoints are reachable from outside its network. Provide UDP TURN plus TCP/TLS fallback, with a valid certificate for `turns:`. TLS on port 443 is useful on restrictive networks **only if the provider/server actually offers it**. A normal HTTPS web host does not automatically provide TURN.

Configure these values in the hosting provider's secret/environment settings, or IntelliJ's environment settings for controlled local tests. Spring Boot does not automatically load `.env.example`.

```text
UNICIRCLE_TURN_URLS=<provider's comma-separated turn: and turns: endpoints>
UNICIRCLE_CALL_ICE_TRANSPORT_POLICY=all
```

Select exactly one authentication method (do not mix manual and Xirsys settings):

1. **Provider username/password:** set `UNICIRCLE_TURN_USERNAME` and `UNICIRCLE_TURN_CREDENTIAL`; leave `UNICIRCLE_TURN_SHARED_SECRET` empty. Follow the provider's credential expiry and rotation instructions. This adapter does not call a proprietary provider token API.
2. **coturn TURN REST shared secret:** enable coturn's `use-auth-secret`, configure a strong `static-auth-secret`, and put the same value in the application's `UNICIRCLE_TURN_SHARED_SECRET`. Leave the username/credential settings empty. Only a temporary username and derived credential reach the authenticated browser. The signing secret never does. `UNICIRCLE_TURN_CREDENTIAL_TTL_SECONDS` defaults to 21600 (six hours), and accepts 60–86400 seconds. Keep server clocks synchronised. Credentials are refreshed for each new call, not during an ongoing call: choose a lifetime covering the longest allowed call; very long calls spanning expiry have not been validated.
3. **Xirsys free Developer plan:** create the free Developer account yourself and review/accept its terms. Do not select a paid plan, add billing or enable an upgrade. Create a channel named `UniCircle`. Leave `UNICIRCLE_TURN_URLS`, `UNICIRCLE_TURN_USERNAME`, `UNICIRCLE_TURN_CREDENTIAL` and `UNICIRCLE_TURN_SHARED_SECRET` empty. Set `UNICIRCLE_XIRSYS_IDENT`, `UNICIRCLE_XIRSYS_SECRET` and `UNICIRCLE_XIRSYS_CHANNEL=UniCircle` in private server environment settings. Do not paste the account secret into chat. UniCircle calls only `https://global.xirsys.net/_turn/UniCircle?webrtc=1&expire=...` to request temporary credentials; it does not call billing, upgrade or subscription APIs. The API secret stays on the backend. Provider failure returns an unavailable message, not a false claim of successful calling.

### Zero-cost prototype limits (verified 16 October 2026)

The owner's budget is £0. [Xirsys's published free Developer plan](https://xirsys.com/pricing) has an ongoing 500 MB/month TURN hard cap, two channels and one testing region after its initial 30-day feature trial. [Its FAQ](https://xirsys.com/faq) says relay service stops at the free limit until reset or upgrade. Do not upgrade: reduced availability is preferable to a charge. One region can relay between users in different countries, but routing/latency and availability must be tested; proximity across all regions is not promised. The allowance is shared across the account, and video can consume it quickly.

The trial's static credentials stop working after the trial unless the account upgrades. This integration uses the [temporary-credential API](https://docs.xirsys.com/api/turn), which remains available on the free plan, instead. Request lifetimes are capped at the provider's six-hour maximum. Account creation, email verification, private configuration and deployment have not been performed by this code change. No live free-plan connection or quota enforcement was tested; these are provider-published limits, not measurements.

There is no unlimited worldwide-relay promise. Free direct calls can continue wherever the two networks permit them, but relay-dependent calls cannot be guaranteed after the free quota is exhausted or when the service is unavailable. Both users also need ordinary internet access. No API integration can prove the account is on a free plan: the owner must check the dashboard and keep paid billing disabled.

Do not paste live secrets into chat, source code or screenshots. Configure relay quotas, bandwidth monitoring and private/loopback peer restrictions according to the provider/coturn security guidance. Temporary credentials remain usable until expiry even after app logout: keep their lifetime proportionate and review abuse controls before public use.

The authenticated `/api/social/calls/configuration` response uses `Cache-Control: no-store`. `relayConfigured=true` only confirms settings are present, **not** DNS, firewall, certificate, credential or media connectivity. `iceTransportPolicy` is `all` (direct or relay) normally; use `relay` temporarily for the forced-relay acceptance test. Relay-only mode refuses to start without configured TURN credentials.

## Audio and privacy

Microphone/camera access is requested when starting or accepting a call, never merely on login. Users must grant their browser permission. Audio has its own player; the video display is muted to prevent duplicate playback. Audio/video tracks arriving separately are accumulated instead of overwriting each other. Echo cancellation, noise suppression and automatic gain are requested where supported.

If a browser blocks playback, **Enable call sound** appears with a retry and device-volume/headset reminder. This does not bypass browser or OS restrictions. Sound uses the browser/OS's selected output; portable in-app speaker/headset selection is not implemented. No claim is made that physical speaker output has been tested by automated logic tests.

## Acceptance tests — still required with real infrastructure

Record app commit/version, browser/device versions, relay region/transports, time, results and failures. Do not record raw credentials or private session descriptions.

1. With the same HTTPS deployment, connect two separate accounts and accept the connection request. Confirm unsolicited/pending/blocked calls are denied.
2. Use a laptop on Wi-Fi and a phone on mobile data (Wi-Fi disabled). Place voice and video calls **both directions**. Confirm two-way sound, video, mute/unmute, camera on/off, reject and hang-up. Repeat with Safari and Chrome, and with headphones and the device's normal output.
3. Set `UNICIRCLE_CALL_ICE_TRANSPORT_POLICY=relay`, restart the deployment, and repeat step 2. Confirm the selected ICE candidate pair uses a `relay` candidate in browser WebRTC diagnostics, and confirm media byte counters rise both ways. Merely seeing a TURN candidate is insufficient. This test must fail clearly with an invalid relay credential; restore the real configuration afterwards.
4. Where supported, test UDP and TCP/TLS fallback independently using the provider's actual endpoints. Test restricted campus/mobile networks and a UK-to-another-country pair. Measure connection time, packet loss, round-trip time and visible/audio interruptions. Restore `all` after testing unless relay-only routing is an explicit deployment policy.
5. Test denied microphone/camera permission, browser-blocked playback and **Enable call sound**, device volume/headset routing, logout, and a brief network outage. A disconnected call currently waits briefly then ends if connectivity does not recover; seamless Wi-Fi/mobile handover is not promised.
6. Leave a tab signed in beyond the temporary credential lifetime, then start a **new** call and confirm fresh credentials are used. Test calls near the maximum allowed duration separately.

Automated checks: `./mvnw verify` covers configuration, authentication and signalling; `node --test scripts/test-calls.mjs` covers isolated frontend configuration/audio/track/cleanup logic using simulated media. Neither replaces the physical-device and live-relay tests above.

## Primary references

- [WebRTC: TURN server](https://webrtc.org/getting-started/turn-server)
- [coturn authentication configuration](https://github.com/coturn/coturn/blob/master/examples/etc/turnserver.conf)
- [MDN: handling blocked media playback](https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement/play)
