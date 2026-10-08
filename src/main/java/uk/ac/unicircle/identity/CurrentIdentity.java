package uk.ac.unicircle.identity;

import java.util.UUID;

import org.springframework.security.core.Authentication;

/**
 * Public identity boundary used by other UniCircle modules. Browser-supplied
 * university or account identifiers are never trusted.
 */
public interface CurrentIdentity {

    SignedInAccount require(Authentication authentication);

    record SignedInAccount(UUID accountId, UUID universityId) {
    }
}
