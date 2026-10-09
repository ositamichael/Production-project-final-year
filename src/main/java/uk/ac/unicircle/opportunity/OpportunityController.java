package uk.ac.unicircle.opportunity;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.unicircle.opportunity.OpportunityResponses.OpportunityResponse;

@RestController
@RequestMapping("/api/opportunities")
class OpportunityController {

    private final OpportunityService opportunities;

    OpportunityController(OpportunityService opportunities) {
        this.opportunities = opportunities;
    }

    @GetMapping
    List<OpportunityResponse> catalogue(
            Authentication authentication,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String query) {
        return opportunities.catalogue(authentication, type, query);
    }

    @GetMapping("/tracker")
    List<OpportunityResponse> tracker(Authentication authentication) {
        return opportunities.tracker(authentication);
    }

    @PostMapping("/{opportunityId}/save")
    OpportunityResponse save(Authentication authentication, @PathVariable UUID opportunityId) {
        return opportunities.save(authentication, opportunityId);
    }

    @PostMapping("/{opportunityId}/unsave")
    OpportunityResponse unsave(Authentication authentication, @PathVariable UUID opportunityId) {
        return opportunities.unsave(authentication, opportunityId);
    }

    @PostMapping("/{opportunityId}/track")
    OpportunityResponse track(
            Authentication authentication,
            @PathVariable UUID opportunityId,
            @RequestBody TrackRequest request) {
        return opportunities.track(authentication, opportunityId, request.status());
    }

    record TrackRequest(String status) {
    }
}
