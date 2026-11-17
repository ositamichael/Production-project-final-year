package uk.ac.unicircle.campus;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import uk.ac.unicircle.identity.CurrentIdentity;

@Service
class CampusContextService {

    private final CurrentIdentity currentIdentity;
    private final UniversityContextGateway universityContext;

    CampusContextService(CurrentIdentity currentIdentity, UniversityContextGateway universityContext) {
        this.currentIdentity = currentIdentity;
        this.universityContext = universityContext;
    }

    UniversityContextResponse current(Authentication authentication) {
        var account = currentIdentity.require(authentication);
        return universityContext.load(account.accountId(), account.universityId());
    }
}
