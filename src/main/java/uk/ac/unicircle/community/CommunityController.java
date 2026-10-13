package uk.ac.unicircle.community;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.unicircle.community.CommunityRequests.CreateCircleRequest;
import uk.ac.unicircle.community.CommunityResponses.CircleResponse;

@RestController
@RequestMapping("/api/community/circles")
class CommunityController {
    private final CommunityService community;

    CommunityController(CommunityService community) {
        this.community = community;
    }

    @GetMapping
    List<CircleResponse> circles(
            Authentication authentication,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "false") boolean joined) {
        return community.circles(authentication, query, category, joined);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CircleResponse create(Authentication authentication, @Valid @RequestBody CreateCircleRequest request) {
        return community.create(authentication, request);
    }

    @PostMapping("/{circleId}/join")
    CircleResponse join(Authentication authentication, @PathVariable UUID circleId) {
        return community.join(authentication, circleId);
    }

    @PostMapping("/{circleId}/leave")
    CircleResponse leave(Authentication authentication, @PathVariable UUID circleId) {
        return community.leave(authentication, circleId);
    }
}
