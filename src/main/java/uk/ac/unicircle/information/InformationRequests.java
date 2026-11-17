package uk.ac.unicircle.information;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

final class InformationRequests {

    private InformationRequests() {
    }

    record CreateSupportCaseRequest(
            @NotBlank @Pattern(regexp = "ACADEMIC|IT|FINANCE|ACCOMMODATION|WELLBEING|ACCESSIBILITY|OTHER")
            String category,
            @NotBlank @Size(max = 220) String subject,
            @NotBlank @Size(max = 5000) String description) {
    }

    record AddSupportReplyRequest(@NotBlank @Size(max = 4000) String body) {
    }

    record PublishAnnouncementRequest(
            @NotBlank @Pattern(regexp = "UNIVERSITY|MODULE|PLACEMENT|INTERNSHIP|CAREERS|EVENT|SOCIETY|URGENT")
            String category,
            @NotBlank @Pattern(regexp = "UNIVERSITY|COURSE|YEAR|MODULE") String audienceType,
            @Size(max = 180) String audienceLabel,
            @NotBlank @Size(max = 220) String title,
            @NotBlank @Size(max = 5000) String body,
            @Size(max = 100) String actionLabel,
            @Pattern(regexp = "^#[A-Za-z0-9-]+$") String actionUrl,
            boolean pinned,
            boolean urgent,
            OffsetDateTime expiresAt) {
    }

    record MeetingMessageRequest(
            @NotBlank @Pattern(regexp = "CHAT|QUESTION") String messageType,
            @NotBlank @Size(max = 2000) String body) {
    }

    record RaiseHandRequest(boolean raised) {
    }
}
