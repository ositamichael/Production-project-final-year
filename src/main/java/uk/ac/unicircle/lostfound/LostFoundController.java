package uk.ac.unicircle.lostfound;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import uk.ac.unicircle.lostfound.LostFoundRequests.CreateReportRequest;
import uk.ac.unicircle.lostfound.LostFoundRequests.SubmitClaimRequest;
import uk.ac.unicircle.lostfound.LostFoundResponses.ClaimResponse;
import uk.ac.unicircle.lostfound.LostFoundResponses.ReportResponse;

@RestController
@RequestMapping("/api/lost-found")
class LostFoundController {

    private final LostFoundService lostFound;

    LostFoundController(LostFoundService lostFound) {
        this.lostFound = lostFound;
    }

    @GetMapping
    List<ReportResponse> catalogue(
            Authentication authentication,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String query) {
        return lostFound.catalogue(authentication, type, category, query);
    }

    @GetMapping("/mine")
    List<ReportResponse> myReports(Authentication authentication) {
        return lostFound.myReports(authentication);
    }

    @GetMapping("/claims")
    List<ClaimResponse> myClaims(Authentication authentication) {
        return lostFound.myClaims(authentication);
    }

    @PostMapping
    ResponseEntity<ReportResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateReportRequest request) {
        ReportResponse created = lostFound.create(authentication, request);
        return ResponseEntity.created(URI.create("/api/lost-found/" + created.id())).body(created);
    }

    @PostMapping(path = "/{reportId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ReportResponse uploadPhoto(
            Authentication authentication,
            @PathVariable UUID reportId,
            @RequestPart("photo") MultipartFile photo) {
        return lostFound.uploadPhoto(authentication, reportId, photo);
    }

    @GetMapping("/{reportId}/photo")
    ResponseEntity<byte[]> photo(Authentication authentication, @PathVariable UUID reportId) {
        LostFoundService.StoredItemPhoto photo = lostFound.photo(authentication, reportId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(photo.mediaType()))
                .body(photo.content());
    }

    @PostMapping("/{reportId}/claims")
    ResponseEntity<ClaimResponse> claim(
            Authentication authentication,
            @PathVariable UUID reportId,
            @Valid @RequestBody SubmitClaimRequest request) {
        ClaimResponse claim = lostFound.claim(authentication, reportId, request);
        return ResponseEntity.created(URI.create("/api/lost-found/claims/" + claim.id())).body(claim);
    }

    @PostMapping("/{reportId}/withdraw")
    ReportResponse withdraw(Authentication authentication, @PathVariable UUID reportId) {
        return lostFound.withdraw(authentication, reportId);
    }
}
