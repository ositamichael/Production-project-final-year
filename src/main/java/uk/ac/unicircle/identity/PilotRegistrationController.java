package uk.ac.unicircle.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pilot")
@Profile("pilot")
class PilotRegistrationController {

    private final PilotRegistrationService registration;

    PilotRegistrationController(PilotRegistrationService registration) {
        this.registration = registration;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    void register(@Valid @RequestBody RegistrationRequest request) {
        registration.register(request);
    }

    record RegistrationRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 2, max = 80) String displayName,
            @NotBlank @Size(max = 180) String courseName,
            @NotNull @Min(1) @Max(12) Integer yearOfStudy,
            @NotBlank @Size(min = 12, max = 128) String password,
            @NotBlank String inviteCode) {
    }
}
