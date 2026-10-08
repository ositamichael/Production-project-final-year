package uk.ac.unicircle.identity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record UpdateProfileRequest(
        @NotBlank @Size(max = 160) String displayName,
        @Size(max = 500) String biography,
        @Size(max = 180) String courseName,
        @Min(1) @Max(12) Integer yearOfStudy,
        @NotNull ProfileVisibility visibility) {
}

