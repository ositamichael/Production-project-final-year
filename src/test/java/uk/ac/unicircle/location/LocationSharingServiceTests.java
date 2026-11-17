package uk.ac.unicircle.location;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles("pilot")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:unicircle-location-purge;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@Transactional
class LocationSharingServiceTests {

    @Autowired LocationSharingService locations;
    @Autowired JdbcTemplate jdbc;

    @Test
    void expiredSharesAreDeletedWithoutRemovingCurrentShares() {
        UUID universityId = UUID.randomUUID();
        UUID expiredAccount = UUID.randomUUID();
        UUID currentAccount = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO universities (id, slug, display_name, email_domain)
                VALUES (?, 'location-test-campus', 'Location Test Campus', 'location-test.example')
                """, universityId);
        insertAccount(expiredAccount, universityId, "expired@location-test.example");
        insertAccount(currentAccount, universityId, "current@location-test.example");

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        insertShare(expiredAccount, universityId, now.minusMinutes(1));
        insertShare(currentAccount, universityId, now.plusMinutes(5));

        assertThat(countShares(expiredAccount)).isEqualTo(1);
        assertThat(countShares(currentAccount)).isEqualTo(1);

        locations.eraseExpiredShares();

        assertThat(countShares(expiredAccount)).isZero();
        assertThat(countShares(currentAccount)).isEqualTo(1);
    }

    private void insertAccount(UUID accountId, UUID universityId, String email) {
        jdbc.update("""
                INSERT INTO user_accounts
                    (id, university_id, external_subject, email, display_name, role)
                VALUES (?, ?, ?, ?, 'Location Test User', 'STUDENT')
                """, accountId, universityId, accountId.toString(), email);
    }

    private void insertShare(UUID accountId, UUID universityId, OffsetDateTime expiresAt) {
        OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO location_shares
                    (account_id, university_id, latitude, longitude, accuracy_metres, updated_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, accountId, universityId, BigDecimal.valueOf(53.8013),
                BigDecimal.valueOf(-1.5486), 10, updatedAt, expiresAt);
    }

    private Integer countShares(UUID accountId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM location_shares WHERE account_id = ?", Integer.class, accountId);
    }
}
