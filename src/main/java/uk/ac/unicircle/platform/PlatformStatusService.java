package uk.ac.unicircle.platform;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

@Service
public class PlatformStatusService {

    private final Clock clock;
    private final Environment environment;

    @Autowired
    public PlatformStatusService(Environment environment) {
        this.clock = Clock.systemUTC();
        this.environment = environment;
    }

    PlatformStatusService(Clock clock) {
        this.clock = clock;
        this.environment = null;
    }

    public PlatformStatus currentStatus() {
        boolean pilot = environment != null && environment.acceptsProfiles(Profiles.of("pilot"));
        return new PlatformStatus(
                "UniCircle",
                pilot ? "UniCircle Staging Campus" : "Sprint 10 engineering prototype",
                pilot ? "STAGING — real invited accounts with fictional campus records; no university identity or official campus data connected"
                        : "Prototype fixtures only — no live university systems connected",
                List.of(
                        "Multi-university data boundary",
                        "OIDC university sign-in configuration",
                        "Real-time messaging foundation",
                        "Versioned database migrations",
                        "Health monitoring"),
                Instant.now(clock));
    }
}
