package uk.ac.unicircle.social;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Server-side adapter; no account API secret is sent to browsers. */
@Component
class XirsysTurnClient {
    private final String ident;
    private final String secret;
    private final String channel;
    private final ObjectMapper mapper;
    private final Transport transport;

    @Autowired
    XirsysTurnClient(@Value("${unicircle.calls.xirsys.ident:}") String ident,
            @Value("${unicircle.calls.xirsys.secret:}") String secret,
            @Value("${unicircle.calls.xirsys.channel:}") String channel, ObjectMapper mapper) {
        this(ident, secret, channel, mapper, defaultTransport());
    }

    XirsysTurnClient(String ident, String secret, String channel, ObjectMapper mapper, Transport transport) {
        this.ident = ident.trim();
        this.secret = secret.trim();
        this.channel = channel.trim();
        this.mapper = mapper;
        this.transport = transport;
        boolean anySetting = !this.ident.isBlank() || !this.secret.isBlank() || !this.channel.isBlank();
        if (anySetting && (this.ident.isBlank() || this.secret.isBlank() || this.channel.isBlank())) {
            throw new IllegalArgumentException("Xirsys calling requires ident, secret and channel together.");
        }
        if (anySetting && (!this.channel.matches("[A-Za-z0-9_-]{1,128}") || this.ident.contains(":"))) {
            throw new IllegalArgumentException("Use a simple Xirsys channel name (letters, digits, _ or -) and a valid ident.");
        }
    }

    boolean enabled() {
        return !ident.isBlank() && !secret.isBlank() && !channel.isBlank();
    }

    List<CallConfigurationController.IceServer> iceServers(long ttlSeconds) {
        if (!enabled()) return List.of();
        URI endpoint = URI.create("https://global.xirsys.net/_turn/" + channel
                + "?webrtc=1&expire=" + Math.min(ttlSeconds, 21600));
        String authorization = "Basic " + Base64.getEncoder().encodeToString(
                (ident + ":" + secret).getBytes(StandardCharsets.UTF_8));
        try {
            ProviderResponse response = transport.send(endpoint, authorization);
            if (response.status() != 200 || response.body().length() > 65536) throw unavailable();
            JsonNode data = mapper.readTree(response.body());
            JsonNode servers = data.path("v").path("iceServers");
            if (!data.path("s").asText("").equals("ok") || !servers.isArray() || servers.isEmpty()) {
                throw unavailable();
            }
            List<CallConfigurationController.IceServer> result = new ArrayList<>();
            boolean hasTurn = false;
            for (JsonNode server : servers) {
                JsonNode urls = server.path("urls");
                if (!urls.isArray() || urls.isEmpty()) throw unavailable();
                List<String> acceptedUrls = new ArrayList<>();
                boolean turn = false;
                for (JsonNode url : urls) {
                    String value = url.asText("");
                    if (!(value.startsWith("stun:") || value.startsWith("stuns:")
                            || value.startsWith("turn:") || value.startsWith("turns:"))) throw unavailable();
                    turn |= value.startsWith("turn:") || value.startsWith("turns:");
                    acceptedUrls.add(value);
                }
                String username = server.path("username").asText("");
                String credential = server.path("credential").asText("");
                if (turn && (username.isBlank() || credential.isBlank()
                        || username.equals(ident) || credential.equals(secret))) throw unavailable();
                hasTurn |= turn;
                result.add(new CallConfigurationController.IceServer(List.copyOf(acceptedUrls),
                        username.isBlank() ? null : username, credential.isBlank() ? null : credential));
            }
            if (!hasTurn) throw unavailable();
            return List.copyOf(result);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException | IllegalArgumentException | tools.jackson.core.JacksonException failure) {
            // Never return provider bodies, headers or exception messages containing credentials.
            throw unavailable();
        }
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Call relay unavailable. Check the free allowance and relay settings.");
    }

    private static Transport defaultTransport() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        return (endpoint, authorization) -> {
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(8))
                    .header("Authorization", authorization).header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new ProviderResponse(response.statusCode(), response.body());
        };
    }

    @FunctionalInterface
    interface Transport {
        ProviderResponse send(URI endpoint, String authorization) throws IOException, InterruptedException;
    }

    record ProviderResponse(int status, String body) {
    }
}
