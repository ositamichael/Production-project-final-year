package uk.ac.unicircle.identity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_accounts")
class UserAccount {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(name = "external_subject", nullable = false, length = 255)
    private String externalSubject;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility", nullable = false, length = 30)
    private ProfileVisibility profileVisibility;

    @Column(length = 500)
    private String biography;

    @Column(name = "course_name", length = 180)
    private String courseName;

    @Column(name = "year_of_study")
    private Integer yearOfStudy;

    @Column(name = "profile_photo_key", length = 255)
    private String profilePhotoKey;

    @Column(name = "profile_photo_media_type", length = 80)
    private String profilePhotoMediaType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccount() {
    }

    UserAccount(UUID id, University university, String externalSubject, String email,
            String displayName, UserRole role) {
        this.id = id;
        this.university = university;
        this.externalSubject = externalSubject;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.profileVisibility = ProfileVisibility.UNIVERSITY_ONLY;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    void updateProfile(String displayName, String biography, String courseName,
            Integer yearOfStudy, ProfileVisibility visibility) {
        this.displayName = displayName.strip();
        this.biography = blankToNull(biography);
        this.courseName = blankToNull(courseName);
        this.yearOfStudy = yearOfStudy;
        this.profileVisibility = visibility;
        this.updatedAt = Instant.now();
    }

    String replacePhoto(String key, String mediaType) {
        String previousKey = profilePhotoKey;
        profilePhotoKey = key;
        profilePhotoMediaType = mediaType;
        updatedAt = Instant.now();
        return previousKey;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    UUID id() { return id; }
    University university() { return university; }
    String externalSubject() { return externalSubject; }
    String email() { return email; }
    String displayName() { return displayName; }
    UserRole role() { return role; }
    ProfileVisibility profileVisibility() { return profileVisibility; }
    String biography() { return biography; }
    String courseName() { return courseName; }
    Integer yearOfStudy() { return yearOfStudy; }
    String profilePhotoKey() { return profilePhotoKey; }
    String profilePhotoMediaType() { return profilePhotoMediaType; }
}

