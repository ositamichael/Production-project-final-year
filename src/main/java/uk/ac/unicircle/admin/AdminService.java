package uk.ac.unicircle.admin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import uk.ac.unicircle.admin.AdminRequests.CreateEventRequest;
import uk.ac.unicircle.admin.AdminRequests.CreateMeetingRequest;
import uk.ac.unicircle.admin.AdminRequests.CreateOpportunityRequest;
import uk.ac.unicircle.admin.AdminRequests.CreateServiceRequest;
import uk.ac.unicircle.admin.AdminRequests.CreateSlotRequest;
import uk.ac.unicircle.admin.AdminRequests.UpdateSafetyReportRequest;
import uk.ac.unicircle.admin.AdminRequests.UpdateSupportCaseRequest;
import uk.ac.unicircle.admin.AdminRequests.UpdateUserRequest;
import uk.ac.unicircle.admin.AdminResponses.AuditResponse;
import uk.ac.unicircle.admin.AdminResponses.EventAdminResponse;
import uk.ac.unicircle.admin.AdminResponses.MeetingAdminResponse;
import uk.ac.unicircle.admin.AdminResponses.OpportunityAdminResponse;
import uk.ac.unicircle.admin.AdminResponses.OverviewResponse;
import uk.ac.unicircle.admin.AdminResponses.PublisherResponse;
import uk.ac.unicircle.admin.AdminResponses.SafetyAdminResponse;
import uk.ac.unicircle.admin.AdminResponses.ServiceAdminResponse;
import uk.ac.unicircle.admin.AdminResponses.SlotAdminResponse;
import uk.ac.unicircle.admin.AdminResponses.SupportAdminResponse;
import uk.ac.unicircle.admin.AdminResponses.UserResponse;
import uk.ac.unicircle.identity.CurrentIdentity;
import uk.ac.unicircle.identity.CurrentIdentity.SignedInAccount;

@Service
class AdminService {
    private final CurrentIdentity currentIdentity;
    private final JdbcTemplate jdbc;

    AdminService(CurrentIdentity currentIdentity, JdbcTemplate jdbc) {
        this.currentIdentity = currentIdentity;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    OverviewResponse overview(Authentication authentication) {
        SignedInAccount admin = requireAdmin(authentication);
        UUID universityId = admin.universityId();
        return new OverviewResponse(
                count("SELECT COUNT(*) FROM user_accounts WHERE university_id = ?", universityId),
                count("SELECT COUNT(*) FROM user_accounts WHERE university_id = ? AND account_status = 'ACTIVE'", universityId),
                count("SELECT COUNT(*) FROM connections WHERE university_id = ? AND status = 'PENDING'", universityId),
                count("SELECT COUNT(*) FROM campus_events WHERE university_id = ? AND published = TRUE AND ends_at > CURRENT_TIMESTAMP", universityId),
                count("SELECT COUNT(*) FROM service_offerings WHERE university_id = ? AND active = TRUE", universityId),
                count("SELECT COUNT(*) FROM support_cases WHERE university_id = ? AND status NOT IN ('RESOLVED','CLOSED')", universityId),
                count("SELECT COUNT(*) FROM safety_reports WHERE university_id = ? AND status IN ('OPEN','REVIEWING')", universityId),
                count("SELECT COUNT(*) FROM lost_found_reports WHERE university_id = ? AND moderation_status = 'PENDING'", universityId));
    }

    @Transactional(readOnly = true)
    List<UserResponse> users(Authentication authentication, String query) {
        SignedInAccount admin = requireAdmin(authentication);
        String pattern = "%" + cleanQuery(query) + "%";
        return jdbc.query("""
                SELECT id, display_name, email, role, account_status, course_name, year_of_study, created_at
                FROM user_accounts
                WHERE university_id = ?
                  AND (LOWER(display_name) LIKE ? OR LOWER(email) LIKE ? OR LOWER(COALESCE(course_name, '')) LIKE ?)
                ORDER BY display_name, email
                """, (result, row) -> new UserResponse(
                        result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                        result.getString(4), result.getString(5), result.getString(6),
                        result.getObject(7, Integer.class), result.getObject(8, OffsetDateTime.class)),
                admin.universityId(), pattern, pattern, pattern);
    }

    @Transactional
    UserResponse updateUser(Authentication authentication, UUID accountId, UpdateUserRequest request) {
        SignedInAccount admin = requireAdmin(authentication);
        UserResponse before = findUser(admin, accountId);
        if (admin.accountId().equals(accountId)
                && (!"ADMIN".equals(request.role()) || "SUSPENDED".equals(request.status()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You cannot remove or suspend your own administrator access.");
        }
        if ("ADMIN".equals(before.role()) && !"ADMIN".equals(request.role())
                && activeAdminCount(admin.universityId()) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The test campus must keep at least one active administrator.");
        }
        jdbc.update("""
                UPDATE user_accounts SET role = ?, account_status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND university_id = ?
                """, request.role(), request.status(), accountId, admin.universityId());
        audit(admin, "UPDATE_USER", "USER", accountId,
                before.displayName() + " changed to " + request.role() + " / " + request.status());
        notification(admin.universityId(), accountId, "ADMIN", "Account permissions updated",
                "Your Test Campus role is now " + request.role() + " and account status is " + request.status() + ".",
                "ACCOUNT", accountId);
        return findUser(admin, accountId);
    }

    @Transactional
    EventAdminResponse createEvent(Authentication authentication, CreateEventRequest request) {
        SignedInAccount admin = requireAdmin(authentication);
        requireValidTime(request.startsAt(), request.endsAt());
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO campus_events
                      (id, university_id, organiser_id, title, summary, category, venue,
                       starts_at, ends_at, capacity, published)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, admin.universityId(), admin.accountId(), request.title().strip(),
                    testLabel(request.summary(), 600), request.category(), request.venue().strip(),
                    request.startsAt(), request.endsAt(), request.capacity(), request.published());
        } catch (DataIntegrityViolationException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An event with this title and start time already exists.", duplicate);
        }
        audit(admin, "CREATE_EVENT", "EVENT", id, "Created Test Campus event: " + request.title().strip());
        if (request.published()) notifyCampus(admin, "EVENT", "New Test Campus event",
                request.title().strip() + " is now open for registration.", "EVENT", id);
        return event(admin, id);
    }

    @Transactional
    EventAdminResponse setEventPublished(Authentication authentication, UUID eventId, boolean published) {
        SignedInAccount admin = requireAdmin(authentication);
        int changed = jdbc.update("UPDATE campus_events SET published = ? WHERE id = ? AND university_id = ?",
                published, eventId, admin.universityId());
        if (changed == 0) throw notFound("event");
        audit(admin, published ? "PUBLISH_EVENT" : "UNPUBLISH_EVENT", "EVENT", eventId,
                published ? "Published Test Campus event" : "Unpublished Test Campus event");
        EventAdminResponse event = event(admin, eventId);
        if (published) notifyCampus(admin, "EVENT", "Test Campus event published",
                event.title() + " is now available.", "EVENT", eventId);
        return event;
    }

    @Transactional
    ServiceAdminResponse createService(Authentication authentication, CreateServiceRequest request) {
        SignedInAccount admin = requireAdmin(authentication);
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO service_offerings
                      (id, university_id, organiser_id, service_type, title, provider_name,
                       description, location, active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)
                    """, id, admin.universityId(), admin.accountId(), request.serviceType(),
                    request.title().strip(), request.providerName().strip(),
                    testLabel(request.description(), 600), request.location().strip());
        } catch (DataIntegrityViolationException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A service with this title already exists.", duplicate);
        }
        audit(admin, "CREATE_SERVICE", "SERVICE", id, "Created Test Campus service: " + request.title().strip());
        return service(admin, id);
    }

    @Transactional
    ServiceAdminResponse setServiceActive(Authentication authentication, UUID serviceId, boolean active) {
        SignedInAccount admin = requireAdmin(authentication);
        int changed = jdbc.update("UPDATE service_offerings SET active = ? WHERE id = ? AND university_id = ?",
                active, serviceId, admin.universityId());
        if (changed == 0) throw notFound("service");
        audit(admin, active ? "ACTIVATE_SERVICE" : "DEACTIVATE_SERVICE", "SERVICE", serviceId,
                active ? "Activated Test Campus service" : "Deactivated Test Campus service");
        return service(admin, serviceId);
    }

    @Transactional
    SlotAdminResponse createSlot(Authentication authentication, UUID serviceId, CreateSlotRequest request) {
        SignedInAccount admin = requireAdmin(authentication);
        requireValidTime(request.startsAt(), request.endsAt());
        service(admin, serviceId);
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO service_slots (id, service_id, starts_at, ends_at, capacity, active)
                    VALUES (?, ?, ?, ?, ?, TRUE)
                    """, id, serviceId, request.startsAt(), request.endsAt(), request.capacity());
        } catch (DataIntegrityViolationException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This service already has a slot at that time.", duplicate);
        }
        audit(admin, "CREATE_SLOT", "SERVICE_SLOT", id, "Created Test Campus availability slot");
        return new SlotAdminResponse(id, serviceId, request.startsAt(), request.endsAt(), request.capacity(), true);
    }

    @Transactional
    MeetingAdminResponse createMeeting(Authentication authentication, CreateMeetingRequest request) {
        SignedInAccount admin = requireAdmin(authentication);
        requireValidTime(request.startsAt(), request.endsAt());
        UUID id = UUID.randomUUID();
        String hostName = jdbc.queryForObject("SELECT display_name FROM user_accounts WHERE id = ?",
                String.class, admin.accountId());
        jdbc.update("""
                INSERT INTO learning_meetings
                  (id, university_id, host_id, organiser_id, host_name, title, module_code,
                   meeting_type, starts_at, ends_at, lobby_opens_at, location_label, agenda,
                   provider_join_url, published)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, admin.universityId(), admin.accountId(), admin.accountId(), hostName,
                request.title().strip(), blankToNull(request.moduleCode()), request.meetingType(),
                request.startsAt(), request.endsAt(), request.startsAt().minusMinutes(10),
                request.locationLabel().strip(), blankToNull(request.agenda()),
                blankToNull(request.providerJoinUrl()), request.published());
        Set<UUID> assignees = new LinkedHashSet<>(request.assigneeIds() == null ? List.of() : request.assigneeIds());
        for (UUID accountId : assignees) {
            requireActiveCampusUser(admin, accountId);
            jdbc.update("""
                    INSERT INTO meeting_assignments (meeting_id, user_id, assigned_by)
                    VALUES (?, ?, ?)
                    """, id, accountId, admin.accountId());
            notification(admin.universityId(), accountId, "MEETING", "Test Campus session assigned",
                    request.title().strip() + " has been added to your UniCircle schedule.", "MEETING", id);
        }
        audit(admin, "CREATE_MEETING", "MEETING", id,
                "Created Test Campus session for " + assignees.size() + " assigned user(s)");
        return meeting(admin, id);
    }

    @Transactional(readOnly = true)
    List<PublisherResponse> publishers(Authentication authentication) {
        SignedInAccount admin = requireAdmin(authentication);
        return jdbc.query("""
                SELECT id, name, publisher_type, verification_status
                FROM opportunity_publishers WHERE university_id = ? ORDER BY name
                """, (result, row) -> new PublisherResponse(result.getObject(1, UUID.class),
                        result.getString(2), result.getString(3), result.getString(4)), admin.universityId());
    }

    @Transactional
    OpportunityAdminResponse createOpportunity(Authentication authentication, CreateOpportunityRequest request) {
        SignedInAccount admin = requireAdmin(authentication);
        String publisherName = jdbc.query("""
                SELECT name FROM opportunity_publishers
                WHERE id = ? AND university_id = ? AND verification_status = 'VERIFIED'
                """, (result, row) -> result.getString(1), request.publisherId(), admin.universityId())
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Choose a verified Test Campus publisher."));
        if (!request.closesAt().isAfter(java.time.LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The closing date must be in the future.");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO opportunities
                  (id, university_id, publisher_id, organiser_id, title, summary, description,
                   opportunity_type, work_mode, location, hours_label, compensation_label,
                   skills_text, eligibility, closes_at, moderation_status, published_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, admin.universityId(), request.publisherId(), admin.accountId(),
                request.title().strip(), testLabel(request.summary(), 600), testLabel(request.description(), 2000),
                request.opportunityType(), request.workMode(), request.location().strip(),
                request.hoursLabel().strip(), request.compensationLabel().strip(),
                request.skillsText().strip(), request.eligibility().strip(), request.closesAt(),
                request.published() ? "APPROVED" : "DRAFT",
                request.published() ? OffsetDateTime.now() : null);
        audit(admin, "CREATE_OPPORTUNITY", "OPPORTUNITY", id,
                "Created Test Campus opportunity from " + publisherName);
        if (request.published()) notifyCampus(admin, "OPPORTUNITY", "New Test Campus opportunity",
                request.title().strip() + " is now available.", "OPPORTUNITY", id);
        return opportunity(admin, id);
    }

    @Transactional(readOnly = true)
    List<SupportAdminResponse> supportCases(Authentication authentication) {
        SignedInAccount admin = requireAdmin(authentication);
        return jdbc.query("""
                SELECT support.id, support.reference_code, account.id, account.display_name,
                       support.category, support.subject, support.description, support.status,
                       support.priority, support.assigned_team, support.updated_at
                FROM support_cases support
                JOIN user_accounts account ON account.id = support.reporter_id
                WHERE support.university_id = ?
                ORDER BY CASE support.status WHEN 'OPEN' THEN 1 WHEN 'IN_PROGRESS' THEN 2 ELSE 3 END,
                         support.updated_at DESC
                """, (result, row) -> support(result), admin.universityId());
    }

    @Transactional
    SupportAdminResponse updateSupportCase(Authentication authentication, UUID caseId,
            UpdateSupportCaseRequest request) {
        SignedInAccount admin = requireAdmin(authentication);
        SupportAdminResponse existing = findSupport(admin, caseId);
        jdbc.update("""
                UPDATE support_cases SET status = ?, priority = ?, assigned_team = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND university_id = ?
                """, request.status(), request.priority(), request.assignedTeam().strip(),
                caseId, admin.universityId());
        if (request.reply() != null && !request.reply().isBlank()) {
            jdbc.update("""
                    INSERT INTO support_case_replies (id, case_id, author_id, body)
                    VALUES (?, ?, ?, ?)
                    """, UUID.randomUUID(), caseId, admin.accountId(), request.reply().strip());
        }
        notification(admin.universityId(), existing.reporterId(), "SUPPORT", "Support request updated",
                existing.referenceCode() + " is now " + request.status() + ".", "SUPPORT_CASE", caseId);
        audit(admin, "UPDATE_SUPPORT_CASE", "SUPPORT_CASE", caseId,
                existing.referenceCode() + " changed to " + request.status());
        return findSupport(admin, caseId);
    }

    @Transactional(readOnly = true)
    List<SafetyAdminResponse> safetyReports(Authentication authentication) {
        SignedInAccount admin = requireAdmin(authentication);
        return jdbc.query("""
                SELECT report.id, reporter.id, reporter.display_name,
                       reported.id, reported.display_name, report.reason, report.details,
                       report.status, report.created_at
                FROM safety_reports report
                JOIN user_accounts reporter ON reporter.id = report.reporter_id
                JOIN user_accounts reported ON reported.id = report.reported_account_id
                WHERE report.university_id = ?
                ORDER BY CASE report.status WHEN 'OPEN' THEN 1 WHEN 'REVIEWING' THEN 2 ELSE 3 END,
                         report.created_at DESC
                """, (result, row) -> safety(result), admin.universityId());
    }

    @Transactional
    SafetyAdminResponse updateSafetyReport(Authentication authentication, UUID reportId,
            UpdateSafetyReportRequest request) {
        SignedInAccount admin = requireAdmin(authentication);
        SafetyAdminResponse existing = findSafety(admin, reportId);
        jdbc.update("""
                UPDATE safety_reports SET status = ?, reviewed_at =
                    CASE WHEN ? IN ('RESOLVED','DISMISSED') THEN CURRENT_TIMESTAMP ELSE reviewed_at END
                WHERE id = ? AND university_id = ?
                """, request.status(), request.status(), reportId, admin.universityId());
        notification(admin.universityId(), existing.reporterId(), "SAFETY", "Safety report updated",
                "Your confidential report is now " + request.status() + ".", "REPORT", reportId);
        audit(admin, "UPDATE_SAFETY_REPORT", "SAFETY_REPORT", reportId,
                "Safety report changed to " + request.status()
                        + (request.note() == null || request.note().isBlank() ? "" : ": " + request.note().strip()));
        return findSafety(admin, reportId);
    }

    @Transactional(readOnly = true)
    List<AuditResponse> auditLog(Authentication authentication) {
        SignedInAccount admin = requireAdmin(authentication);
        return jdbc.query("""
                SELECT audit.id, actor.display_name, audit.action_type, audit.resource_type,
                       audit.resource_id, audit.summary, audit.created_at
                FROM admin_audit_log audit
                JOIN user_accounts actor ON actor.id = audit.actor_id
                WHERE audit.university_id = ? ORDER BY audit.created_at DESC LIMIT 100
                """, (result, row) -> new AuditResponse(result.getObject(1, UUID.class),
                        result.getString(2), result.getString(3), result.getString(4),
                        result.getObject(5, UUID.class), result.getString(6),
                        result.getObject(7, OffsetDateTime.class)), admin.universityId());
    }

    private SignedInAccount requireAdmin(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        Integer permitted = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_accounts
                WHERE id = ? AND university_id = ? AND role = 'ADMIN' AND account_status = 'ACTIVE'
                """, Integer.class, current.accountId(), current.universityId());
        if (permitted == null || permitted == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "An active Test Campus administrator account is required.");
        }
        return current;
    }

    private UserResponse findUser(SignedInAccount admin, UUID accountId) {
        return jdbc.query("""
                SELECT id, display_name, email, role, account_status, course_name, year_of_study, created_at
                FROM user_accounts WHERE id = ? AND university_id = ?
                """, (result, row) -> new UserResponse(result.getObject(1, UUID.class), result.getString(2),
                        result.getString(3), result.getString(4), result.getString(5), result.getString(6),
                        result.getObject(7, Integer.class), result.getObject(8, OffsetDateTime.class)),
                accountId, admin.universityId()).stream().findFirst().orElseThrow(() -> notFound("user"));
    }

    private EventAdminResponse event(SignedInAccount admin, UUID id) {
        return jdbc.query("""
                SELECT id, title, category, venue, starts_at, ends_at, capacity, published
                FROM campus_events WHERE id = ? AND university_id = ?
                """, (result, row) -> new EventAdminResponse(result.getObject(1, UUID.class),
                        result.getString(2), result.getString(3), result.getString(4),
                        result.getObject(5, OffsetDateTime.class), result.getObject(6, OffsetDateTime.class),
                        result.getInt(7), result.getBoolean(8)), id, admin.universityId())
                .stream().findFirst().orElseThrow(() -> notFound("event"));
    }

    private ServiceAdminResponse service(SignedInAccount admin, UUID id) {
        return jdbc.query("""
                SELECT id, service_type, title, provider_name, location, active
                FROM service_offerings WHERE id = ? AND university_id = ?
                """, (result, row) -> new ServiceAdminResponse(result.getObject(1, UUID.class),
                        result.getString(2), result.getString(3), result.getString(4),
                        result.getString(5), result.getBoolean(6)), id, admin.universityId())
                .stream().findFirst().orElseThrow(() -> notFound("service"));
    }

    private MeetingAdminResponse meeting(SignedInAccount admin, UUID id) {
        return jdbc.query("""
                SELECT meeting.id, meeting.title, meeting.module_code, meeting.meeting_type,
                       meeting.starts_at, meeting.ends_at, meeting.location_label,
                       (SELECT COUNT(*) FROM meeting_assignments assignment WHERE assignment.meeting_id = meeting.id),
                       meeting.published
                FROM learning_meetings meeting WHERE meeting.id = ? AND meeting.university_id = ?
                """, (result, row) -> new MeetingAdminResponse(result.getObject(1, UUID.class),
                        result.getString(2), result.getString(3), result.getString(4),
                        result.getObject(5, OffsetDateTime.class), result.getObject(6, OffsetDateTime.class),
                        result.getString(7), result.getInt(8), result.getBoolean(9)), id, admin.universityId())
                .stream().findFirst().orElseThrow(() -> notFound("meeting"));
    }

    private OpportunityAdminResponse opportunity(SignedInAccount admin, UUID id) {
        return jdbc.query("""
                SELECT opportunity.id, opportunity.title, opportunity.opportunity_type,
                       publisher.name, opportunity.closes_at, opportunity.moderation_status
                FROM opportunities opportunity
                JOIN opportunity_publishers publisher ON publisher.id = opportunity.publisher_id
                WHERE opportunity.id = ? AND opportunity.university_id = ?
                """, (result, row) -> new OpportunityAdminResponse(result.getObject(1, UUID.class),
                        result.getString(2), result.getString(3), result.getString(4),
                        result.getObject(5, java.time.LocalDate.class), result.getString(6)),
                id, admin.universityId()).stream().findFirst().orElseThrow(() -> notFound("opportunity"));
    }

    private SupportAdminResponse findSupport(SignedInAccount admin, UUID id) {
        return jdbc.query("""
                SELECT support.id, support.reference_code, account.id, account.display_name,
                       support.category, support.subject, support.description, support.status,
                       support.priority, support.assigned_team, support.updated_at
                FROM support_cases support JOIN user_accounts account ON account.id = support.reporter_id
                WHERE support.id = ? AND support.university_id = ?
                """, (result, row) -> support(result), id, admin.universityId())
                .stream().findFirst().orElseThrow(() -> notFound("support request"));
    }

    private SupportAdminResponse support(ResultSet result) throws SQLException {
        return new SupportAdminResponse(result.getObject(1, UUID.class), result.getString(2),
                result.getObject(3, UUID.class), result.getString(4), result.getString(5),
                result.getString(6), result.getString(7), result.getString(8), result.getString(9),
                result.getString(10), result.getObject(11, OffsetDateTime.class));
    }

    private SafetyAdminResponse findSafety(SignedInAccount admin, UUID id) {
        return jdbc.query("""
                SELECT report.id, reporter.id, reporter.display_name,
                       reported.id, reported.display_name, report.reason, report.details,
                       report.status, report.created_at
                FROM safety_reports report
                JOIN user_accounts reporter ON reporter.id = report.reporter_id
                JOIN user_accounts reported ON reported.id = report.reported_account_id
                WHERE report.id = ? AND report.university_id = ?
                """, (result, row) -> safety(result), id, admin.universityId())
                .stream().findFirst().orElseThrow(() -> notFound("safety report"));
    }

    private SafetyAdminResponse safety(ResultSet result) throws SQLException {
        return new SafetyAdminResponse(result.getObject(1, UUID.class),
                result.getObject(2, UUID.class), result.getString(3),
                result.getObject(4, UUID.class), result.getString(5), result.getString(6),
                result.getString(7), result.getString(8), result.getObject(9, OffsetDateTime.class));
    }

    private void requireActiveCampusUser(SignedInAccount admin, UUID accountId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_accounts
                WHERE id = ? AND university_id = ? AND account_status = 'ACTIVE'
                """, Integer.class, accountId, admin.universityId());
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Every assigned account must be an active member of this Test Campus.");
        }
    }

    private long activeAdminCount(UUID universityId) {
        return count("""
                SELECT COUNT(*) FROM user_accounts
                WHERE university_id = ? AND role = 'ADMIN' AND account_status = 'ACTIVE'
                """, universityId);
    }

    private long count(String sql, UUID universityId) {
        Long value = jdbc.queryForObject(sql, Long.class, universityId);
        return value == null ? 0 : value;
    }

    private void requireValidTime(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The end time must be after the start time.");
        }
    }

    private String cleanQuery(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String testLabel(String value, int maximumLength) {
        String clean = value.strip();
        String labelled = clean.startsWith("[TEST CAMPUS]") ? clean : "[TEST CAMPUS] " + clean;
        return labelled.length() <= maximumLength ? labelled : labelled.substring(0, maximumLength);
    }

    private ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "This Test Campus " + resource + " is unavailable.");
    }

    private void audit(SignedInAccount admin, String action, String resourceType,
            UUID resourceId, String summary) {
        jdbc.update("""
                INSERT INTO admin_audit_log
                  (id, university_id, actor_id, action_type, resource_type, resource_id, summary)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), admin.universityId(), admin.accountId(), action,
                resourceType, resourceId, summary);
    }

    private void notification(UUID universityId, UUID recipientId, String type, String title,
            String body, String resourceType, UUID resourceId) {
        jdbc.update("""
                INSERT INTO notifications
                  (id, university_id, recipient_id, notification_type, title, body, resource_type, resource_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), universityId, recipientId, type, title, body, resourceType, resourceId);
    }

    private void notifyCampus(SignedInAccount admin, String type, String title, String body,
            String resourceType, UUID resourceId) {
        List<UUID> recipients = jdbc.query("""
                SELECT id FROM user_accounts
                WHERE university_id = ? AND account_status = 'ACTIVE'
                """, (result, row) -> result.getObject(1, UUID.class), admin.universityId());
        recipients.forEach(accountId -> notification(admin.universityId(), accountId,
                type, title, body, resourceType, resourceId));
    }
}
