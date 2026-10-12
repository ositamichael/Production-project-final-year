package uk.ac.unicircle.evaluation;

import java.time.OffsetDateTime;
import java.util.List;

final class PilotTestResponses {

    private PilotTestResponses() {
    }

    record PilotScenarioResponse(
            String code,
            String area,
            String title,
            String instruction,
            String expectedResult,
            String status,
            String evidenceNote,
            String deviceLabel,
            OffsetDateTime testedAt) {
    }

    record PilotTestCentreResponse(
            int total,
            int passed,
            int failed,
            int blocked,
            int notRun,
            List<PilotScenarioResponse> scenarios) {
    }
}
