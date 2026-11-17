package uk.ac.unicircle.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.unicircle.identity.CurrentIdentity;

@Service
class SearchService {
    private static final int RESULT_LIMIT = 30;

    private final CurrentIdentity currentIdentity;
    private final JdbcTemplate jdbc;

    SearchService(CurrentIdentity currentIdentity, JdbcTemplate jdbc) {
        this.currentIdentity = currentIdentity;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    List<SearchResult> search(Authentication authentication, String query) {
        var current = currentIdentity.require(authentication);
        String clean = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (clean.length() < 2) return List.of();
        String pattern = "%" + clean + "%";
        List<SearchResult> results = new ArrayList<>();
        results.addAll(jdbc.query("""
                SELECT account.id, account.display_name,
                       COALESCE(account.course_name, 'Test Campus member')
                FROM user_accounts account
                WHERE account.university_id = ? AND account.id <> ?
                  AND account.account_status = 'ACTIVE' AND account.profile_visibility <> 'PRIVATE'
                  AND NOT EXISTS (
                    SELECT 1 FROM connections blocked
                    WHERE blocked.university_id = account.university_id
                      AND blocked.status = 'BLOCKED'
                      AND (blocked.requester_id = ? OR blocked.addressee_id = ?)
                      AND (blocked.requester_id = account.id OR blocked.addressee_id = account.id)
                  )
                  AND (LOWER(account.display_name) LIKE ? OR LOWER(COALESCE(account.course_name, '')) LIKE ?)
                ORDER BY account.display_name LIMIT 8
                """, (result, row) -> new SearchResult("PERSON",
                        result.getObject(1, java.util.UUID.class), result.getString(2),
                        result.getString(3), "#messages-page"),
                current.universityId(), current.accountId(), current.accountId(), current.accountId(),
                pattern, pattern));
        results.addAll(jdbc.query("""
                SELECT id, title, venue FROM campus_events
                WHERE university_id = ? AND published = TRUE AND ends_at > CURRENT_TIMESTAMP
                  AND (LOWER(title) LIKE ? OR LOWER(summary) LIKE ? OR LOWER(venue) LIKE ?)
                ORDER BY starts_at LIMIT 6
                """, (result, row) -> new SearchResult("EVENT",
                        result.getObject(1, java.util.UUID.class), result.getString(2),
                        result.getString(3), "#events-page"),
                current.universityId(), pattern, pattern, pattern));
        results.addAll(jdbc.query("""
                SELECT id, title, provider_name FROM service_offerings
                WHERE university_id = ? AND active = TRUE
                  AND (LOWER(title) LIKE ? OR LOWER(description) LIKE ? OR LOWER(provider_name) LIKE ?)
                ORDER BY title LIMIT 6
                """, (result, row) -> new SearchResult("SERVICE",
                        result.getObject(1, java.util.UUID.class), result.getString(2),
                        result.getString(3), "#services-page"),
                current.universityId(), pattern, pattern, pattern));
        results.addAll(jdbc.query("""
                SELECT id, name, category FROM campus_circles
                WHERE university_id = ? AND active = TRUE
                  AND (LOWER(name) LIKE ? OR LOWER(description) LIKE ?)
                ORDER BY name LIMIT 5
                """, (result, row) -> new SearchResult("CIRCLE",
                        result.getObject(1, java.util.UUID.class), result.getString(2),
                        result.getString(3), "#circles-page"),
                current.universityId(), pattern, pattern));
        results.addAll(jdbc.query("""
                SELECT opportunity.id, opportunity.title, publisher.name
                FROM opportunities opportunity
                JOIN opportunity_publishers publisher ON publisher.id = opportunity.publisher_id
                WHERE opportunity.university_id = ? AND opportunity.moderation_status = 'APPROVED'
                  AND opportunity.closes_at >= CURRENT_DATE
                  AND (LOWER(opportunity.title) LIKE ? OR LOWER(opportunity.summary) LIKE ?
                       OR LOWER(opportunity.skills_text) LIKE ? OR LOWER(publisher.name) LIKE ?)
                ORDER BY opportunity.closes_at LIMIT 5
                """, (result, row) -> new SearchResult("OPPORTUNITY",
                        result.getObject(1, java.util.UUID.class), result.getString(2),
                        result.getString(3), "#opportunities-page"),
                current.universityId(), pattern, pattern, pattern, pattern));
        return results.stream().limit(RESULT_LIMIT).toList();
    }
}
