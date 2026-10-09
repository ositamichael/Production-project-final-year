package uk.ac.unicircle.opportunity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

final class OpportunityResponses {

    private OpportunityResponses() {
    }

    record OpportunityResponse(
            UUID id,
            String title,
            String summary,
            String description,
            String type,
            String workMode,
            String location,
            String hoursLabel,
            String compensationLabel,
            LocalDate closesAt,
            List<String> skills,
            String eligibility,
            String publisherName,
            String publisherType,
            boolean publisherVerified,
            boolean saved,
            String trackingStatus) {
    }
}
