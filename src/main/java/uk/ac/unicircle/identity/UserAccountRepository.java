package uk.ac.unicircle.identity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByExternalSubject(String externalSubject);
    Optional<UserAccount> findByUniversityOidcIssuerAndExternalSubject(String oidcIssuer, String externalSubject);
}
