package uk.ac.unicircle.identity;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
class ProfileService {

    static final String DEMO_SUBJECT = "demo-student-c3707780";

    private final UserAccountRepository accounts;
    private final ProfilePhotoStorage photoStorage;
    private final CurrentIdentity currentIdentity;

    ProfileService(UserAccountRepository accounts, ProfilePhotoStorage photoStorage, CurrentIdentity currentIdentity) {
        this.accounts = accounts;
        this.photoStorage = photoStorage;
        this.currentIdentity = currentIdentity;
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
        return accounts.findById(currentIdentity.require(authentication).accountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The signed-in account is unavailable."));
    }

    record ProfilePhoto(byte[] content, String mediaType) {
    }
}
