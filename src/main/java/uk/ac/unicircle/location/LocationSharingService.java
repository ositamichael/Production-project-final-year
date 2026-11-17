package uk.ac.unicircle.location;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import uk.ac.unicircle.identity.CurrentIdentity;
import uk.ac.unicircle.identity.CurrentIdentity.SignedInAccount;

@Service
@Profile("pilot")
class LocationSharingService {

    private final CurrentIdentity currentIdentity;
    private final JdbcTemplate jdbc;

    LocationSharingService(CurrentIdentity currentIdentity, JdbcTemplate jdbc) {
        this.currentIdentity = currentIdentity;
        this.jdbc = jdbc;
    }

    @Transactional
    MyLocationResponse share(Authentication authentication, double latitude, double longitude, int accuracyMetres) {
        SignedInAccount current = currentIdentity.require(authentication);
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180
                || accuracyMetres < 0 || accuracyMetres > 10000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The location is invalid.");
        }
        BigDecimal lat = BigDecimal.valueOf(latitude).setScale(4, RoundingMode.HALF_UP);
        BigDecimal lon = BigDecimal.valueOf(longitude).setScale(4, RoundingMode.HALF_UP);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expires = now.plusMinutes(5);
        int updated = jdbc.update("""
                UPDATE location_shares SET latitude = ?, longitude = ?, accuracy_metres = ?,
                    updated_at = ?, expires_at = ?
                WHERE account_id = ? AND university_id = ?
                """, lat, lon, accuracyMetres, now, expires, current.accountId(), current.universityId());
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO location_shares
                        (account_id, university_id, latitude, longitude, accuracy_metres, updated_at, expires_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, current.accountId(), current.universityId(), lat, lon, accuracyMetres, now, expires);
        }
        return mine(authentication);
    }

    @Transactional(readOnly = true)
    MyLocationResponse mine(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        return jdbc.query("""
                SELECT latitude, longitude, accuracy_metres, expires_at
                FROM location_shares
                WHERE account_id = ? AND university_id = ? AND expires_at > CURRENT_TIMESTAMP
                """, (result, row) -> new MyLocationResponse(true,
                        result.getBigDecimal(1), result.getBigDecimal(2), result.getInt(3),
                        result.getObject(4, OffsetDateTime.class)),
                current.accountId(), current.universityId()).stream().findFirst()
                .orElse(new MyLocationResponse(false, null, null, null, null));
    }

    @Transactional
    void stop(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        jdbc.update("DELETE FROM location_shares WHERE account_id = ? AND university_id = ?",
                current.accountId(), current.universityId());
    }

    @Transactional(readOnly = true)
    List<FriendLocationResponse> visibleFriends(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        Integer sharing = jdbc.queryForObject("""
                SELECT COUNT(*) FROM location_shares
                WHERE account_id = ? AND university_id = ? AND expires_at > CURRENT_TIMESTAMP
                """, Integer.class, current.accountId(), current.universityId());
        if (sharing == null || sharing == 0) {
            return List.of();
        }
        return jdbc.query("""
                SELECT friend.id, friend.display_name, ls.latitude, ls.longitude,
                       ls.accuracy_metres, ls.updated_at, ls.expires_at
                FROM connections c
                JOIN user_accounts friend ON friend.id =
                    CASE WHEN c.requester_id = ? THEN c.addressee_id ELSE c.requester_id END
                JOIN location_shares ls ON ls.account_id = friend.id
                WHERE c.university_id = ? AND c.status = 'ACCEPTED'
                  AND (c.requester_id = ? OR c.addressee_id = ?)
                  AND friend.university_id = ?
                  AND ls.university_id = ? AND ls.expires_at > CURRENT_TIMESTAMP
                ORDER BY friend.display_name
                """, (result, row) -> new FriendLocationResponse(
                        result.getObject(1, UUID.class), result.getString(2),
                        result.getBigDecimal(3), result.getBigDecimal(4), result.getInt(5),
                        result.getObject(6, OffsetDateTime.class), result.getObject(7, OffsetDateTime.class)),
                current.accountId(), current.universityId(), current.accountId(), current.accountId(),
                current.universityId(), current.universityId());
    }

    @Scheduled(fixedDelay = 60_000)
    void eraseExpiredShares() {
        jdbc.update("DELETE FROM location_shares WHERE expires_at <= CURRENT_TIMESTAMP");
    }

    record MyLocationResponse(boolean sharing, BigDecimal latitude, BigDecimal longitude,
            Integer accuracyMetres, OffsetDateTime expiresAt) {
    }

    record FriendLocationResponse(UUID accountId, String displayName, BigDecimal latitude,
            BigDecimal longitude, int accuracyMetres, OffsetDateTime updatedAt, OffsetDateTime expiresAt) {
    }
}
