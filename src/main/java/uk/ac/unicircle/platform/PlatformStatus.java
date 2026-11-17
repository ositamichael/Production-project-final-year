package uk.ac.unicircle.platform;

import java.time.Instant;
import java.util.List;

public record PlatformStatus(
        String application,
        String stage,
        String dataStatus,
        List<String> enabledFoundations,
        Instant generatedAt) {
}
