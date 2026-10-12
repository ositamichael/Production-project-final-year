package uk.ac.unicircle.identity;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
class PublicProfilePhotoController {

    private final ProfileService profiles;

    PublicProfilePhotoController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/{accountId}/photo")
    ResponseEntity<byte[]> photo(Authentication authentication, @PathVariable UUID accountId) {
        ProfileService.ProfilePhoto photo = profiles.photoFor(authentication, accountId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(photo.mediaType()))
                .body(photo.content());
    }
}
