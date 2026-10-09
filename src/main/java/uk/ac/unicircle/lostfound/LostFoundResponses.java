package uk.ac.unicircle.lostfound;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

final class LostFoundResponses {

    private LostFoundResponses() {
    }

    record ReportResponse(
            UUID id,
            String reportType,
            String category,
            String itemName,
            String colour,
            String publicDescription,
            String lastSeenLocation,
            LocalDate occurredOn,
            String custodyStatus,
            String reportStatus,
            String moderationStatus,
            boolean hasPhoto,
            String photoUrl,
            boolean mine,
            String myClaimStatus,
            String myReviewNote,
            Instant createdAt) {
    }

    record ClaimResponse(
            UUID id,
            UUID reportId,
            String itemName,
            String claimStatus,
            Instant createdAt) {
    }
}
