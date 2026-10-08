package uk.ac.unicircle.identity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "universities")
class University {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(name = "display_name", nullable = false, length = 180)
    private String displayName;

    @Column(name = "email_domain", nullable = false, unique = true, length = 180)
    private String emailDomain;

    @Column(name = "oidc_issuer", length = 500)
    private String oidcIssuer;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected University() {
    }

    University(UUID id, String slug, String displayName, String emailDomain, String oidcIssuer) {
        this.id = id;
        this.slug = slug;
        this.displayName = displayName;
        this.emailDomain = emailDomain;
        this.oidcIssuer = oidcIssuer;
        this.active = true;
        this.createdAt = Instant.now();
    }

    UUID id() {
        return id;
    }

    String displayName() {
        return displayName;
    }

    String oidcIssuer() {
        return oidcIssuer;
    }
}

