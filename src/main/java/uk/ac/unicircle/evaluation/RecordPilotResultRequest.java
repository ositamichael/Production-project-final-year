package uk.ac.unicircle.evaluation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record RecordPilotResultRequest(
        @NotBlank
        @Pattern(regexp = "PASS|FAIL|BLOCKED|NOT_RUN")
        String status,

        @Size(max = 2000)
        String evidenceNote,

        @Size(max = 180)
        String deviceLabel) {
}
