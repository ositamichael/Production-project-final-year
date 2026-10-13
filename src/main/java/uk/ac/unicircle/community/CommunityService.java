package uk.ac.unicircle.community;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import uk.ac.unicircle.community.CommunityRequests.CreateCircleRequest;
import uk.ac.unicircle.community.CommunityResponses.CircleResponse;
import uk.ac.unicircle.identity.CurrentIdentity;
import uk.ac.unicircle.identity.CurrentIdentity.SignedInAccount;

@Service
class CommunityService {
    private static final int MAX_OWNED_CIRCLES = 10;

    private final CurrentIdentity currentIdentity;
    private final JdbcTemplate jdbc;

    CommunityService(CurrentIdentity currentIdentity, JdbcTemplate jdbc) {
        this.currentIdentity = currentIdentity;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    List<CircleResponse> circles(Authentication authentication, String query, String category, boolean joinedOnly) {
        SignedInAccount current = currentIdentity.require(authentication);
        String cleanQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String cleanCategory = category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
        if (!cleanCategory.isBlank()) {
            try {
                CommunityCategory.valueOf(cleanCategory);
            } catch (IllegalArgumentException invalid) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid circle category.", invalid);
            }
        }
        String pattern = "%" + cleanQuery + "%";
        return jdbc.query("""
                SELECT circle.id, circle.name, circle.description, circle.category,
                       (SELECT COUNT(*) FROM circle_members members WHERE members.circle_id = circle.id),
                       CASE WHEN mine.account_id IS NULL THEN FALSE ELSE TRUE END,
                       CASE WHEN circle.owner_account_id = ? THEN TRUE ELSE FALSE END,
                       circle.created_at
                FROM campus_circles circle
                LEFT JOIN circle_members mine ON mine.circle_id = circle.id AND mine.account_id = ?
                WHERE circle.university_id = ? AND circle.active = TRUE
                  AND (? = '' OR LOWER(circle.name) LIKE ? OR LOWER(circle.description) LIKE ?)
                  AND (? = '' OR circle.category = ?)
                  AND (? = FALSE OR mine.account_id IS NOT NULL)
                ORDER BY CASE WHEN mine.account_id IS NULL THEN 1 ELSE 0 END,
                         (SELECT COUNT(*) FROM circle_members members WHERE members.circle_id = circle.id) DESC,
                         circle.name
                """, (result, row) -> response(result), current.accountId(), current.accountId(),
                current.universityId(), cleanQuery, pattern, pattern, cleanCategory, cleanCategory, joinedOnly);
    }

    @Transactional
    CircleResponse create(Authentication authentication, CreateCircleRequest request) {
        SignedInAccount current = currentIdentity.require(authentication);
        Integer owned = jdbc.queryForObject("""
                SELECT COUNT(*) FROM campus_circles
                WHERE university_id = ? AND owner_account_id = ? AND active = TRUE
                """, Integer.class, current.universityId(), current.accountId());
        if (owned != null && owned >= MAX_OWNED_CIRCLES) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have reached the limit for circles you can create.");
        }
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO campus_circles
                      (id, university_id, owner_account_id, name, description, category)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, id, current.universityId(), current.accountId(), request.name().trim(),
                    request.description().trim(), request.category().name());
            jdbc.update("""
                    INSERT INTO circle_members (circle_id, account_id, member_role)
                    VALUES (?, ?, 'OWNER')
                    """, id, current.accountId());
        } catch (DuplicateKeyException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A circle with this name already exists at your university.", duplicate);
        }
        return circle(current, id);
    }

    @Transactional
    CircleResponse join(Authentication authentication, UUID circleId) {
        SignedInAccount current = currentIdentity.require(authentication);
        lockAvailableCircle(current, circleId);
        Integer existing = jdbc.queryForObject("""
                SELECT COUNT(*) FROM circle_members WHERE circle_id = ? AND account_id = ?
                """, Integer.class, circleId, current.accountId());
        if (existing == null || existing == 0) {
            jdbc.update("""
                    INSERT INTO circle_members (circle_id, account_id, member_role)
                    VALUES (?, ?, 'MEMBER')
                    """, circleId, current.accountId());
        }
        return circle(current, circleId);
    }

    @Transactional
    CircleResponse leave(Authentication authentication, UUID circleId) {
        SignedInAccount current = currentIdentity.require(authentication);
        CircleLock circle = lockAvailableCircle(current, circleId);
        if (current.accountId().equals(circle.ownerAccountId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The circle owner cannot leave without transferring ownership.");
        }
        int changed = jdbc.update("""
                DELETE FROM circle_members WHERE circle_id = ? AND account_id = ?
                """, circleId, current.accountId());
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not a member of this circle.");
        }
        return circle(current, circleId);
    }

    private CircleLock lockAvailableCircle(SignedInAccount current, UUID circleId) {
        try {
            CircleLock circle = jdbc.queryForObject("""
                    SELECT university_id, owner_account_id, active
                    FROM campus_circles WHERE id = ? FOR UPDATE
                    """, (result, row) -> new CircleLock(result.getObject(1, UUID.class),
                            result.getObject(2, UUID.class), result.getBoolean(3)), circleId);
            if (!circle.universityId().equals(current.universityId()) || !circle.active()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This circle is unavailable.");
            }
            return circle;
        } catch (EmptyResultDataAccessException missing) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This circle is unavailable.", missing);
        }
    }

    private CircleResponse circle(SignedInAccount current, UUID circleId) {
        try {
            return jdbc.queryForObject("""
                    SELECT circle.id, circle.name, circle.description, circle.category,
                           (SELECT COUNT(*) FROM circle_members members WHERE members.circle_id = circle.id),
                           CASE WHEN mine.account_id IS NULL THEN FALSE ELSE TRUE END,
                           CASE WHEN circle.owner_account_id = ? THEN TRUE ELSE FALSE END,
                           circle.created_at
                    FROM campus_circles circle
                    LEFT JOIN circle_members mine ON mine.circle_id = circle.id AND mine.account_id = ?
                    WHERE circle.id = ? AND circle.university_id = ? AND circle.active = TRUE
                    """, (result, row) -> response(result), current.accountId(), current.accountId(), circleId,
                    current.universityId());
        } catch (EmptyResultDataAccessException missing) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This circle is unavailable.", missing);
        }
    }

    private CircleResponse response(ResultSet result) throws SQLException {
        return new CircleResponse(result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                CommunityCategory.valueOf(result.getString(4)), result.getLong(5), result.getBoolean(6),
                result.getBoolean(7), result.getObject(8, OffsetDateTime.class));
    }

    private record CircleLock(UUID universityId, UUID ownerAccountId, boolean active) {
    }
}
