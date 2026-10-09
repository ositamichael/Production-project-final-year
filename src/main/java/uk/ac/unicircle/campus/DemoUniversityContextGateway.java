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
@Profile("!prod")
class DemoUniversityContextGateway implements UniversityContextGateway {

    private static final String SOURCE = "FICTIONAL_DEVELOPMENT_FIXTURE";
    private static final String NOTICE = "Fictional sandbox records for development only — not supplied by Leeds Beckett University or another live university system.";

    private final JdbcTemplate jdbc;

    DemoUniversityContextGateway(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UniversityContextResponse load(UUID accountId, UUID universityId) {
        StudentRow student = jdbc.query("""
                SELECT un.display_name, ua.display_name, ua.email, ap.id, ap.code,
                       ap.title, ap.qualification, e.year_of_study
                FROM enrolments e
                JOIN user_accounts ua ON ua.id = e.user_id AND ua.university_id = e.university_id
                JOIN universities un ON un.id = e.university_id
                JOIN academic_programmes ap ON ap.id = e.programme_id AND ap.university_id = e.university_id
                WHERE e.user_id = ? AND e.university_id = ? AND e.status = 'ACTIVE'
                """, (result, row) -> new StudentRow(
                        result.getString(1), result.getString(2), result.getString(3),
                        result.getObject(4, UUID.class), result.getString(5), result.getString(6),
                        result.getString(7), result.getInt(8)), accountId, universityId)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active academic record is available for this signed-in account."));

        List<ModuleSummary> modules = jdbc.query("""
                SELECT am.code, am.title, am.study_level
                FROM programme_modules pm
                JOIN academic_modules am ON am.id = pm.module_id
                WHERE pm.programme_id = ? AND pm.year_of_study = ?
                  AND am.university_id = ? AND am.active = TRUE
                ORDER BY am.code
                """, (result, row) -> new ModuleSummary(
                        result.getString(1), result.getString(2), result.getInt(3)),
                student.programmeId(), student.yearOfStudy(), universityId);

        List<String> sharedModuleCodes = modules.stream().map(ModuleSummary::code).toList();
        List<CoursemateSummary> coursemates = jdbc.query("""
                SELECT ua.id, ua.display_name, ap.title, e.year_of_study,
                       CASE
                         WHEN c.status = 'ACCEPTED' THEN 'CONNECTED'
                         WHEN c.status = 'PENDING' THEN 'PENDING'
                         ELSE 'AVAILABLE_TO_CONNECT'
                       END AS connection_state,
                       c.id,
                       CASE
                         WHEN c.id IS NULL THEN 'NONE'
                         WHEN c.requester_id = ? THEN 'OUTGOING'
                         ELSE 'INCOMING'
                       END AS connection_direction
                FROM enrolments e
                JOIN user_accounts ua ON ua.id = e.user_id AND ua.university_id = e.university_id
                JOIN academic_programmes ap ON ap.id = e.programme_id
                LEFT JOIN connections c
                  ON c.university_id = e.university_id
                 AND ((c.requester_id = ? AND c.addressee_id = ua.id)
                   OR (c.requester_id = ua.id AND c.addressee_id = ?))
                 AND c.status IN ('PENDING', 'ACCEPTED')
                WHERE e.university_id = ? AND e.programme_id = ? AND e.status = 'ACTIVE'
                  AND e.user_id <> ? AND ua.profile_visibility <> 'PRIVATE'
                ORDER BY ua.display_name
                """, (result, row) -> new CoursemateSummary(
                        result.getObject(1, UUID.class), result.getString(2), result.getString(3), result.getInt(4),
                        result.getString(5), result.getObject(6, UUID.class), result.getString(7), sharedModuleCodes),
                accountId, accountId, accountId, universityId, student.programmeId(), accountId);

        return new UniversityContextResponse(
                SOURCE,
                NOTICE,
                new UniversitySummary(student.universityName()),
                new StudentSummary(student.displayName(), student.email(), student.programmeCode(),
                        student.programmeTitle(), student.qualification(), student.yearOfStudy()),
                modules,
                coursemates);
    }

    private record StudentRow(
            String universityName,
            String displayName,
            String email,
            UUID programmeId,
            String programmeCode,
            String programmeTitle,
            String qualification,
            int yearOfStudy) {
    }
}
