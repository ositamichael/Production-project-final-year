package uk.ac.unicircle.campus;

import java.sql.Date;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("!prod")
class DemoAcademicDataConfiguration {

    private static final UUID UNIVERSITY_ID = UUID.fromString("2c952dd9-5276-4e44-bf20-3fe98b345101");
    private static final UUID MICHAEL_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be101");
    private static final UUID PROGRAMME_ID = UUID.fromString("31aa46af-f735-46e0-b9bf-e1d2e8a8a101");

    @Bean
    @Order(20)
    ApplicationRunner seedFictionalAcademicContext(JdbcTemplate jdbc) {
        return arguments -> {
            insertProgramme(jdbc);
            insertModule(jdbc, "41a21719-c76e-4108-902c-94815aa0a101", "UC-L6-ASE", "Advanced Software Engineering");
            insertModule(jdbc, "41a21719-c76e-4108-902c-94815aa0a102", "UC-L6-PRJ", "Production Project");
            insertModule(jdbc, "41a21719-c76e-4108-902c-94815aa0a103", "UC-L6-CSP", "Cloud and Secure Platforms");

            insertStudent(jdbc, "7ccb9282-ebcb-4ea0-94f1-3af1f84be102", "fictional-aisha",
                    "aisha.khan@students.unicircle.test", "Aisha Khan");
            insertStudent(jdbc, "7ccb9282-ebcb-4ea0-94f1-3af1f84be103", "fictional-james",
                    "james.wilson@students.unicircle.test", "James Wilson");
            insertStudent(jdbc, "7ccb9282-ebcb-4ea0-94f1-3af1f84be104", "fictional-sophie",
                    "sophie.patel@students.unicircle.test", "Sophie Patel");

            insertEnrolment(jdbc, MICHAEL_ID, "5dda1c87-cc59-4ac3-9168-079b7806a101");
            insertEnrolment(jdbc, UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be102"), "5dda1c87-cc59-4ac3-9168-079b7806a102");
            insertEnrolment(jdbc, UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be103"), "5dda1c87-cc59-4ac3-9168-079b7806a103");
            insertEnrolment(jdbc, UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be104"), "5dda1c87-cc59-4ac3-9168-079b7806a104");

            insertConnection(jdbc, "6fda523f-aa98-47df-a6f0-7d322aa8a101", MICHAEL_ID,
                    UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be102"), "ACCEPTED");
            insertConnection(jdbc, "6fda523f-aa98-47df-a6f0-7d322aa8a102",
                    UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be104"), MICHAEL_ID, "PENDING");
        };
    }

    private void insertProgramme(JdbcTemplate jdbc) {
        if (count(jdbc, "academic_programmes", PROGRAMME_ID) == 0) {
            jdbc.update("""
                    INSERT INTO academic_programmes
                      (id, university_id, code, title, qualification, active)
                    VALUES (?, ?, ?, ?, ?, TRUE)
                    """, PROGRAMME_ID, UNIVERSITY_ID, "UC-BSC-CS", "Computer Science", "BSc (Hons)");
        }
    }

    private void insertModule(JdbcTemplate jdbc, String idValue, String code, String title) {
        UUID id = UUID.fromString(idValue);
        if (count(jdbc, "academic_modules", id) == 0) {
            jdbc.update("""
                    INSERT INTO academic_modules (id, university_id, code, title, study_level, active)
                    VALUES (?, ?, ?, ?, 6, TRUE)
                    """, id, UNIVERSITY_ID, code, title);
            jdbc.update("""
                    INSERT INTO programme_modules (programme_id, module_id, year_of_study)
                    VALUES (?, ?, 3)
                    """, PROGRAMME_ID, id);
        }
    }

    private void insertStudent(JdbcTemplate jdbc, String idValue, String subject, String email, String name) {
        UUID id = UUID.fromString(idValue);
        if (count(jdbc, "user_accounts", id) == 0) {
            jdbc.update("""
                    INSERT INTO user_accounts
                      (id, university_id, external_subject, email, display_name, role,
                       profile_visibility, biography, course_name, year_of_study, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, 'STUDENT', 'UNIVERSITY_ONLY', ?, 'BSc (Hons) Computer Science', 3,
                            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, id, UNIVERSITY_ID, subject, email, name,
                    "Fictional student account used only for the UniCircle development sandbox.");
        }
    }

    private void insertEnrolment(JdbcTemplate jdbc, UUID userId, String enrolmentIdValue) {
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM enrolments WHERE user_id = ? AND programme_id = ?", Integer.class,
                userId, PROGRAMME_ID);
        if (existing != null && existing == 0) {
            jdbc.update("""
                    INSERT INTO enrolments
                      (id, university_id, user_id, programme_id, year_of_study, status, started_on, expected_end_on)
                    VALUES (?, ?, ?, ?, 3, 'ACTIVE', ?, ?)
                    """, UUID.fromString(enrolmentIdValue), UNIVERSITY_ID, userId, PROGRAMME_ID,
                    Date.valueOf(LocalDate.of(2024, 9, 23)), Date.valueOf(LocalDate.of(2027, 6, 30)));
        }
    }

    private void insertConnection(JdbcTemplate jdbc, String idValue, UUID requester, UUID addressee, String status) {
        UUID id = UUID.fromString(idValue);
        if (count(jdbc, "connections", id) == 0) {
            jdbc.update("""
                    INSERT INTO connections
                      (id, university_id, requester_id, addressee_id, status, created_at, responded_at)
                    VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP,
                            CASE WHEN ? = 'ACCEPTED' THEN CURRENT_TIMESTAMP ELSE NULL END)
                    """, id, UNIVERSITY_ID, requester, addressee, status, status);
        }
    }

    private int count(JdbcTemplate jdbc, String table, UUID id) {
        Integer result = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id);
        return result == null ? 0 : result;
    }
}
