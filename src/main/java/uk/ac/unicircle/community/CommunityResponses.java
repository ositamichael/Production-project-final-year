package uk.ac.unicircle.community;

import java.time.OffsetDateTime;
import java.util.UUID;

final class CommunityResponses {
    private CommunityResponses() {
    }

    record CircleResponse(
            UUID id,
            String name,
            String description,
            CommunityCategory category,
            long memberCount,
            boolean joined,
            boolean owner,
            OffsetDateTime createdAt) {
    }
}
