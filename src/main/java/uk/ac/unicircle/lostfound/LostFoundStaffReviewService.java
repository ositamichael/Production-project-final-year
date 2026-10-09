package uk.ac.unicircle.lostfound;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
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

@Service
class LostFoundStaffReviewService {

    private final CurrentIdentity identities;
    private final JdbcTemplate jdbc;
    private final LostFoundPhotoStorage photos;

    LostFoundStaffReviewService(CurrentIdentity identities, JdbcTemplate jdbc, LostFoundPhotoStorage photos) {
        this.identities = identities;
        this.jdbc = jdbc;
        this.photos = photos;
    }

    @Transactional(readOnly = true)
    List<PendingReport> pending(Authentication authentication) {
        SignedInAccount reviewer = requireReviewer(authentication);
        return jdbc.query("""
                SELECT report.id, report.report_type, report.category, report.item_name, report.colour,
                       report.public_description, report.private_identifying_details,
                       report.last_seen_location, report.occurred_on, report.custody_status,
                       report.photo_storage_key, reporter.display_name, report.created_at
                FROM lost_found_reports report
                JOIN user_accounts reporter ON reporter.id = report.reporter_id
                WHERE report.university_id = ? AND report.moderation_status = 'PENDING'
                  AND report.report_status = 'OPEN' AND report.reporter_id <> ?
                ORDER BY report.created_at ASC
                """, (row, number) -> {
                    UUID id = row.getObject(1, UUID.class);
                    boolean hasPhoto = row.getString(11) != null;
                    return new PendingReport(
                            id, row.getString(2), row.getString(3), row.getString(4), row.getString(5),
                            row.getString(6), row.getString(7), row.getString(8),
                            row.getObject(9, LocalDate.class), row.getString(10), hasPhoto,
                            hasPhoto ? "/api/lost-found/staff/reports/" + id + "/photo" : null,
                            row.getString(12), row.getObject(13, java.time.OffsetDateTime.class).toInstant());
                }, reviewer.universityId(), reviewer.accountId());
    }

    @Transactional
    ReviewResult decide(Authentication authentication, UUID reportId, String rawDecision, String rawReason) {
        SignedInAccount reviewer = requireReviewer(authentication);
        String decision = rawDecision == null ? "" : rawDecision.trim().toUpperCase(Locale.ROOT);
        if (!decision.equals("APPROVED") && !decision.equals("REJECTED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose approve or reject.");
        }
        String reason = rawReason == null ? "" : rawReason.strip();
        if (reason.length() < 20 || reason.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Give a review reason of 20 to 500 characters.");
        }
        List<ReviewTarget> targets = jdbc.query("""
                SELECT reporter_id, item_name
                FROM lost_found_reports WHERE id = ? AND university_id = ?
                """, (row, number) -> new ReviewTarget(
                        row.getObject(1, UUID.class), row.getString(2)),
                reportId, reviewer.universityId());
        if (targets.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This report is unavailable.");
        ReviewTarget target = targets.getFirst();
        if (target.reporterId().equals(reviewer.accountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot moderate your own report.");
        }
        int changed = jdbc.update("""
                UPDATE lost_found_reports
                SET moderation_status = ?, review_note = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND university_id = ? AND moderation_status = 'PENDING' AND report_status = 'OPEN'
                """, decision, decision.equals("REJECTED") ? reason : null, reportId, reviewer.universityId());
        if (changed != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This report is no longer awaiting review.");
        }
        jdbc.update("""
                INSERT INTO lost_found_review_audit
                  (id, university_id, report_id, reviewer_id, decision, reason)
                VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), reviewer.universityId(), reportId, reviewer.accountId(), decision, reason);
        jdbc.update("""
                INSERT INTO notifications
                  (id, university_id, recipient_id, notification_type, title, body, resource_type, resource_id)
                VALUES (?, ?, ?, 'LOST_FOUND', ?, ?, 'LOST_FOUND_REPORT', ?)
                """, UUID.randomUUID(), reviewer.universityId(), target.reporterId(),
                decision.equals("APPROVED") ? "Item report approved" : "Item report needs changes",
                decision.equals("APPROVED")
                        ? "Your lost-and-found report was approved for the university catalogue."
                        : "Your lost-and-found report was not published. Open My reports to read the review note.",
                reportId);
        return new ReviewResult(reportId, decision, target.itemName());
    }

    @Transactional(readOnly = true)
    StaffPhoto photo(Authentication authentication, UUID reportId) {
        SignedInAccount reviewer = requireReviewer(authentication);
        List<PhotoRecord> records = jdbc.query("""
                SELECT photo_storage_key, photo_media_type FROM lost_found_reports
                WHERE id = ? AND university_id = ? AND moderation_status = 'PENDING'
                  AND report_status = 'OPEN' AND photo_storage_key IS NOT NULL AND reporter_id <> ?
                """, (row, number) -> new PhotoRecord(row.getString(1), row.getString(2)),
                reportId, reviewer.universityId(), reviewer.accountId());
        if (records.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This photograph is unavailable.");
        try {
            PhotoRecord record = records.getFirst();
            return new StaffPhoto(photos.read(record.key()), record.mediaType());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This photograph is unavailable.", exception);
        }
    }

    private SignedInAccount requireReviewer(Authentication authentication) {
        SignedInAccount account = identities.require(authentication);
        Boolean authorised = jdbc.queryForObject("""
                SELECT CASE WHEN COUNT(*) = 1 THEN TRUE ELSE FALSE END
                FROM user_accounts
                WHERE id = ? AND university_id = ? AND role IN ('STAFF', 'ADMIN')
                """, Boolean.class, account.accountId(), account.universityId());
        if (!Boolean.TRUE.equals(authorised)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff review access is required.");
        }
        return account;
    }

    record PendingReport(
            UUID id, String reportType, String category, String itemName, String colour,
            String publicDescription, String privateIdentifyingDetails, String lastSeenLocation,
            LocalDate occurredOn, String custodyStatus, boolean hasPhoto, String photoUrl,
            String reporterName, Instant createdAt) {
    }

    record ReviewResult(UUID reportId, String moderationStatus, String itemName) {
    }

    record StaffPhoto(byte[] content, String mediaType) {
    }

    private record ReviewTarget(UUID reporterId, String itemName) {
    }

    private record PhotoRecord(String key, String mediaType) {
    }
}
