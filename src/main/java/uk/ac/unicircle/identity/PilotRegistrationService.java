package uk.ac.unicircle.identity;

import java.util.Locale;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("pilot")
class PilotRegistrationService {

    private static final UUID PILOT_UNIVERSITY_ID = UUID.fromString("2c952dd9-5276-4e44-bf20-3fe98b345111");
    private static final UUID PILOT_PROGRAMME_ID = UUID.fromString("3d063eea-6387-4f55-a031-4af09f451101");

    private final UniversityRepository universities;
    private final UserAccountRepository accounts;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    PilotRegistrationService(UniversityRepository universities, UserAccountRepository accounts,
            JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.universities = universities;
        this.accounts = accounts;
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    void register(PilotRegistrationController.RegistrationRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (accounts.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account already exists for this email.");
        }
        University pilot = universities.findById(PILOT_UNIVERSITY_ID)
                .orElseGet(() -> universities.saveAndFlush(new University(PILOT_UNIVERSITY_ID,
                        "pilot-campus", "UniCircle Campus Pilot", "pilot.unicircle.invalid", null)));
        UserAccount account = accounts.saveAndFlush(new UserAccount(UUID.randomUUID(), pilot,
                "pilot-" + UUID.randomUUID(), email, request.displayName().strip(), UserRole.STUDENT));
        account.updateProfile(request.displayName(), null, request.courseName(), request.yearOfStudy(),
                ProfileVisibility.UNIVERSITY_ONLY);
        accounts.saveAndFlush(account);
        jdbc.update("INSERT INTO pilot_credentials (account_id, password_hash) VALUES (?, ?)",
                account.id(), passwordEncoder.encode(request.password()));
        jdbc.update("""
                INSERT INTO enrolments
                  (id, university_id, user_id, programme_id, year_of_study, status, started_on)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', CURRENT_DATE)
                """, UUID.randomUUID(), PILOT_UNIVERSITY_ID, account.id(), PILOT_PROGRAMME_ID,
                request.yearOfStudy());
        createCatchUpNotifications(account.id(), request.courseName().strip(), request.yearOfStudy());
    }

    private void createCatchUpNotifications(UUID accountId, String courseName, int yearOfStudy) {
        List<PublishedRecord> records = new java.util.ArrayList<>();
        records.addAll(jdbc.query("""
                SELECT announcement.id, 'ANNOUNCEMENT', 'Campus update', announcement.title
                FROM campus_announcements announcement
                WHERE announcement.university_id = ?
                  AND (announcement.expires_at IS NULL OR announcement.expires_at > CURRENT_TIMESTAMP)
                  AND (
                    announcement.audience_type = 'UNIVERSITY'
                    OR (announcement.audience_type = 'COURSE'
                        AND LOWER(announcement.audience_label) = LOWER(?))
                    OR (announcement.audience_type = 'YEAR'
                        AND announcement.audience_label = CAST(? AS VARCHAR))
                    OR (announcement.audience_type = 'MODULE' AND EXISTS (
                        SELECT 1 FROM programme_modules link
                        JOIN academic_modules module ON module.id = link.module_id
                        WHERE link.programme_id = ?
                          AND (LOWER(module.code) = LOWER(announcement.audience_label)
                               OR LOWER(module.title) = LOWER(announcement.audience_label))
                    ))
                  )
                """, (result, row) -> publishedRecord(result), PILOT_UNIVERSITY_ID,
                courseName, yearOfStudy, PILOT_PROGRAMME_ID));
        records.addAll(jdbc.query("""
                SELECT id, 'EVENT', 'Upcoming campus event', title
                FROM campus_events
                WHERE university_id = ? AND published = TRUE AND ends_at > CURRENT_TIMESTAMP
                """, (result, row) -> publishedRecord(result), PILOT_UNIVERSITY_ID));
        records.addAll(jdbc.query("""
                SELECT id, 'MEETING', 'Upcoming live class', title
                FROM learning_meetings
                WHERE university_id = ? AND published = TRUE AND access_scope = 'CAMPUS'
                  AND ends_at > CURRENT_TIMESTAMP
                """, (result, row) -> publishedRecord(result), PILOT_UNIVERSITY_ID));
        records.addAll(jdbc.query("""
                SELECT id, 'OPPORTUNITY', 'Campus opportunity', title
                FROM opportunities
                WHERE university_id = ? AND moderation_status = 'APPROVED'
                  AND closes_at >= CURRENT_DATE
                """, (result, row) -> publishedRecord(result), PILOT_UNIVERSITY_ID));
        records.addAll(jdbc.query("""
                SELECT DISTINCT service.id, 'BOOKING', 'Bookable campus service', service.title
                FROM service_offerings service
                JOIN service_slots slot ON slot.service_id = service.id
                WHERE service.university_id = ? AND service.active = TRUE AND slot.active = TRUE
                  AND slot.ends_at > CURRENT_TIMESTAMP
                """, (result, row) -> publishedRecord(result), PILOT_UNIVERSITY_ID));
        records.forEach(record -> jdbc.update("""
                INSERT INTO notifications
                  (id, university_id, recipient_id, notification_type, title, body,
                   resource_type, resource_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), PILOT_UNIVERSITY_ID, accountId, record.type(),
                record.heading(), record.title(), record.type(), record.id()));
    }

    private PublishedRecord publishedRecord(java.sql.ResultSet result) throws java.sql.SQLException {
        return new PublishedRecord(result.getObject(1, UUID.class), result.getString(2),
                result.getString(3), result.getString(4));
    }

    private record PublishedRecord(UUID id, String type, String heading, String title) {
    }
}
