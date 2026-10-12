package uk.ac.unicircle.information;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

final class InformationResponses {

    private InformationResponses() {
    }

    record AnnouncementResponse(
            UUID id,
            String authorName,
            String authorRole,
            String category,
            String audienceType,
            String audienceLabel,
            String title,
            String body,
            String actionLabel,
            String actionUrl,
            boolean pinned,
            boolean urgent,
            boolean read,
            OffsetDateTime publishedAt) {
    }

    record SupportCaseResponse(
            UUID id,
            String referenceCode,
            String category,
            String subject,
            String description,
            String status,
            String priority,
            String assignedTeam,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            List<SupportReplyResponse> replies,
            String deliveryMode) {
    }

    record SupportReplyResponse(
            UUID id,
            String authorName,
            String authorRole,
            String body,
            OffsetDateTime createdAt) {
    }

    record MeetingSummaryResponse(
            UUID id,
            String hostName,
            String title,
            String moduleCode,
            String meetingType,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String locationLabel,
            String agenda,
            boolean joined,
            boolean handRaised,
            int attendeeCount,
            boolean videoProviderConfigured) {
    }

    record MeetingRoomResponse(
            MeetingSummaryResponse meeting,
            List<MeetingParticipantResponse> participants,
            List<MeetingMessageResponse> messages,
            String providerJoinUrl,
            String providerStatus) {
    }

    record MeetingParticipantResponse(
            UUID accountId,
            String displayName,
            String role,
            boolean present,
            boolean handRaised,
            OffsetDateTime joinedAt) {
    }

    record MeetingMessageResponse(
            UUID id,
            UUID senderId,
            String senderName,
            String senderRole,
            String messageType,
            String body,
            OffsetDateTime sentAt) {
    }
}
