package uk.ac.unicircle.identity;

import java.util.UUID;

record ProfileResponse(
        UUID id,
        String university,
        String email,
        String displayName,
        UserRole role,
        ProfileVisibility visibility,
        String biography,
        String courseName,
        Integer yearOfStudy,
        boolean hasPhoto,
        String photoUrl) {

    static ProfileResponse from(UserAccount account) {
        boolean hasPhoto = account.profilePhotoKey() != null;
        return new ProfileResponse(
                account.id(),
                account.university().displayName(),
                account.email(),
                account.displayName(),
                account.role(),
                account.profileVisibility(),
                account.biography(),
                account.courseName(),
                account.yearOfStudy(),
                hasPhoto,
                hasPhoto ? "/api/profile/me/photo" : null);
    }
}

