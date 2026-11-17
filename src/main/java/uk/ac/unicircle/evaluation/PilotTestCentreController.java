package uk.ac.unicircle.evaluation;

import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.unicircle.evaluation.PilotTestResponses.PilotTestCentreResponse;

@RestController
@Profile("pilot")
@RequestMapping("/api/pilot-tests")
class PilotTestCentreController {

    private final PilotTestCentreService tests;

    PilotTestCentreController(PilotTestCentreService tests) {
        this.tests = tests;
    }

    @GetMapping
    PilotTestCentreResponse testCentre(Authentication authentication) {
        return tests.testCentre(authentication);
    }

    @PutMapping("/{scenarioCode}")
    PilotTestCentreResponse record(Authentication authentication,
            @PathVariable String scenarioCode,
            @Valid @RequestBody RecordPilotResultRequest request) {
        return tests.record(authentication, scenarioCode, request);
    }
}
