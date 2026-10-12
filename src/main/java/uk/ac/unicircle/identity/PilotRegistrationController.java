package uk.ac.unicircle.identity;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<RegistrationError> invalidDetails(MethodArgumentNotValidException error) {
        Map<String, String> fields = new LinkedHashMap<>();
        error.getBindingResult().getFieldErrors().forEach(field ->
                fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new RegistrationError(
                "VALIDATION_ERROR", "Check the highlighted registration details.", fields));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    ResponseEntity<RegistrationError> rejectedRegistration(
            org.springframework.web.server.ResponseStatusException error) {
        int status = error.getStatusCode().value();
        String code = status == HttpStatus.CONFLICT.value() ? "EMAIL_EXISTS" : "REGISTRATION_REJECTED";
        String message = status == HttpStatus.CONFLICT.value()
                ? "This email already has a staging account."
                : "The registration could not be completed.";
        return ResponseEntity.status(error.getStatusCode())
                .body(new RegistrationError(code, message, Map.of()));
    }

    record RegistrationRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 2, max = 80) String displayName,
            @NotBlank @Size(max = 180) String courseName,
            @NotNull @Min(1) @Max(12) Integer yearOfStudy,
            @NotBlank @Size(min = 12, max = 128) String password) {
    }

    record RegistrationError(String code, String message, Map<String, String> fields) {
    }
}
