package uk.ac.unicircle.platform;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class PlatformStatusService {

    private final Clock clock;

    public PlatformStatusService() {
        this(Clock.systemUTC());
    }

    PlatformStatusService(Clock clock) {
        this.clock = clock;
    }

    public PlatformStatus currentStatus() {
        return new PlatformStatus(
                "UniCircle",
                "Sprint 1 engineering foundation",
                "Prototype fixtures only — no live university systems connected",
                List.of(
                        "Multi-university data boundary",
                        "OIDC university sign-in configuration",
                        "Real-time messaging foundation",
                        "Versioned database migrations",
                        "Health monitoring"),
                Instant.now(clock));
    }
}
