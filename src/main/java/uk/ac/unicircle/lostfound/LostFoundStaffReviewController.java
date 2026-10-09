package uk.ac.unicircle.lostfound;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.unicircle.lostfound.LostFoundStaffReviewService.PendingReport;
import uk.ac.unicircle.lostfound.LostFoundStaffReviewService.ReviewResult;

@RestController
@RequestMapping("/api/lost-found/staff")
class LostFoundStaffReviewController {

    private final LostFoundStaffReviewService reviews;

    LostFoundStaffReviewController(LostFoundStaffReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping("/reports")
    ResponseEntity<List<PendingReport>> pending(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reviews.pending(authentication));
    }

    @PostMapping("/reports/{reportId}/decision")
    ReviewResult decide(Authentication authentication, @PathVariable UUID reportId,
            @Valid @RequestBody DecisionRequest request) {
        return reviews.decide(authentication, reportId, request.decision(), request.reason());
    }

    @GetMapping("/reports/{reportId}/photo")
    ResponseEntity<byte[]> photo(Authentication authentication, @PathVariable UUID reportId) {
        var photo = reviews.photo(authentication, reportId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(photo.mediaType()))
                .body(photo.content());
    }

    record DecisionRequest(@NotBlank String decision, @NotBlank @Size(min = 20, max = 500) String reason) {
    }
}
