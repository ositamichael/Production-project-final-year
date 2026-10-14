package uk.ac.unicircle.admin;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

final class AdminRequests {
    private AdminRequests() {
    }

    record UpdateUserRequest(
            @NotBlank @Pattern(regexp = "STUDENT|STAFF|SERVICE_PROVIDER|ADMIN") String role,
            @NotBlank @Pattern(regexp = "ACTIVE|SUSPENDED") String status) {
    }

    record CreateEventRequest(
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 600) String summary,
            @NotBlank @Pattern(regexp = "ACADEMIC|CAREERS|WELLBEING|SOCIAL") String category,
            @NotBlank @Size(max = 180) String venue,
            @NotNull @Future OffsetDateTime startsAt,
            @NotNull OffsetDateTime endsAt,
            @Min(1) @Max(10000) int capacity,
            boolean published) {
    }

    record CreateServiceRequest(
            @NotBlank @Pattern(regexp = "APPOINTMENT|FACILITY|CAMPUS") String serviceType,
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 180) String providerName,
            @NotBlank @Size(max = 600) String description,
            @NotBlank @Size(max = 180) String location) {
    }

    record CreateSlotRequest(
            @NotNull @Future OffsetDateTime startsAt,
            @NotNull OffsetDateTime endsAt,
            @Min(1) @Max(1000) int capacity) {
    }

    record CreateMeetingRequest(
            @NotBlank @Size(max = 220) String title,
            @Size(max = 40) String moduleCode,
            @NotBlank @Pattern(regexp = "LECTURE|SEMINAR|TUTORIAL|PROJECT|DROP_IN") String meetingType,
            @NotNull @Future OffsetDateTime startsAt,
            @NotNull OffsetDateTime endsAt,
            @NotBlank @Size(max = 180) String locationLabel,
            @Size(max = 3000) String agenda,
            @Size(max = 1000) String providerJoinUrl,
            List<UUID> assigneeIds,
            boolean published) {
    }

    record CreateOpportunityRequest(
            @NotNull UUID publisherId,
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 600) String summary,
            @NotBlank @Size(max = 2000) String description,
            @NotBlank @Pattern(regexp = "JOB|PLACEMENT|VOLUNTEERING") String opportunityType,
            @NotBlank @Pattern(regexp = "ON_CAMPUS|HYBRID|REMOTE|FLEXIBLE") String workMode,
            @NotBlank @Size(max = 180) String location,
            @NotBlank @Size(max = 120) String hoursLabel,
            @NotBlank @Size(max = 120) String compensationLabel,
            @NotBlank @Size(max = 600) String skillsText,
            @NotBlank @Size(max = 800) String eligibility,
            @NotNull LocalDate closesAt,
            boolean published) {
    }

    record UpdateSupportCaseRequest(
            @NotBlank @Pattern(regexp = "OPEN|IN_PROGRESS|WAITING_FOR_STUDENT|RESOLVED|CLOSED") String status,
            @NotBlank @Pattern(regexp = "LOW|NORMAL|HIGH|URGENT") String priority,
            @NotBlank @Size(max = 120) String assignedTeam,
            @Size(max = 4000) String reply) {
    }

    record UpdateSafetyReportRequest(
            @NotBlank @Pattern(regexp = "OPEN|REVIEWING|RESOLVED|DISMISSED") String status,
            @Size(max = 500) String note) {
    }
}
