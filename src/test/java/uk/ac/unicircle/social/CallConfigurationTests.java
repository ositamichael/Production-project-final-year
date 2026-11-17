package uk.ac.unicircle.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class CallConfigurationTests {
    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1800000000), ZoneOffset.UTC);

    private CallConfigurationController configuration(String urls, String username, String credential,
            String secret, long ttl, String policy) {
        return new CallConfigurationController("stun:stun.example.test:3478", urls,
                username, credential, secret, ttl, policy, CLOCK);
    }

    @Test
    void issuesExpiringCoturnCredentialsWithoutReturningTheSharedSecret() throws Exception {
        String secret = "fictional-test-secret-not-a-live-credential";
        var controller = configuration("turn:relay.example.test:3478?transport=udp,turns:relay.example.test:5349?transport=tcp",
                "", "", secret, 600, "all");
        var response = controller.configuration();
        var body = response.getBody();
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(body.relayConfigured()).isTrue();
        assertThat(body.expiresAtEpochSeconds()).isEqualTo(1800000600L);
        var server = body.iceServers().get(1);
        assertThat(server.urls()).hasSize(2);
        assertThat(server.username()).startsWith("1800000600:");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        assertThat(server.credential()).isEqualTo(Base64.getEncoder().encodeToString(
                mac.doFinal(server.username().getBytes(StandardCharsets.UTF_8))));
        assertThat(body.toString()).doesNotContain(secret);
        assertThat(controller.configuration().getBody().iceServers().get(1).username())
                .isNotEqualTo(server.username());
    }

    @Test
    void keepsCompatibilityWithProviderSuppliedUsernameAndCredential() {
        var body = configuration("turns:relay.example.test:443?transport=tcp", "test-user", "test-password",
                "", 600, "relay").configuration().getBody();
        assertThat(body.iceTransportPolicy()).isEqualTo("relay");
        assertThat(body.relayConfigured()).isTrue();
        assertThat(body.iceServers().get(1).username()).isEqualTo("test-user");
        assertThat(body.iceServers().get(1).credential()).isEqualTo("test-password");
        assertThat(body.expiresAtEpochSeconds()).isZero();
    }

    @Test
    void incompleteCredentialsDoNotAdvertiseAConfiguredRelay() {
        var body = configuration("turn:relay.example.test:3478", "test-user", "", "", 600, "all")
                .configuration().getBody();
        assertThat(body.relayConfigured()).isFalse();
        assertThat(body.iceServers()).hasSize(1);
    }

    @Test
    void refusesRelayOnlyModeWithoutCredentials() {
        assertThatThrownBy(() -> configuration("", "", "", "", 600, "relay"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Relay-only");
    }

    @Test
    void refusesAmbiguousCredentialsAndInvalidEndpointOrPolicy() {
        assertThatThrownBy(() -> configuration("turn:r.test:3478", "user", "password", "secret", 600, "all"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not both");
        assertThatThrownBy(() -> configuration("https://r.test", "user", "password", "", 600, "all"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("TURN URLs");
        assertThatThrownBy(() -> configuration("", "", "", "", 600, "anything"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("transport policy");
    }

    @Test
    void refusesUnsafeCredentialLifetimes() {
        for (long ttl : new long[] { 0, 59, 86401 }) {
            assertThatThrownBy(() -> configuration("", "", "", "", ttl, "all"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("lifetime");
        }
    }
}
