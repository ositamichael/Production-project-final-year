package uk.ac.unicircle.location;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.unicircle.location.LocationSharingService.FriendLocationResponse;
import uk.ac.unicircle.location.LocationSharingService.MyLocationResponse;

@RestController
@RequestMapping("/api/location")
@Profile("pilot")
class LocationSharingController {

    private final LocationSharingService locations;

    LocationSharingController(LocationSharingService locations) {
        this.locations = locations;
    }

    @GetMapping("/me")
    MyLocationResponse mine(Authentication authentication) {
        return locations.mine(authentication);
    }

    @PostMapping("/me")
    MyLocationResponse share(Authentication authentication, @Valid @RequestBody ShareLocationRequest request) {
        return locations.share(authentication, request.latitude(), request.longitude(), request.accuracyMetres());
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void stop(Authentication authentication) {
        locations.stop(authentication);
    }

    @GetMapping("/connections")
    List<FriendLocationResponse> connections(Authentication authentication) {
        return locations.visibleFriends(authentication);
    }

    record ShareLocationRequest(@NotNull Double latitude, @NotNull Double longitude,
            @NotNull Integer accuracyMetres) {
    }
}
