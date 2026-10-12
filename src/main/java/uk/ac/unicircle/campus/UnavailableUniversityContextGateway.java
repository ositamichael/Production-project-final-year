package uk.ac.unicircle.campus;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Profile("prod | pilot")
class UnavailableUniversityContextGateway implements UniversityContextGateway {

    @Override
    public UniversityContextResponse load(UUID accountId, UUID universityId) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No approved university academic-data connector is configured.");
    }
}
