package uk.ac.unicircle.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class PlatformStatusServiceTests {

    @Test
    void reportsFoundationWithoutClaimingLiveIntegrations() {
        Instant fixedInstant = Instant.parse("2026-10-08T12:00:00Z");
        PlatformStatusService service = new PlatformStatusService(Clock.fixed(fixedInstant, ZoneOffset.UTC));

        PlatformStatus status = service.currentStatus();

        assertThat(status.application()).isEqualTo("UniCircle");
        assertThat(status.dataStatus()).contains("Prototype fixtures only");
        assertThat(status.enabledFoundations()).contains("Multi-university data boundary");
        assertThat(status.generatedAt()).isEqualTo(fixedInstant);
    }
}
