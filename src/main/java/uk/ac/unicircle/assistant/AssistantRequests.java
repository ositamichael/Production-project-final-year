package uk.ac.unicircle.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

final class AssistantRequests {
    private AssistantRequests() {
    }

    record AskRequest(
            @NotBlank @Size(max = 4_000) String message,
            @NotNull AssistantMode mode) {
    }
}
