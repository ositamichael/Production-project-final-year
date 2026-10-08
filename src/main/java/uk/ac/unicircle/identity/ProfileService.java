package uk.ac.unicircle.identity;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
class ProfileService {

    static final String DEMO_SUBJECT = "demo-student-c3707780";

    private final UserAccountRepository accounts;
    private final ProfilePhotoStorage photoStorage;

    ProfileService(UserAccountRepository accounts, ProfilePhotoStorage photoStorage) {
        this.accounts = accounts;
        this.photoStorage = photoStorage;
    }

    @Transactional(readOnly = true)
    ProfileResponse currentProfile(Authentication authentication) {
        return ProfileResponse.from(resolveAccount(authentication));
    }

    @Transactional
    ProfileResponse updateProfile(Authentication authentication, UpdateProfileRequest request) {
        UserAccount account = resolveAccount(authentication);
        account.updateProfile(request.displayName(), request.biography(), request.courseName(),
                request.yearOfStudy(), request.visibility());
        return ProfileResponse.from(account);
    }

    @Transactional
    ProfileResponse updatePhoto(Authentication authentication, MultipartFile photo) {
        UserAccount account = resolveAccount(authentication);
        ProfilePhotoStorage.StoredPhoto stored;
        try {
            stored = photoStorage.store(photo);
        } catch (InvalidProfilePhotoException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "The photograph could not be processed.", exception);
        }

        String previousKey = account.replacePhoto(stored.key(), stored.mediaType());
        try {
            photoStorage.delete(previousKey);
        } catch (IOException ignored) {
            // The new photograph remains valid. Orphan cleanup is an operational concern.
        }
        return ProfileResponse.from(account);
    }

    @Transactional(readOnly = true)
    ProfilePhoto photo(Authentication authentication) {
        UserAccount account = resolveAccount(authentication);
        if (account.profilePhotoKey() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No profile photograph has been uploaded.");
        }
        try {
            return new ProfilePhoto(photoStorage.read(account.profilePhotoKey()), account.profilePhotoMediaType());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The profile photograph is unavailable.", exception);
        }
    }

    private UserAccount resolveAccount(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            var issuer = oidcUser.getIdToken().getClaimAsURL("iss");
            if (issuer != null) {
                return accounts.findByUniversityOidcIssuerAndExternalSubject(issuer.toString(), authentication.getName())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "The authenticated university account has not been provisioned."));
            }
        }
        return accounts.findByExternalSubject(DEMO_SUBJECT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The local demonstration profile is unavailable."));
    }

    record ProfilePhoto(byte[] content, String mediaType) {
    }
}

