package uk.ac.unicircle.campus;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import uk.ac.unicircle.campus.UniversityContextResponse.CoursemateSummary;
import uk.ac.unicircle.campus.UniversityContextResponse.ModuleSummary;
import uk.ac.unicircle.campus.UniversityContextResponse.StudentSummary;
import uk.ac.unicircle.campus.UniversityContextResponse.UniversitySummary;

@Component
@Profile("pilot")
class PilotUniversityContextGateway implements UniversityContextGateway {

    private static final String SOURCE = "SELF_DECLARED_PILOT_PROFILE";
    private static final String NOTICE = "Course and year are supplied by pilot members and are not verified university records.";

    private final JdbcTemplate jdbc;

    PilotUniversityContextGateway(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UniversityContextResponse load(UUID accountId, UUID universityId) {
        StudentRow student = jdbc.query("""
                SELECT university.display_name, account.display_name, account.email,
                       COALESCE(account.course_name, 'Course not added'),
                       COALESCE(account.year_of_study, 1)
                FROM user_accounts account
                JOIN universities university ON university.id = account.university_id
                WHERE account.id = ? AND account.university_id = ?
                """, (result, row) -> new StudentRow(
                        result.getString(1), result.getString(2), result.getString(3),
                        result.getString(4), result.getInt(5)), accountId, universityId)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The pilot profile is unavailable."));

        List<ModuleSummary> modules = jdbc.query("""
                SELECT code, title, study_level
                FROM academic_modules
                WHERE university_id = ? AND active = TRUE
                ORDER BY code
                """, (result, row) -> new ModuleSummary(
                        result.getString(1), result.getString(2), result.getInt(3)), universityId);

        List<String> sharedModuleCodes = modules.stream().map(ModuleSummary::code).toList();
        List<CoursemateSummary> coursemates = jdbc.query("""
                SELECT other.id, other.display_name, COALESCE(other.course_name, 'Course not added'),
                       COALESCE(other.year_of_study, 1),
                       CASE
                         WHEN connection.status = 'ACCEPTED' THEN 'CONNECTED'
                         WHEN connection.status = 'PENDING' THEN 'PENDING'
                         ELSE 'AVAILABLE_TO_CONNECT'
                       END,
                       connection.id,
                       CASE
                         WHEN connection.id IS NULL THEN 'NONE'
                         WHEN connection.requester_id = ? THEN 'OUTGOING'
                         ELSE 'INCOMING'
                       END
                FROM user_accounts other
                LEFT JOIN connections connection
                  ON connection.university_id = other.university_id
                 AND ((connection.requester_id = ? AND connection.addressee_id = other.id)
                   OR (connection.requester_id = other.id AND connection.addressee_id = ?))
                 AND connection.status IN ('PENDING', 'ACCEPTED')
                WHERE other.university_id = ? AND other.id <> ?
                  AND other.profile_visibility <> 'PRIVATE'
                  AND LOWER(COALESCE(other.course_name, '')) = LOWER(?)
                  AND COALESCE(other.course_name, '') <> ''
                ORDER BY other.display_name
                """, (result, row) -> new CoursemateSummary(
                        result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                        result.getInt(4), result.getString(5), result.getObject(6, UUID.class),
                        result.getString(7), sharedModuleCodes),
                accountId, accountId, accountId, universityId, accountId, student.courseName());

        return new UniversityContextResponse(
                SOURCE,
                NOTICE,
                new UniversitySummary(student.universityName()),
                new StudentSummary(student.displayName(), student.email(), "PILOT",
                        student.courseName(), "Self-declared pilot profile", student.yearOfStudy()),
                modules,
                coursemates);
    }

    private record StudentRow(String universityName, String displayName, String email,
            String courseName, int yearOfStudy) {
    }
}
