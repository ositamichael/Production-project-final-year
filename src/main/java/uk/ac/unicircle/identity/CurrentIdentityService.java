package uk.ac.unicircle.identity;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class CurrentIdentityService implements CurrentIdentity {

    private final UserAccountRepository accounts;
    private final Environment environment;

    CurrentIdentityService(UserAccountRepository accounts, Environment environment) {
        this.accounts = accounts;
        this.environment = environment;
    }

    @Override
    @Transactional(readOnly = true)
    public SignedInAccount require(Authentication authentication) {
        UserAccount account;
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            var issuer = oidcUser.getIdToken().getClaimAsURL("iss");
            if (issuer == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "The university identity did not include an issuer.");
            }
            account = accounts.findByUniversityOidcIssuerAndExternalSubject(
                            issuer.toString(), authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "The authenticated university account has not been provisioned."));
        } else if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            account = accounts.findByExternalSubject(ProfileService.DEMO_SUBJECT)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "The local demonstration profile is unavailable."));
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "A verified university sign-in is required.");
        }
        return new SignedInAccount(account.id(), account.university().id());
    }
}
