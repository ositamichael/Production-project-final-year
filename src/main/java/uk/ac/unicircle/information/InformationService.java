package uk.ac.unicircle.information;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import uk.ac.unicircle.identity.CurrentIdentity;
import uk.ac.unicircle.identity.CurrentIdentity.SignedInAccount;
import uk.ac.unicircle.information.InformationRequests.AddSupportReplyRequest;
import uk.ac.unicircle.information.InformationRequests.CreateSupportCaseRequest;
import uk.ac.unicircle.information.InformationRequests.MeetingMessageRequest;
import uk.ac.unicircle.information.InformationRequests.PublishAnnouncementRequest;
import uk.ac.unicircle.information.InformationResponses.AnnouncementResponse;
import uk.ac.unicircle.information.InformationResponses.MeetingMessageResponse;
import uk.ac.unicircle.information.InformationResponses.MeetingParticipantResponse;
import uk.ac.unicircle.information.InformationResponses.MeetingRoomResponse;
import uk.ac.unicircle.information.InformationResponses.MeetingSummaryResponse;
import uk.ac.unicircle.information.InformationResponses.SupportCaseResponse;
import uk.ac.unicircle.information.InformationResponses.SupportReplyResponse;

@Service
class InformationService {

    private final CurrentIdentity currentIdentity;
    private final JdbcTemplate jdbc;

    InformationService(CurrentIdentity currentIdentity, JdbcTemplate jdbc) {
        this.currentIdentity = currentIdentity;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    List<AnnouncementResponse> announcements(Authentication authentication, String category) {
        SignedInAccount current = currentIdentity.require(authentication);
        String cleanCategory = category == null ? "" : category.strip().toUpperCase(Locale.ROOT);
        return jdbc.query("""
                SELECT announcement.id, announcement.author_name, announcement.author_role,
                       announcement.category, announcement.audience_type, announcement.audience_label,
                       announcement.title, announcement.body, announcement.action_label,
                       announcement.action_url, announcement.pinned, announcement.urgent,
                       CASE WHEN reads.user_id IS NULL THEN FALSE ELSE TRUE END,
                       announcement.published_at
                FROM campus_announcements announcement
                JOIN user_accounts account ON account.id = ?
                LEFT JOIN announcement_reads reads
                  ON reads.announcement_id = announcement.id AND reads.user_id = account.id
                WHERE announcement.university_id = ?
                  AND (announcement.expires_at IS NULL OR announcement.expires_at > CURRENT_TIMESTAMP)
                  AND (? = '' OR announcement.category = ?)
                  AND (
                    announcement.audience_type = 'UNIVERSITY'
                    OR (announcement.audience_type = 'COURSE'
                        AND LOWER(announcement.audience_label) = LOWER(account.course_name))
                    OR (announcement.audience_type = 'YEAR'
                        AND announcement.audience_label = CAST(account.year_of_study AS VARCHAR))
                    OR (announcement.audience_type = 'MODULE' AND EXISTS (
                        SELECT 1 FROM enrolments enrolment
                        JOIN programme_modules link ON link.programme_id = enrolment.programme_id
                        JOIN academic_modules module ON module.id = link.module_id
                        WHERE enrolment.user_id = account.id AND enrolment.status = 'ACTIVE'
                          AND (module.code = announcement.audience_label
                               OR module.title = announcement.audience_label)
                    ))
                  )
                ORDER BY announcement.urgent DESC, announcement.pinned DESC,
                         announcement.published_at DESC
                """, (result, row) -> new AnnouncementResponse(
                        result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                        result.getString(4), result.getString(5), result.getString(6),
                        result.getString(7), result.getString(8), result.getString(9),
                        result.getString(10), result.getBoolean(11), result.getBoolean(12),
                        result.getBoolean(13), result.getObject(14, OffsetDateTime.class)),
                current.accountId(), current.universityId(), cleanCategory, cleanCategory);
    }

    @Transactional
    AnnouncementResponse markAnnouncementRead(Authentication authentication, UUID announcementId) {
        SignedInAccount current = currentIdentity.require(authentication);
        announcements(authentication, "").stream()
                .filter(item -> item.id().equals(announcementId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "This announcement is unavailable."));
        int changed = jdbc.update("""
                UPDATE announcement_reads SET read_at = CURRENT_TIMESTAMP
                WHERE announcement_id = ? AND user_id = ?
                """, announcementId, current.accountId());
        if (changed == 0) {
            jdbc.update("""
                    INSERT INTO announcement_reads (announcement_id, user_id)
                    VALUES (?, ?)
                    """, announcementId, current.accountId());
        }
        return announcements(authentication, "").stream()
                .filter(item -> item.id().equals(announcementId))
                .findFirst().orElseThrow();
    }

    @Transactional
    AnnouncementResponse publishAnnouncement(Authentication authentication,
            PublishAnnouncementRequest request) {
        SignedInAccount current = currentIdentity.require(authentication);
        Publisher publisher = requirePublisher(current);
        String audienceLabel = clean(request.audienceLabel());
        if (!"UNIVERSITY".equals(request.audienceType()) && audienceLabel == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose the course, year or module that should receive this announcement.");
        }
        if ("UNIVERSITY".equals(request.audienceType())) audienceLabel = "All students";
        OffsetDateTime publishedAt = OffsetDateTime.now();
        if (request.expiresAt() != null && !request.expiresAt().isAfter(publishedAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The expiry time must be in the future.");
        }
        UUID id = UUID.randomUUID();
        boolean urgent = request.urgent() || "URGENT".equals(request.category());
        jdbc.update("""
                INSERT INTO campus_announcements
                  (id, university_id, author_id, author_name, author_role, category,
                   audience_type, audience_label, title, body, action_label, action_url,
                   pinned, urgent, published_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, current.universityId(), current.accountId(), publisher.name(),
                publisher.roleLabel(), request.category(), request.audienceType(), audienceLabel,
                request.title().strip(), request.body().strip(), clean(request.actionLabel()),
                clean(request.actionUrl()), request.pinned(), urgent, publishedAt, request.expiresAt());
        return new AnnouncementResponse(id, publisher.name(), publisher.roleLabel(), request.category(),
                request.audienceType(), audienceLabel, request.title().strip(), request.body().strip(),
                clean(request.actionLabel()), clean(request.actionUrl()), request.pinned(), urgent,
                false, publishedAt);
    }

    @Transactional(readOnly = true)
    List<SupportCaseResponse> supportCases(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        return jdbc.query("""
                SELECT id, reference_code, category, subject, description, status, priority,
                       assigned_team, created_at, updated_at
                FROM support_cases
                WHERE university_id = ? AND reporter_id = ?
                ORDER BY updated_at DESC
                """, (result, row) -> supportCaseResponse(result.getObject(1, UUID.class),
                        result.getString(2), result.getString(3), result.getString(4),
                        result.getString(5), result.getString(6), result.getString(7),
                        result.getString(8), result.getObject(9, OffsetDateTime.class),
                        result.getObject(10, OffsetDateTime.class)),
                current.universityId(), current.accountId());
    }

    @Transactional
    SupportCaseResponse createSupportCase(Authentication authentication, CreateSupportCaseRequest request) {
        SignedInAccount current = currentIdentity.require(authentication);
        UUID id = UUID.randomUUID();
        String reference = "UC-" + id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String priority = "WELLBEING".equals(request.category()) ? "HIGH" : "NORMAL";
        String team = supportTeam(request.category());
        jdbc.update("""
                INSERT INTO support_cases
                  (id, university_id, reporter_id, reference_code, category, subject,
                   description, status, priority, assigned_team)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'OPEN', ?, ?)
                """, id, current.universityId(), current.accountId(), reference,
                request.category(), request.subject().strip(), request.description().strip(),
                priority, team);
        return supportCase(current, id);
    }

    @Transactional
    SupportCaseResponse addSupportReply(Authentication authentication, UUID caseId,
            AddSupportReplyRequest request) {
        SignedInAccount current = currentIdentity.require(authentication);
        requireOwnedCase(current, caseId);
        jdbc.update("""
                INSERT INTO support_case_replies (id, case_id, author_id, body)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID(), caseId, current.accountId(), request.body().strip());
        jdbc.update("""
                UPDATE support_cases SET status = 'OPEN', updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND reporter_id = ?
                """, caseId, current.accountId());
        return supportCase(current, caseId);
    }

    @Transactional(readOnly = true)
    List<MeetingSummaryResponse> meetings(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        return jdbc.query("""
                SELECT meeting.id, meeting.host_name, meeting.title, meeting.module_code,
                       meeting.meeting_type, meeting.starts_at, meeting.ends_at,
                       meeting.location_label, meeting.agenda,
                       CASE WHEN mine.user_id IS NULL THEN FALSE ELSE TRUE END,
                       COALESCE(mine.hand_raised, FALSE),
                       (SELECT COUNT(*) FROM meeting_attendance attendee
                        WHERE attendee.meeting_id = meeting.id),
                       CASE WHEN meeting.provider_join_url IS NULL THEN FALSE ELSE TRUE END
                FROM learning_meetings meeting
                LEFT JOIN meeting_attendance mine
                  ON mine.meeting_id = meeting.id AND mine.user_id = ?
                WHERE meeting.university_id = ? AND meeting.published = TRUE
                  AND meeting.ends_at > ?
                ORDER BY meeting.starts_at
                """, (result, row) -> meetingSummary(result),
                current.accountId(), current.universityId(), OffsetDateTime.now().minusDays(1));
    }

    @Transactional(readOnly = true)
    MeetingRoomResponse meetingRoom(Authentication authentication, UUID meetingId) {
        SignedInAccount current = currentIdentity.require(authentication);
        MeetingSummaryResponse summary = meeting(current, meetingId);
        String providerUrl = jdbc.queryForObject("""
                SELECT provider_join_url FROM learning_meetings
                WHERE id = ? AND university_id = ?
                """, String.class, meetingId, current.universityId());
        return new MeetingRoomResponse(summary, participants(current, meetingId),
                meetingMessages(current, meetingId), providerUrl,
                providerUrl == null
                        ? "Secure video provider not connected. Class chat, questions, attendance and hand raising are active."
                        : "Secure video room is available. UniCircle still records only its own attendance and class interactions.");
    }

    @Transactional
    MeetingRoomResponse joinMeeting(Authentication authentication, UUID meetingId) {
        SignedInAccount current = currentIdentity.require(authentication);
        meeting(current, meetingId);
        Integer existing = jdbc.queryForObject("""
                SELECT COUNT(*) FROM meeting_attendance WHERE meeting_id = ? AND user_id = ?
                """, Integer.class, meetingId, current.accountId());
        if (existing != null && existing > 0) {
            jdbc.update("""
                    UPDATE meeting_attendance
                    SET last_joined_at = CURRENT_TIMESTAMP, last_left_at = NULL,
                        join_count = join_count + 1
                    WHERE meeting_id = ? AND user_id = ?
                    """, meetingId, current.accountId());
        } else {
            jdbc.update("""
                    INSERT INTO meeting_attendance (meeting_id, user_id)
                    VALUES (?, ?)
                    """, meetingId, current.accountId());
        }
        return meetingRoom(authentication, meetingId);
    }

    @Transactional
    MeetingRoomResponse leaveMeeting(Authentication authentication, UUID meetingId) {
        SignedInAccount current = currentIdentity.require(authentication);
        int changed = jdbc.update("""
                UPDATE meeting_attendance
                SET last_left_at = CURRENT_TIMESTAMP, hand_raised = FALSE, hand_raised_at = NULL
                WHERE meeting_id = ? AND user_id = ?
                """, meetingId, current.accountId());
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Join the class before leaving it.");
        }
        return meetingRoom(authentication, meetingId);
    }

    @Transactional
    MeetingRoomResponse raiseHand(Authentication authentication, UUID meetingId, boolean raised) {
        SignedInAccount current = currentIdentity.require(authentication);
        int changed = jdbc.update("""
                UPDATE meeting_attendance
                SET hand_raised = ?, hand_raised_at = CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END
                WHERE meeting_id = ? AND user_id = ? AND last_left_at IS NULL
                """, raised, raised, meetingId, current.accountId());
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Join the class before using the raise-hand control.");
        }
        return meetingRoom(authentication, meetingId);
    }

    @Transactional
    MeetingMessageResponse addMeetingMessage(Authentication authentication, UUID meetingId,
            MeetingMessageRequest request) {
        SignedInAccount current = currentIdentity.require(authentication);
        Integer attending = jdbc.queryForObject("""
                SELECT COUNT(*) FROM meeting_attendance
                WHERE meeting_id = ? AND user_id = ? AND last_left_at IS NULL
                """, Integer.class, meetingId, current.accountId());
        if (attending == null || attending == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Join the class before posting in its chat.");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO meeting_messages (id, meeting_id, sender_id, message_type, body)
                VALUES (?, ?, ?, ?, ?)
                """, id, meetingId, current.accountId(), request.messageType(), request.body().strip());
        return jdbc.queryForObject("""
                SELECT message.id, message.sender_id, account.display_name, account.role,
                       message.message_type, message.body, message.sent_at
                FROM meeting_messages message
                JOIN user_accounts account ON account.id = message.sender_id
                WHERE message.id = ?
                """, (result, row) -> meetingMessage(result), id);
    }

    private SupportCaseResponse supportCase(SignedInAccount current, UUID caseId) {
        requireOwnedCase(current, caseId);
        return jdbc.queryForObject("""
                SELECT id, reference_code, category, subject, description, status, priority,
                       assigned_team, created_at, updated_at
                FROM support_cases WHERE id = ?
                """, (result, row) -> supportCaseResponse(result.getObject(1, UUID.class),
                        result.getString(2), result.getString(3), result.getString(4),
                        result.getString(5), result.getString(6), result.getString(7),
                        result.getString(8), result.getObject(9, OffsetDateTime.class),
                        result.getObject(10, OffsetDateTime.class)), caseId);
    }

    private SupportCaseResponse supportCaseResponse(UUID id, String reference, String category,
            String subject, String description, String status, String priority, String team,
            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        List<SupportReplyResponse> replies = jdbc.query("""
                SELECT reply.id, account.display_name, account.role, reply.body, reply.created_at
                FROM support_case_replies reply
                JOIN user_accounts account ON account.id = reply.author_id
                WHERE reply.case_id = ? ORDER BY reply.created_at
                """, (result, row) -> new SupportReplyResponse(
                        result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                        result.getString(4), result.getObject(5, OffsetDateTime.class)), id);
        return new SupportCaseResponse(id, reference, category, subject, description, status,
                priority, team, createdAt, updatedAt, replies,
                "IN_APP_TRACKING — university email connector not configured");
    }

    private void requireOwnedCase(SignedInAccount current, UUID caseId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM support_cases
                WHERE id = ? AND university_id = ? AND reporter_id = ?
                """, Integer.class, caseId, current.universityId(), current.accountId());
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This support request is unavailable.");
        }
    }

    private Publisher requirePublisher(SignedInAccount current) {
        List<Publisher> publishers = jdbc.query("""
                SELECT display_name, role FROM user_accounts
                WHERE id = ? AND university_id = ? AND role IN ('STAFF', 'ADMIN')
                """, (result, row) -> new Publisher(result.getString(1),
                        "ADMIN".equals(result.getString(2))
                                ? "University administrator" : "Verified university staff"),
                current.accountId(), current.universityId());
        if (publishers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A verified staff publishing account is required.");
        }
        return publishers.getFirst();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private MeetingSummaryResponse meeting(SignedInAccount current, UUID meetingId) {
        try {
            return jdbc.queryForObject("""
                    SELECT meeting.id, meeting.host_name, meeting.title, meeting.module_code,
                           meeting.meeting_type, meeting.starts_at, meeting.ends_at,
                           meeting.location_label, meeting.agenda,
                           CASE WHEN mine.user_id IS NULL THEN FALSE ELSE TRUE END,
                           COALESCE(mine.hand_raised, FALSE),
                           (SELECT COUNT(*) FROM meeting_attendance attendee
                            WHERE attendee.meeting_id = meeting.id),
                           CASE WHEN meeting.provider_join_url IS NULL THEN FALSE ELSE TRUE END
                    FROM learning_meetings meeting
                    LEFT JOIN meeting_attendance mine
                      ON mine.meeting_id = meeting.id AND mine.user_id = ?
                    WHERE meeting.id = ? AND meeting.university_id = ? AND meeting.published = TRUE
                    """, (result, row) -> meetingSummary(result),
                    current.accountId(), meetingId, current.universityId());
        } catch (EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This class meeting is unavailable.");
        }
    }

    private MeetingSummaryResponse meetingSummary(java.sql.ResultSet result) throws java.sql.SQLException {
        return new MeetingSummaryResponse(
                result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                result.getString(4), result.getString(5), result.getObject(6, OffsetDateTime.class),
                result.getObject(7, OffsetDateTime.class), result.getString(8), result.getString(9),
                result.getBoolean(10), result.getBoolean(11), result.getInt(12), result.getBoolean(13));
    }

    private List<MeetingParticipantResponse> participants(SignedInAccount current, UUID meetingId) {
        meeting(current, meetingId);
        return jdbc.query("""
                SELECT account.id, account.display_name, account.role,
                       CASE WHEN attendance.last_left_at IS NULL
                                  OR attendance.last_joined_at > attendance.last_left_at
                            THEN TRUE ELSE FALSE END,
                       attendance.hand_raised, attendance.last_joined_at
                FROM meeting_attendance attendance
                JOIN user_accounts account ON account.id = attendance.user_id
                WHERE attendance.meeting_id = ? AND account.university_id = ?
                ORDER BY attendance.hand_raised DESC, account.display_name
                """, (result, row) -> new MeetingParticipantResponse(
                        result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                        result.getBoolean(4), result.getBoolean(5),
                        result.getObject(6, OffsetDateTime.class)), meetingId, current.universityId());
    }

    private List<MeetingMessageResponse> meetingMessages(SignedInAccount current, UUID meetingId) {
        meeting(current, meetingId);
        return jdbc.query("""
                SELECT message.id, message.sender_id, account.display_name, account.role,
                       message.message_type, message.body, message.sent_at
                FROM meeting_messages message
                JOIN user_accounts account ON account.id = message.sender_id
                WHERE message.meeting_id = ? AND account.university_id = ?
                ORDER BY message.sent_at
                """, (result, row) -> meetingMessage(result), meetingId, current.universityId());
    }

    private MeetingMessageResponse meetingMessage(java.sql.ResultSet result) throws java.sql.SQLException {
        return new MeetingMessageResponse(
                result.getObject(1, UUID.class), result.getObject(2, UUID.class),
                result.getString(3), result.getString(4), result.getString(5),
                result.getString(6), result.getObject(7, OffsetDateTime.class));
    }

    private String supportTeam(String category) {
        return switch (category) {
            case "ACADEMIC" -> "Student Advice";
            case "IT" -> "IT Service Desk";
            case "FINANCE" -> "Student Finance";
            case "ACCOMMODATION" -> "Accommodation Team";
            case "WELLBEING" -> "Wellbeing Team";
            case "ACCESSIBILITY" -> "Disability and Inclusion";
            default -> "Student Services";
        };
    }

    private record Publisher(String name, String roleLabel) {
    }
}
