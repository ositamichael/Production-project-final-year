package uk.ac.unicircle.lostfound;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import uk.ac.unicircle.identity.CurrentIdentity;
import uk.ac.unicircle.identity.CurrentIdentity.SignedInAccount;
import uk.ac.unicircle.lostfound.LostFoundPhotoStorage.StoredPhoto;
import uk.ac.unicircle.lostfound.LostFoundRequests.CreateReportRequest;
import uk.ac.unicircle.lostfound.LostFoundRequests.SubmitClaimRequest;
import uk.ac.unicircle.lostfound.LostFoundResponses.ClaimResponse;
import uk.ac.unicircle.lostfound.LostFoundResponses.ReportResponse;

@Service
class LostFoundService {

    private static final List<String> TYPES = List.of("LOST", "FOUND");
    private static final List<String> CATEGORIES =
            List.of("ELECTRONICS", "KEYS", "BAGS", "CLOTHING", "DOCUMENTS", "ACCESSORIES", "OTHER");
    private static final List<String> CUSTODY =
            List.of("WITH_REPORTER", "CAMPUS_SECURITY", "SERVICE_DESK", "NOT_APPLICABLE");

    private final CurrentIdentity currentIdentity;
    private final JdbcTemplate jdbc;
    private final LostFoundPhotoStorage photos;

    LostFoundService(CurrentIdentity currentIdentity, JdbcTemplate jdbc, LostFoundPhotoStorage photos) {
        this.currentIdentity = currentIdentity;
        this.jdbc = jdbc;
        this.photos = photos;
    }

    @Transactional(readOnly = true)
    List<ReportResponse> catalogue(Authentication authentication, String type, String category, String query) {
        SignedInAccount current = currentIdentity.require(authentication);
        String cleanType = option(type, TYPES, "report type");
        String cleanCategory = option(category, CATEGORIES, "item category");
        String cleanQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (cleanQuery.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search terms must be 100 characters or fewer.");
        }
        String contains = "%" + cleanQuery + "%";
        return jdbc.query(baseQuery() + """
                WHERE report.university_id = ?
                  AND report.moderation_status = 'APPROVED'
                  AND report.report_status IN ('OPEN', 'MATCHED')
                  AND (? = '' OR report.report_type = ?)
                  AND (? = '' OR report.category = ?)
                  AND (? = '' OR LOWER(report.item_name) LIKE ? OR LOWER(report.colour) LIKE ?
                       OR LOWER(report.public_description) LIKE ? OR LOWER(report.last_seen_location) LIKE ?)
                ORDER BY report.occurred_on DESC, report.created_at DESC
                """, (row, number) -> response(row), current.accountId(), current.accountId(),
                current.universityId(), cleanType, cleanType, cleanCategory, cleanCategory,
                cleanQuery, contains, contains, contains, contains);
    }

    @Transactional(readOnly = true)
    List<ReportResponse> myReports(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        return jdbc.query(baseQuery() + """
                WHERE report.university_id = ? AND report.reporter_id = ?
                ORDER BY report.created_at DESC
                """, (row, number) -> response(row), current.accountId(), current.accountId(),
                current.universityId(), current.accountId());
    }

    @Transactional(readOnly = true)
    List<ClaimResponse> myClaims(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        return jdbc.query("""
                SELECT claim.id, claim.report_id, report.item_name, claim.claim_status, claim.created_at
                FROM lost_found_claims claim
                JOIN lost_found_reports report ON report.id = claim.report_id
                WHERE claim.university_id = ? AND claim.claimant_id = ?
                ORDER BY claim.created_at DESC
                """, (row, number) -> new ClaimResponse(
                        row.getObject(1, UUID.class), row.getObject(2, UUID.class), row.getString(3),
                        row.getString(4), row.getObject(5, java.time.OffsetDateTime.class).toInstant()),
                current.universityId(), current.accountId());
    }

    @Transactional
    ReportResponse create(Authentication authentication, CreateReportRequest request) {
        SignedInAccount current = currentIdentity.require(authentication);
        String type = option(request.reportType(), TYPES, "report type");
        String category = option(request.category(), CATEGORIES, "item category");
        String custody = option(request.custodyStatus(), CUSTODY, "custody option");
        if (type.equals("LOST") && !custody.equals("NOT_APPLICABLE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lost items cannot have a custody location.");
        }
        if (type.equals("FOUND") && custody.equals("NOT_APPLICABLE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose where the found item is being kept.");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO lost_found_reports
                  (id, university_id, reporter_id, report_type, category, item_name, colour,
                   public_description, private_identifying_details, last_seen_location, occurred_on,
                   custody_status, moderation_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                """, id, current.universityId(), current.accountId(), type, category,
                request.itemName().trim(), cleanOptional(request.colour()), request.publicDescription().trim(),
                request.privateIdentifyingDetails().trim(), request.lastSeenLocation().trim(),
                request.occurredOn(), custody);
        return report(current, id, true);
    }

    ReportResponse uploadPhoto(Authentication authentication, UUID reportId, MultipartFile upload) {
        SignedInAccount current = currentIdentity.require(authentication);
        String oldKey = requireOwnedReport(current, reportId);
        StoredPhoto stored = null;
        try {
            stored = photos.store(upload);
            int changed = jdbc.update("""
                    UPDATE lost_found_reports
                    SET photo_storage_key = ?, photo_media_type = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE id = ? AND university_id = ? AND reporter_id = ?
                    """, stored.key(), stored.mediaType(), reportId, current.universityId(), current.accountId());
            if (changed != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This report is unavailable.");
            try {
                photos.delete(oldKey);
            } catch (IOException ignored) {
                // A stale old photograph must not invalidate the newly saved report.
            }
            return report(current, reportId, true);
        } catch (InvalidLostFoundPhotoException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IOException | RuntimeException exception) {
            if (stored != null) {
                try {
                    photos.delete(stored.key());
                } catch (IOException ignored) {
                    // The original storage error is the useful failure to report.
                }
            }
            if (exception instanceof RuntimeException runtime) throw runtime;
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "The item photograph could not be stored safely.", exception);
        }
    }

    @Transactional(readOnly = true)
    StoredItemPhoto photo(Authentication authentication, UUID reportId) {
        SignedInAccount current = currentIdentity.require(authentication);
        List<PhotoRecord> records = jdbc.query("""
                SELECT report.photo_storage_key, report.photo_media_type
                FROM lost_found_reports report
                WHERE report.id = ? AND report.university_id = ? AND report.photo_storage_key IS NOT NULL
                  AND (report.reporter_id = ? OR
                       (report.moderation_status = 'APPROVED' AND report.report_status IN ('OPEN', 'MATCHED')))
                """, (row, number) -> new PhotoRecord(row.getString(1), row.getString(2)),
                reportId, current.universityId(), current.accountId());
        if (records.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This photograph is unavailable.");
        try {
            PhotoRecord record = records.getFirst();
            return new StoredItemPhoto(photos.read(record.key()), record.mediaType());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This photograph is unavailable.", exception);
        }
    }

    @Transactional
    ClaimResponse claim(Authentication authentication, UUID reportId, SubmitClaimRequest request) {
        SignedInAccount current = currentIdentity.require(authentication);
        List<ClaimTarget> targets = jdbc.query("""
                SELECT item_name, reporter_id
                FROM lost_found_reports
                WHERE id = ? AND university_id = ? AND report_type = 'FOUND'
                  AND moderation_status = 'APPROVED' AND report_status = 'OPEN'
                """, (row, number) -> new ClaimTarget(row.getString(1), row.getObject(2, UUID.class)),
                reportId, current.universityId());
        if (targets.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This item is not open for claims.");
        ClaimTarget target = targets.getFirst();
        if (target.reporterId().equals(current.accountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot claim an item that you reported.");
        }
        UUID claimId = UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO lost_found_claims
                      (id, university_id, report_id, claimant_id, private_ownership_evidence)
                    VALUES (?, ?, ?, ?, ?)
                    """, claimId, current.universityId(), reportId, current.accountId(),
                    request.privateOwnershipEvidence().trim());
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already submitted a claim for this item.");
        }
        jdbc.update("""
                INSERT INTO notifications
                  (id, university_id, recipient_id, notification_type, title, body, resource_type, resource_id)
                VALUES (?, ?, ?, 'LOST_FOUND', 'Private ownership claim received',
                        'A student submitted private ownership evidence for your found-item report.',
                        'LOST_FOUND_REPORT', ?)
                """, UUID.randomUUID(), current.universityId(), target.reporterId(), reportId);
        return new ClaimResponse(claimId, reportId, target.itemName(), "SUBMITTED", Instant.now());
    }

    @Transactional
    ReportResponse withdraw(Authentication authentication, UUID reportId) {
        SignedInAccount current = currentIdentity.require(authentication);
        int changed = jdbc.update("""
                UPDATE lost_found_reports
                SET report_status = 'WITHDRAWN', updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND university_id = ? AND reporter_id = ?
                  AND report_status NOT IN ('RETURNED', 'WITHDRAWN')
                """, reportId, current.universityId(), current.accountId());
        if (changed != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "This report cannot be withdrawn.");
        return report(current, reportId, true);
    }

    private String option(String value, List<String> allowed, String label) {
        String clean = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!clean.isEmpty() && !allowed.contains(clean)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid " + label + ".");
        }
        return clean;
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String requireOwnedReport(SignedInAccount current, UUID reportId) {
        List<String> keys = jdbc.query("""
                SELECT photo_storage_key FROM lost_found_reports
                WHERE id = ? AND university_id = ? AND reporter_id = ? AND report_status = 'OPEN'
                """, (row, number) -> row.getString(1), reportId, current.universityId(), current.accountId());
        if (keys.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This report is unavailable.");
        return keys.getFirst();
    }

    private ReportResponse report(SignedInAccount current, UUID reportId, boolean allowUnapprovedOwnReport) {
        String moderation = allowUnapprovedOwnReport ? "" : " AND report.moderation_status = 'APPROVED'";
        List<ReportResponse> reports = jdbc.query(baseQuery() + """
                WHERE report.id = ? AND report.university_id = ?
                  AND (report.reporter_id = ? OR report.moderation_status = 'APPROVED')
                """ + moderation, (row, number) -> response(row), current.accountId(), current.accountId(),
                reportId, current.universityId(), current.accountId());
        if (reports.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This report is unavailable.");
        return reports.getFirst();
    }

    private String baseQuery() {
        return """
                SELECT report.id, report.report_type, report.category, report.item_name, report.colour,
                       report.public_description, report.last_seen_location, report.occurred_on,
                       report.custody_status, report.report_status, report.moderation_status,
                       report.photo_storage_key, report.created_at,
                       CASE WHEN report.reporter_id = ? THEN TRUE ELSE FALSE END,
                       claim.claim_status
                FROM lost_found_reports report
                LEFT JOIN lost_found_claims claim
                  ON claim.report_id = report.id AND claim.claimant_id = ?
                """;
    }

    private ReportResponse response(ResultSet row) throws SQLException {
        UUID id = row.getObject(1, UUID.class);
        boolean hasPhoto = row.getString(12) != null;
        return new ReportResponse(
                id, row.getString(2), row.getString(3), row.getString(4), row.getString(5),
                row.getString(6), row.getString(7), row.getObject(8, LocalDate.class),
                row.getString(9), row.getString(10), row.getString(11), hasPhoto,
                hasPhoto ? "/api/lost-found/" + id + "/photo" : null,
                row.getBoolean(14), row.getString(15),
                row.getObject(13, java.time.OffsetDateTime.class).toInstant());
    }

    record StoredItemPhoto(byte[] content, String mediaType) {
    }

    private record PhotoRecord(String key, String mediaType) {
    }

    private record ClaimTarget(String itemName, UUID reporterId) {
    }
}
