package uk.ac.unicircle.admin;

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
            @Value("${unicircle.pilot.bootstrap-admin-email:}") String email) {
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
        };
    }
}
