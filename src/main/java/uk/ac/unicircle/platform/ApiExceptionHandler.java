package uk.ac.unicircle.platform;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validationFailure(MethodArgumentNotValidException error) {
        Map<String, String> fields = new LinkedHashMap<>();
        error.getBindingResult().getFieldErrors().forEach(field ->
                fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Check the submitted details.", fields);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> rejectedRequest(ResponseStatusException error) {
        String detail = error.getReason() == null
                ? "The request could not be completed."
                : error.getReason();
        return response(error.getStatusCode(), codeFor(error.getStatusCode()), detail, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accessDenied() {
        return response(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "This account does not have permission to perform that action.", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflictingData() {
        return response(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "The requested change conflicts with an existing record.", Map.of());
    }

    private ResponseEntity<ApiError> response(HttpStatusCode status, String code,
            String detail, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(
                OffsetDateTime.now(ZoneOffset.UTC), status.value(), code, detail, fields));
    }

    private String codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "BAD_REQUEST";
            case 403 -> "ACCESS_DENIED";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            default -> "REQUEST_REJECTED";
        };
    }

    record ApiError(OffsetDateTime timestamp, int status, String code,
            String detail, Map<String, String> fields) {
    }
}
