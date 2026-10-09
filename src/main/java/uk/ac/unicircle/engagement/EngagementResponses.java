package uk.ac.unicircle.engagement;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

final class EngagementResponses {

    private EngagementResponses() {
    }

    record EventResponse(
            UUID id,
            String title,
            String summary,
            String category,
            String venue,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            int capacity,
            long placesRemaining,
            boolean registered) {
    }

    record ServiceOfferingResponse(
            UUID id,
            String type,
            String title,
            String providerName,
            String description,
            String location,
            List<ServiceSlotResponse> slots) {
    }

    record ServiceSlotResponse(
            UUID id,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            int capacity,
            long placesRemaining,
            boolean booked) {
    }

    record BookingResponse(
            UUID bookingId,
            UUID slotId,
            String serviceTitle,
            String providerName,
            String location,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String status) {
    }

    record ScheduleItemResponse(
            UUID resourceId,
            String resourceType,
            String title,
            String location,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String status) {
    }
}
