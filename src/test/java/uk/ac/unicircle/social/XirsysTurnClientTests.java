package uk.ac.unicircle.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.ObjectMapper;

class XirsysTurnClientTests {
    private static final String SECRET = "fictional-account-api-secret";
    private static final String VALID_RESPONSE = """
            {"s":"ok","v":{"iceServers":[{"urls":["stun:relay.example.test:3478",
            "turn:relay.example.test:3478?transport=udp","turns:relay.example.test:443?transport=tcp"],
            "username":"temporary-user","credential":"temporary-password"}]}}
            """;

    private XirsysTurnClient client(XirsysTurnClient.Transport transport) {
        return new XirsysTurnClient("test-ident", SECRET, "UniCircle", new ObjectMapper(), transport);
    }

    @Test
    void requestsTemporaryIceCredentialsFromOnlyTheFixedProviderEndpoint() {
        var client = client((uri, auth) -> {
            assertThat(uri.toString()).isEqualTo("https://global.xirsys.net/_turn/UniCircle?webrtc=1&expire=21600");
            assertThat(new String(Base64.getDecoder().decode(auth.substring(6)), StandardCharsets.UTF_8))
                    .isEqualTo("test-ident:" + SECRET);
            return new XirsysTurnClient.ProviderResponse(200, VALID_RESPONSE);
        });
        var servers = client.iceServers(86400);
        assertThat(servers).hasSize(1);
        assertThat(servers.getFirst().urls()).hasSize(3);
        assertThat(servers.getFirst().username()).isEqualTo("temporary-user");
        assertThat(servers.getFirst().credential()).isEqualTo("temporary-password");
        assertThat(servers.toString()).doesNotContain(SECRET, "test-ident");
    }

    @Test
    void disabledProviderNeverMakesAnExternalRequest() {
        var client = new XirsysTurnClient("", "", "", new ObjectMapper(), (uri, auth) -> {
            throw new AssertionError("No provider request expected");
        });
        assertThat(client.enabled()).isFalse();
        assertThat(client.iceServers(600)).isEmpty();
    }

    @Test
    void rejectsPartialConfigurationAndChannelPathInjection() {
        assertThatThrownBy(() -> new XirsysTurnClient("ident", "", "UniCircle", new ObjectMapper(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("together");
        for (String channel : new String[] { "../billing", "UniCircle?secret=other", "https://other.test" }) {
            assertThatThrownBy(() -> new XirsysTurnClient("ident", SECRET, channel, new ObjectMapper(), null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("channel name");
        }
    }

    @Test
    void quotaOrAuthenticationFailuresAreReportedWithoutLeakingProviderBodies() {
        for (int status : new int[] { 401, 403, 429, 500 }) {
            assertThatThrownBy(() -> client((uri, auth) -> new XirsysTurnClient.ProviderResponse(status, SECRET))
                    .iceServers(600))
                    .isInstanceOfSatisfying(ResponseStatusException.class, error -> {
                        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                        assertThat(error.getMessage()).contains("free allowance").doesNotContain(SECRET);
                    });
        }
    }

    @Test
    void rejectsMalformedOrNonRelayResponsesRatherThanSilentlyAdvertisingSuccess() {
        for (String body : new String[] { "not-json", "{\"s\":\"error\"}",
                "{\"s\":\"ok\",\"v\":{\"iceServers\":[]}}",
                VALID_RESPONSE.replace("temporary-password", SECRET),
                VALID_RESPONSE.replace("turn:", "https:").replace("turns:", "https:") }) {
            assertThatThrownBy(() -> client((uri, auth) -> new XirsysTurnClient.ProviderResponse(200, body))
                    .iceServers(600)).isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test
    void networkFailuresDoNotLeakSecretsOrTriggerAnUpgradeRequest() {
        assertThatThrownBy(() -> client((uri, auth) -> { throw new IOException(SECRET); }).iceServers(600))
                .isInstanceOf(ResponseStatusException.class).hasMessageNotContaining(SECRET);
    }

    @Test
    void controllerUsesProviderCredentialsInANonCachedResponse() {
        var provider = client((uri, auth) -> new XirsysTurnClient.ProviderResponse(200, VALID_RESPONSE));
        var controller = new CallConfigurationController("", "", "", "", "", 600, "relay",
                Clock.fixed(Instant.ofEpochSecond(1800000000), ZoneOffset.UTC), provider);
        var response = controller.configuration();
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody().relayConfigured()).isTrue();
        assertThat(response.getBody().iceTransportPolicy()).isEqualTo("relay");
        assertThat(response.getBody().expiresAtEpochSeconds()).isEqualTo(1800000600L);
        assertThat(response.getBody().iceServers().getFirst().username()).isEqualTo("temporary-user");
    }

    @Test
    void controllerRejectsMixedManualAndProviderSettings() {
        var provider = client((uri, auth) -> new XirsysTurnClient.ProviderResponse(200, VALID_RESPONSE));
        assertThatThrownBy(() -> new CallConfigurationController("", "turn:relay.test:3478", "user", "password", "",
                600, "all", Clock.systemUTC(), provider))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not both");
    }
}
