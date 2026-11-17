package uk.ac.unicircle.opportunity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import uk.ac.unicircle.identity.CurrentIdentity;
import uk.ac.unicircle.identity.CurrentIdentity.SignedInAccount;
import uk.ac.unicircle.opportunity.OpportunityResponses.OpportunityResponse;

@Service
class OpportunityService {

    private static final List<String> TYPES = List.of("JOB", "PLACEMENT", "VOLUNTEERING");
    private static final List<String> TRACKING_STATUSES =
            List.of("PLANNING", "APPLIED", "INTERVIEW", "OFFER", "CLOSED");

    private final CurrentIdentity currentIdentity;
    private final JdbcTemplate jdbc;

    OpportunityService(CurrentIdentity currentIdentity, JdbcTemplate jdbc) {
        this.currentIdentity = currentIdentity;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    List<OpportunityResponse> catalogue(Authentication authentication, String type, String query) {
        SignedInAccount current = currentIdentity.require(authentication);
        String cleanType = normaliseType(type);
        String cleanQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (cleanQuery.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search terms must be 100 characters or fewer.");
        }
        String contains = "%" + cleanQuery + "%";
        return jdbc.query(baseQuery() + """
                WHERE opportunity.university_id = ?
                  AND opportunity.moderation_status = 'APPROVED'
                  AND publisher.verification_status = 'VERIFIED'
                  AND opportunity.closes_at >= CURRENT_DATE
                  AND (? = '' OR opportunity.opportunity_type = ?)
                  AND (? = '' OR LOWER(opportunity.title) LIKE ?
                       OR LOWER(opportunity.summary) LIKE ? OR LOWER(publisher.name) LIKE ?
                       OR LOWER(opportunity.skills_text) LIKE ?)
                ORDER BY opportunity.closes_at, opportunity.title
                """, (result, row) -> response(result), current.accountId(), current.accountId(),
                current.universityId(), cleanType, cleanType, cleanQuery,
                contains, contains, contains, contains);
    }

    @Transactional(readOnly = true)
    List<OpportunityResponse> tracker(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        return jdbc.query(baseQuery() + """
                WHERE opportunity.university_id = ?
                  AND opportunity.moderation_status = 'APPROVED'
                  AND publisher.verification_status = 'VERIFIED'
                  AND (saved.user_id IS NOT NULL OR tracking.user_id IS NOT NULL)
                ORDER BY COALESCE(tracking.updated_at, saved.saved_at) DESC, opportunity.title
                """, (result, row) -> response(result), current.accountId(), current.accountId(),
                current.universityId());
    }

    @Transactional
    OpportunityResponse save(Authentication authentication, UUID opportunityId) {
        SignedInAccount current = currentIdentity.require(authentication);
        requireAvailable(current, opportunityId);
        jdbc.update("""
                INSERT INTO saved_opportunities (opportunity_id, user_id)
                SELECT ?, ? WHERE NOT EXISTS (
                    SELECT 1 FROM saved_opportunities WHERE opportunity_id = ? AND user_id = ?
                )
                """, opportunityId, current.accountId(), opportunityId, current.accountId());
        return opportunity(current, opportunityId);
    }

    @Transactional
    OpportunityResponse unsave(Authentication authentication, UUID opportunityId) {
        SignedInAccount current = currentIdentity.require(authentication);
        requireAvailable(current, opportunityId);
        jdbc.update("DELETE FROM saved_opportunities WHERE opportunity_id = ? AND user_id = ?",
                opportunityId, current.accountId());
        return opportunity(current, opportunityId);
    }

    @Transactional
    OpportunityResponse track(Authentication authentication, UUID opportunityId, String status) {
        SignedInAccount current = currentIdentity.require(authentication);
        requireAvailable(current, opportunityId);
        String cleanStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!TRACKING_STATUSES.contains(cleanStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid application stage.");
        }
        Integer existing = jdbc.queryForObject("""
                SELECT COUNT(*) FROM opportunity_tracking WHERE opportunity_id = ? AND user_id = ?
                """, Integer.class, opportunityId, current.accountId());
        if (existing != null && existing > 0) {
            jdbc.update("""
                    UPDATE opportunity_tracking SET tracking_status = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE opportunity_id = ? AND user_id = ?
                    """, cleanStatus, opportunityId, current.accountId());
        } else {
            jdbc.update("""
                    INSERT INTO opportunity_tracking (opportunity_id, user_id, tracking_status)
                    VALUES (?, ?, ?)
                    """, opportunityId, current.accountId(), cleanStatus);
        }
        return opportunity(current, opportunityId);
    }

    private String normaliseType(String type) {
        String clean = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!clean.isEmpty() && !TYPES.contains(clean)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid opportunity type.");
        }
        return clean;
    }

    private void requireAvailable(SignedInAccount current, UUID opportunityId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM opportunities opportunity
                JOIN opportunity_publishers publisher ON publisher.id = opportunity.publisher_id
                WHERE opportunity.id = ? AND opportunity.university_id = ?
                  AND opportunity.moderation_status = 'APPROVED'
                  AND publisher.verification_status = 'VERIFIED'
                  AND opportunity.closes_at >= CURRENT_DATE
                """, Integer.class, opportunityId, current.universityId());
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This opportunity is unavailable.");
        }
    }

    private OpportunityResponse opportunity(SignedInAccount current, UUID opportunityId) {
        List<OpportunityResponse> result = jdbc.query(baseQuery() + """
                WHERE opportunity.id = ? AND opportunity.university_id = ?
                  AND opportunity.moderation_status = 'APPROVED'
                  AND publisher.verification_status = 'VERIFIED'
                """, (row, number) -> response(row), current.accountId(), current.accountId(),
                opportunityId, current.universityId());
        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This opportunity is unavailable.");
        }
        return result.getFirst();
    }

    private String baseQuery() {
        return """
                SELECT opportunity.id, opportunity.title, opportunity.summary, opportunity.description,
                       opportunity.opportunity_type, opportunity.work_mode, opportunity.location,
                       opportunity.hours_label, opportunity.compensation_label, opportunity.closes_at,
                       opportunity.skills_text, opportunity.eligibility, publisher.name,
                       publisher.publisher_type,
                       CASE WHEN publisher.verification_status = 'VERIFIED' THEN TRUE ELSE FALSE END,
                       CASE WHEN saved.user_id IS NOT NULL THEN TRUE ELSE FALSE END,
                       tracking.tracking_status
                FROM opportunities opportunity
                JOIN opportunity_publishers publisher ON publisher.id = opportunity.publisher_id
                LEFT JOIN saved_opportunities saved
                  ON saved.opportunity_id = opportunity.id AND saved.user_id = ?
                LEFT JOIN opportunity_tracking tracking
                  ON tracking.opportunity_id = opportunity.id AND tracking.user_id = ?
                """;
    }

    private OpportunityResponse response(ResultSet result) throws SQLException {
        List<String> skills = Arrays.stream(result.getString(11).split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        return new OpportunityResponse(
                result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                result.getString(4), result.getString(5), result.getString(6), result.getString(7),
                result.getString(8), result.getString(9), result.getObject(10, LocalDate.class),
                skills, result.getString(12), result.getString(13), result.getString(14),
                result.getBoolean(15), result.getBoolean(16), result.getString(17));
    }
}
