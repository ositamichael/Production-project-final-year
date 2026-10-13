package uk.ac.unicircle.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

final class CommunityRequests {
    private CommunityRequests() {
    }

    record CreateCircleRequest(
            @NotBlank @Size(min = 3, max = 120) String name,
            @NotBlank @Size(min = 10, max = 600) String description,
            @NotNull CommunityCategory category) {
    }
}
