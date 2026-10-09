package uk.ac.unicircle.lostfound;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

final class LostFoundRequests {

    private LostFoundRequests() {
    }

    record CreateReportRequest(
            @NotBlank @Size(max = 20) String reportType,
            @NotBlank @Size(max = 30) String category,
            @NotBlank @Size(max = 160) String itemName,
            @Size(max = 80) String colour,
            @NotBlank @Size(max = 800) String publicDescription,
            @NotBlank @Size(max = 1000) String privateIdentifyingDetails,
            @NotBlank @Size(max = 180) String lastSeenLocation,
            @NotNull @PastOrPresent LocalDate occurredOn,
            @NotBlank @Size(max = 30) String custodyStatus) {
    }

    record SubmitClaimRequest(
            @NotBlank @Size(min = 20, max = 1000) String privateOwnershipEvidence) {
    }
}
