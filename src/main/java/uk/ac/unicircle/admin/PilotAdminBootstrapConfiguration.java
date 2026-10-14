package uk.ac.unicircle.admin;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("pilot")
class PilotAdminBootstrapConfiguration {

    @Bean
    @Order(90)
    ApplicationRunner promoteConfiguredPilotAdministrator(
            JdbcTemplate jdbc,
            @Value("${unicircle.pilot.bootstrap-admin-email:}") String email,
            @Value("${unicircle.pilot.bootstrap-admin-recovery-token-hash:6798ce72b13b638b1b59a96075f57fa883ca7fc81f885f926fc6ba7906dab54c}")
            String recoveryTokenHash) {
        return arguments -> {
            if (email == null || email.isBlank()) return;
            jdbc.update("""
                    UPDATE user_accounts SET role = 'ADMIN', account_status = 'ACTIVE',
                        updated_at = CURRENT_TIMESTAMP
                    WHERE LOWER(email) = LOWER(?)
                      AND university_id = (
                        SELECT id FROM universities WHERE slug = 'pilot-campus'
                      )
                    """, email.strip());
            if (recoveryTokenHash == null || recoveryTokenHash.isBlank()) return;
            jdbc.update("""
                    INSERT INTO pilot_password_reset_tokens
                      (id, account_id, token_hash, expires_at)
                    SELECT ?, account.id, ?, ?
                    FROM user_accounts account
                    JOIN universities university ON university.id = account.university_id
                    WHERE LOWER(account.email) = LOWER(?)
                      AND university.slug = 'pilot-campus'
                      AND NOT EXISTS (
                        SELECT 1 FROM pilot_password_reset_tokens existing
                        WHERE existing.token_hash = ?
                      )
                    """, UUID.randomUUID(), recoveryTokenHash,
                    OffsetDateTime.now(ZoneOffset.UTC).plusDays(2), email.strip(), recoveryTokenHash);
        };
    }
}
