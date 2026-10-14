package uk.ac.unicircle.admin;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

final class AdminResponses {
    private AdminResponses() {
    }

    record OverviewResponse(long users, long activeUsers, long pendingConnections,
            long upcomingEvents, long activeServices, long openSupportCases,
            long openSafetyReports, long pendingLostFoundReports, long publishedAnnouncements,
            long upcomingMeetings, long activeOpportunities, long activeCircles,
            long confirmedBookings, long programmes, long modules) {
    }

    record AnnouncementAdminResponse(UUID id, String title, String category, String audienceType,
            String audienceLabel, boolean pinned, boolean urgent, OffsetDateTime publishedAt,
            OffsetDateTime expiresAt) {
    }

    record ProgrammeAdminResponse(UUID id, String code, String title, String qualification,
            int moduleCount, boolean active) {
    }

    record ModuleAdminResponse(UUID id, String code, String title, int studyLevel,
            UUID programmeId, String programmeTitle, int yearOfStudy, boolean active) {
    }

    record CircleAdminResponse(UUID id, String name, String description, String category,
            int memberCount, boolean active, OffsetDateTime createdAt) {
    }

    record CatalogueRecordResponse(UUID id, String recordType, String title, String detail,
            String status, OffsetDateTime effectiveAt) {
    }

    record UserResponse(UUID id, String displayName, String email, String role, String status,
            String courseName, Integer yearOfStudy, OffsetDateTime createdAt) {
    }

    record EventAdminResponse(UUID id, String title, String category, String venue,
            OffsetDateTime startsAt, OffsetDateTime endsAt, int capacity, boolean published) {
    }

    record ServiceAdminResponse(UUID id, String serviceType, String title, String providerName,
            String location, boolean active) {
    }

    record SlotAdminResponse(UUID id, UUID serviceId, OffsetDateTime startsAt,
            OffsetDateTime endsAt, int capacity, boolean active) {
    }

    record MeetingAdminResponse(UUID id, String title, String moduleCode, String meetingType,
            OffsetDateTime startsAt, OffsetDateTime endsAt, String locationLabel,
            int assignedUsers, boolean published) {
    }

    record PublisherResponse(UUID id, String name, String type, String verificationStatus) {
    }

    record OpportunityAdminResponse(UUID id, String title, String opportunityType,
            String publisherName, LocalDate closesAt, String moderationStatus) {
    }

    record SupportAdminResponse(UUID id, String referenceCode, UUID reporterId,
            String reporterName, String category, String subject, String description,
            String status, String priority, String assignedTeam, OffsetDateTime updatedAt) {
    }

    record SafetyAdminResponse(UUID id, UUID reporterId, String reporterName,
            UUID reportedAccountId, String reportedAccountName, String reason, String details,
            String status, OffsetDateTime createdAt) {
    }

    record AuditResponse(UUID id, String actorName, String actionType, String resourceType,
            UUID resourceId, String summary, OffsetDateTime createdAt) {
    }
}
