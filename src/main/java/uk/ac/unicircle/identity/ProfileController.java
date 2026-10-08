package uk.ac.unicircle.identity;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile/me")
class ProfileController {

    private final ProfileService profiles;

    ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    ProfileResponse profile(Authentication authentication) {
        return profiles.currentProfile(authentication);
    }

    @PatchMapping
    ProfileResponse update(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        return profiles.updateProfile(authentication, request);
    }

    @PostMapping(path = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ProfileResponse> uploadPhoto(Authentication authentication,
            @RequestPart("photo") MultipartFile photo) {
        ProfileResponse updated = profiles.updatePhoto(authentication, photo);
        return ResponseEntity.created(URI.create(updated.photoUrl())).body(updated);
    }

    @GetMapping("/photo")
    ResponseEntity<byte[]> photo(Authentication authentication) {
        ProfileService.ProfilePhoto photo = profiles.photo(authentication);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(photo.mediaType()))
                .body(photo.content());
    }
}

