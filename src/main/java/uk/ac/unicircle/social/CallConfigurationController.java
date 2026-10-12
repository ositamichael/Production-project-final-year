package uk.ac.unicircle.social;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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

    CallConfigurationController(
            @Value("${unicircle.calls.stun-urls:}") String stunUrls,
            @Value("${unicircle.calls.turn-urls:}") String turnUrls,
            @Value("${unicircle.calls.turn-username:}") String turnUsername,
            @Value("${unicircle.calls.turn-credential:}") String turnCredential) {
        this.stunUrls = split(stunUrls);
        this.turnUrls = split(turnUrls);
        this.turnUsername = turnUsername.trim();
        this.turnCredential = turnCredential.trim();
    }

    @GetMapping("/configuration")
    CallConfiguration configuration() {
        List<IceServer> servers = new ArrayList<>();
        if (!stunUrls.isEmpty()) servers.add(new IceServer(stunUrls, null, null));
        if (!turnUrls.isEmpty() && !turnUsername.isBlank() && !turnCredential.isBlank()) {
            servers.add(new IceServer(turnUrls, turnUsername, turnCredential));
        }
        return new CallConfiguration(
                servers,
                !turnUrls.isEmpty() && !turnUsername.isBlank() && !turnCredential.isBlank(),
                "Calls use browser WebRTC and are never recorded by UniCircle.");
    }

    private List<String> split(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .distinct()
                .toList();
    }

    record CallConfiguration(List<IceServer> iceServers, boolean relayConfigured, String privacyNotice) {
    }

    record IceServer(List<String> urls, String username, String credential) {
    }
}
