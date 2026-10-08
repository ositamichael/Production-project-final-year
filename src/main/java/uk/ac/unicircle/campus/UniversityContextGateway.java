package uk.ac.unicircle.campus;

import java.util.UUID;

interface UniversityContextGateway {
    UniversityContextResponse load(UUID accountId, UUID universityId);
}
