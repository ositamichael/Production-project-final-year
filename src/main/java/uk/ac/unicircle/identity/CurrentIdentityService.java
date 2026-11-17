package uk.ac.unicircle.identity;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class CurrentIdentityService implements CurrentIdentity {

    private final UserAccountRepository accounts;
    private final Environment environment;
    private final JdbcTemplate jdbc;

    CurrentIdentityService(UserAccountRepository accounts, Environment environment, JdbcTemplate jdbc) {
        this.accounts = accounts;
        this.environment = environment;
        this.jdbc = jdbc;
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
        } else if (environment.acceptsProfiles(Profiles.of("pilot"))) {
            String authenticatedName = authentication == null ? "" : authentication.getName();
            account = accounts.findByEmailIgnoreCase(authenticatedName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "The pilot account is unavailable."));
        } else if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            String authenticatedName = authentication == null ? "" : authentication.getName();
            account = accounts.findByEmailIgnoreCase(authenticatedName)
                    .or(() -> accounts.findByExternalSubject(ProfileService.DEMO_SUBJECT))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "The local demonstration profile is unavailable."));
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "A verified university sign-in is required.");
        }
        String status = jdbc.queryForObject(
                "SELECT account_status FROM user_accounts WHERE id = ?",
                String.class, account.id());
        if (!"ACTIVE".equals(status)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This Test Campus account is suspended. Contact an authorised administrator.");
        }
        return new SignedInAccount(account.id(), account.university().id());
    }
}
