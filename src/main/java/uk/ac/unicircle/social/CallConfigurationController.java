package uk.ac.unicircle.social;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social/calls")
class CallConfigurationController {

    private final List<String> stunUrls;
    private final List<String> turnUrls;
    private final String turnUsername;
    private final String turnCredential;
    private final String turnSharedSecret;
    private final long credentialTtlSeconds;
    private final String iceTransportPolicy;
    private final Clock clock;
    private final XirsysTurnClient xirsys;

    @Autowired
    CallConfigurationController(
            @Value("${unicircle.calls.stun-urls:}") String stunUrls,
            @Value("${unicircle.calls.turn-urls:}") String turnUrls,
            @Value("${unicircle.calls.turn-username:}") String turnUsername,
            @Value("${unicircle.calls.turn-credential:}") String turnCredential,
            @Value("${unicircle.calls.turn-shared-secret:}") String turnSharedSecret,
            @Value("${unicircle.calls.credential-ttl-seconds:21600}") long credentialTtlSeconds,
            @Value("${unicircle.calls.ice-transport-policy:all}") String iceTransportPolicy,
            XirsysTurnClient xirsys) {
        this(stunUrls, turnUrls, turnUsername, turnCredential, turnSharedSecret,
                credentialTtlSeconds, iceTransportPolicy, Clock.systemUTC(), xirsys);
    }

    CallConfigurationController(String stunUrls, String turnUrls, String turnUsername,
            String turnCredential, String turnSharedSecret, long credentialTtlSeconds,
            String iceTransportPolicy, Clock clock) {
        this(stunUrls, turnUrls, turnUsername, turnCredential, turnSharedSecret,
                credentialTtlSeconds, iceTransportPolicy, clock, null);
    }

    CallConfigurationController(String stunUrls, String turnUrls, String turnUsername,
            String turnCredential, String turnSharedSecret, long credentialTtlSeconds,
            String iceTransportPolicy, Clock clock, XirsysTurnClient xirsys) {
        this.stunUrls = split(stunUrls);
        this.turnUrls = split(turnUrls);
        this.turnUsername = turnUsername.trim();
        this.turnCredential = turnCredential.trim();
        this.turnSharedSecret = turnSharedSecret.trim();
        if (credentialTtlSeconds < 60 || credentialTtlSeconds > 86400) {
            throw new IllegalArgumentException("TURN credential lifetime must be between 60 and 86400 seconds.");
        }
        if (!List.of("all", "relay").contains(iceTransportPolicy)) {
            throw new IllegalArgumentException("Call ICE transport policy must be all or relay.");
        }
        if (this.turnUrls.stream().anyMatch(url -> !url.startsWith("turn:") && !url.startsWith("turns:"))) {
            throw new IllegalArgumentException("TURN URLs must start with turn: or turns:.");
        }
        if (!this.turnSharedSecret.isBlank() && (!this.turnUsername.isBlank() || !this.turnCredential.isBlank())) {
            throw new IllegalArgumentException("Configure a TURN shared secret OR a username and credential, not both.");
        }
        this.credentialTtlSeconds = credentialTtlSeconds;
        this.iceTransportPolicy = iceTransportPolicy;
        this.clock = clock;
        this.xirsys = xirsys;
        if (xirsysEnabled() && (!this.turnUrls.isEmpty() || !this.turnUsername.isBlank()
                || !this.turnCredential.isBlank() || !this.turnSharedSecret.isBlank())) {
            throw new IllegalArgumentException("Choose Xirsys OR manual TURN settings, not both.");
        }
        if (iceTransportPolicy.equals("relay") && !relayConfigured()) {
            throw new IllegalArgumentException("Relay-only calls require TURN URLs and valid TURN credential settings.");
        }
    }

    @GetMapping("/configuration")
    ResponseEntity<CallConfiguration> configuration() {
        List<IceServer> servers = new ArrayList<>();
        if (!stunUrls.isEmpty()) servers.add(new IceServer(stunUrls, null, null));
        long expiresAt = 0;
        if (xirsysEnabled()) {
            servers.addAll(xirsys.iceServers(credentialTtlSeconds));
            expiresAt = clock.instant().getEpochSecond() + Math.min(credentialTtlSeconds, 21600);
        } else if (relayConfigured()) {
            if (!turnSharedSecret.isBlank()) {
                expiresAt = clock.instant().getEpochSecond() + credentialTtlSeconds;
                String username = expiresAt + ":" + UUID.randomUUID();
                servers.add(new IceServer(turnUrls, username, temporaryCredential(username)));
            } else {
                servers.add(new IceServer(turnUrls, turnUsername, turnCredential));
            }
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new CallConfiguration(
                servers,
                relayConfigured(),
                "Calls use browser WebRTC and are never recorded by UniCircle.",
                iceTransportPolicy, expiresAt));
    }

    private boolean relayConfigured() {
        return xirsysEnabled() || (!turnUrls.isEmpty() && (!turnSharedSecret.isBlank()
                || (!turnUsername.isBlank() && !turnCredential.isBlank())));
    }

    private boolean xirsysEnabled() {
        return xirsys != null && xirsys.enabled();
    }

    private String temporaryCredential(String username) {
        try {
            // The coturn TURN REST authentication protocol specifies HMAC-SHA1.
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(turnSharedSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(username.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException error) {
            throw new IllegalStateException("Could not issue temporary TURN credentials.", error);
        }
    }

    private List<String> split(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .distinct()
                .toList();
    }

    record CallConfiguration(List<IceServer> iceServers, boolean relayConfigured, String privacyNotice,
            String iceTransportPolicy, long expiresAtEpochSeconds) {
    }

    record IceServer(List<String> urls, String username, String credential) {
    }
}
