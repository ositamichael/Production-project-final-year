package uk.ac.unicircle.platform;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("pilot")
class PilotCampusDataConfiguration {

    private static final ZoneId CAMPUS_ZONE = ZoneId.of("Europe/London");
    private static final UUID UNIVERSITY_ID = UUID.fromString("2c952dd9-5276-4e44-bf20-3fe98b345111");
    private static final UUID PROGRAMME_ID = UUID.fromString("3d063eea-6387-4f55-a031-4af09f451101");

    @Bean
    @Order(20)
    ApplicationRunner seedPilotCampusCatalogue(JdbcTemplate jdbc) {
        return arguments -> {
            seedUniversity(jdbc);
            seedAcademicContext(jdbc);
            seedEventsAndServices(jdbc);
            seedOpportunities(jdbc);
            enrolExistingPilotMembers(jdbc);
        };
    }

    private void seedUniversity(JdbcTemplate jdbc) {
        if (count(jdbc, "universities", UNIVERSITY_ID) == 0) {
            jdbc.update("""
                    INSERT INTO universities (id, slug, display_name, email_domain, active)
                    VALUES (?, 'pilot-campus', 'UniCircle Campus Pilot', 'pilot.unicircle.invalid', TRUE)
                    """, UNIVERSITY_ID);
        }
    }

    private void seedAcademicContext(JdbcTemplate jdbc) {
        if (count(jdbc, "academic_programmes", PROGRAMME_ID) == 0) {
            jdbc.update("""
                    INSERT INTO academic_programmes
                      (id, university_id, code, title, qualification, active)
                    VALUES (?, ?, 'PILOT', 'Self-declared pilot study', 'Pilot profile', TRUE)
                    """, PROGRAMME_ID, UNIVERSITY_ID);
        }
        module(jdbc, "4e174ffb-7498-4066-b142-5bf10f562101", "UC101", "Campus connections and safety", 4);
        module(jdbc, "4e174ffb-7498-4066-b142-5bf10f562102", "UC201", "Digital collaboration", 5);
        module(jdbc, "4e174ffb-7498-4066-b142-5bf10f562103", "UC301", "Employability and wellbeing", 6);
    }

    private void module(JdbcTemplate jdbc, String idValue, String code, String title, int level) {
        UUID id = UUID.fromString(idValue);
        if (count(jdbc, "academic_modules", id) == 0) {
            jdbc.update("""
                    INSERT INTO academic_modules (id, university_id, code, title, study_level, active)
                    VALUES (?, ?, ?, ?, ?, TRUE)
                    """, id, UNIVERSITY_ID, code, title, level);
        }
        Integer linked = jdbc.queryForObject("""
                SELECT COUNT(*) FROM programme_modules WHERE programme_id = ? AND module_id = ?
                """, Integer.class, PROGRAMME_ID, id);
        if (linked != null && linked == 0) {
            jdbc.update("""
                    INSERT INTO programme_modules (programme_id, module_id, year_of_study)
                    VALUES (?, ?, 1)
                    """, PROGRAMME_ID, id);
        }
    }

    private void seedEventsAndServices(JdbcTemplate jdbc) {
        LocalDate nextMonday = LocalDate.now(CAMPUS_ZONE).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        event(jdbc, "5f28500c-8509-4177-a253-6cf20f673101", "Pilot Welcome Meetup",
                "Meet other pilot testers and explore the UniCircle connection journey.",
                "SOCIAL", "Campus Hub", at(nextMonday, 12, 0), at(nextMonday, 13, 30), 80);
        event(jdbc, "5f28500c-8509-4177-a253-6cf20f673102", "Careers and Placement Drop-in",
                "Bring your CV and discuss placements, internships and graduate preparation.",
                "CAREERS", "Careers Centre", at(nextMonday.plusDays(1), 11, 0),
                at(nextMonday.plusDays(1), 15, 0), 50);
        event(jdbc, "5f28500c-8509-4177-a253-6cf20f673103", "Study Skills Workshop",
                "Practical planning, research and revision techniques for university study.",
                "ACADEMIC", "Library Workshop Room", at(nextMonday.plusDays(2), 14, 0),
                at(nextMonday.plusDays(2), 15, 30), 30);
        event(jdbc, "5f28500c-8509-4177-a253-6cf20f673104", "Wellbeing Check-in",
                "A guided session introducing wellbeing resources and support routes.",
                "WELLBEING", "Student Support Centre", at(nextMonday.plusDays(3), 10, 0),
                at(nextMonday.plusDays(3), 11, 0), 24);

        UUID tutor = service(jdbc, "6a39611d-961a-4288-b364-7df31f784101", "APPOINTMENT",
                "Academic adviser appointment", "Pilot Student Support",
                "A test booking for discussing study planning or academic progress.", "Online or Campus Hub");
        UUID room = service(jdbc, "6a39611d-961a-4288-b364-7df31f784102", "FACILITY",
                "Group study room", "Learning Resources",
                "A collaborative room for up to four pilot members.", "Library, Level 2");
        UUID careers = service(jdbc, "6a39611d-961a-4288-b364-7df31f784103", "CAMPUS",
                "CV review", "Careers Service",
                "A short pilot appointment for CV and application feedback.", "Careers Centre");

        slot(jdbc, "7b4a722e-a72b-4399-c475-8ef42f895101", tutor,
                at(nextMonday, 10, 0), at(nextMonday, 10, 30), 1);
        slot(jdbc, "7b4a722e-a72b-4399-c475-8ef42f895102", tutor,
                at(nextMonday.plusDays(1), 14, 0), at(nextMonday.plusDays(1), 14, 30), 1);
        slot(jdbc, "7b4a722e-a72b-4399-c475-8ef42f895103", room,
                at(nextMonday.plusDays(2), 12, 0), at(nextMonday.plusDays(2), 13, 0), 4);
        slot(jdbc, "7b4a722e-a72b-4399-c475-8ef42f895104", room,
                at(nextMonday.plusDays(3), 15, 0), at(nextMonday.plusDays(3), 16, 0), 4);
        slot(jdbc, "7b4a722e-a72b-4399-c475-8ef42f895105", careers,
                at(nextMonday.plusDays(4), 11, 0), at(nextMonday.plusDays(4), 11, 30), 1);
    }

    private void event(JdbcTemplate jdbc, String idValue, String title, String summary,
            String category, String venue, OffsetDateTime startsAt, OffsetDateTime endsAt, int capacity) {
        UUID id = UUID.fromString(idValue);
        if (count(jdbc, "campus_events", id) == 0) {
            jdbc.update("""
                    INSERT INTO campus_events
                      (id, university_id, title, summary, category, venue, starts_at, ends_at,
                       capacity, published)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)
                    """, id, UNIVERSITY_ID, title, summary, category, venue,
                    Timestamp.from(startsAt.toInstant()), Timestamp.from(endsAt.toInstant()), capacity);
        }
    }

    private UUID service(JdbcTemplate jdbc, String idValue, String type, String title,
            String provider, String description, String location) {
        UUID id = UUID.fromString(idValue);
        if (count(jdbc, "service_offerings", id) == 0) {
            jdbc.update("""
                    INSERT INTO service_offerings
                      (id, university_id, service_type, title, provider_name, description, location)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, id, UNIVERSITY_ID, type, title, provider, description, location);
        }
        return id;
    }

    private void slot(JdbcTemplate jdbc, String idValue, UUID serviceId,
            OffsetDateTime startsAt, OffsetDateTime endsAt, int capacity) {
        UUID id = UUID.fromString(idValue);
        if (count(jdbc, "service_slots", id) == 0) {
            jdbc.update("""
                    INSERT INTO service_slots (id, service_id, starts_at, ends_at, capacity)
                    VALUES (?, ?, ?, ?, ?)
                    """, id, serviceId, Timestamp.from(startsAt.toInstant()),
                    Timestamp.from(endsAt.toInstant()), capacity);
        }
    }

    private void seedOpportunities(JdbcTemplate jdbc) {
        UUID campus = publisher(jdbc, "8c5b833f-b83c-44aa-d586-9fa53fa06101",
                "Pilot Campus Team", "UNIVERSITY", "pilot.unicircle.invalid");
        UUID employer = publisher(jdbc, "8c5b833f-b83c-44aa-d586-9fa53fa06102",
                "Northstar Digital", "EMPLOYER", "northstar.example");
        UUID union = publisher(jdbc, "8c5b833f-b83c-44aa-d586-9fa53fa06103",
                "Pilot Students' Union", "STUDENT_UNION", "union.example");

        opportunity(jdbc, "9d6c9440-c94d-45bb-e697-afb64fb17101", campus,
                "Student Community Ambassador", "Help welcome new pilot members and improve campus connection.",
                "Support test events, collect structured feedback and help members find the right campus service.",
                "JOB", "ON_CAMPUS", "Campus Hub", "6 hours per week", "Paid pilot role",
                "Communication,Teamwork,Event support", "Open to staging student testers.", 28);
        opportunity(jdbc, "9d6c9440-c94d-45bb-e697-afb64fb17102", employer,
                "Software Engineering Placement", "Join a supervised product team for a structured placement.",
                "Work with Java, APIs, automated testing and delivery practices with technical mentoring.",
                "PLACEMENT", "HYBRID", "Leeds and remote", "12 months", "Paid placement",
                "Java,Git,Testing,Problem solving", "Suitable for computing students seeking an industry placement.", 42);
        opportunity(jdbc, "9d6c9440-c94d-45bb-e697-afb64fb17103", union,
                "Peer Welcome Volunteer", "Help students settle into university life.",
                "Join welcome activities, answer practical questions and signpost students to approved support.",
                "VOLUNTEERING", "FLEXIBLE", "Across campus", "Flexible sessions", "Volunteer role",
                "Listening,Inclusivity,Signposting", "Training is provided; no previous experience is required.", 35);
    }

    private UUID publisher(JdbcTemplate jdbc, String idValue, String name, String type, String domain) {
        UUID id = UUID.fromString(idValue);
        if (count(jdbc, "opportunity_publishers", id) == 0) {
            jdbc.update("""
                    INSERT INTO opportunity_publishers
                      (id, university_id, name, publisher_type, website_domain,
                       verification_status, verified_at)
                    VALUES (?, ?, ?, ?, ?, 'VERIFIED', CURRENT_TIMESTAMP)
                    """, id, UNIVERSITY_ID, name, type, domain);
        }
        return id;
    }

    private void opportunity(JdbcTemplate jdbc, String idValue, UUID publisherId,
            String title, String summary, String description, String type, String workMode,
            String location, String hours, String compensation, String skills,
            String eligibility, int closesInDays) {
        UUID id = UUID.fromString(idValue);
        if (count(jdbc, "opportunities", id) == 0) {
            jdbc.update("""
                    INSERT INTO opportunities
                      (id, university_id, publisher_id, title, summary, description,
                       opportunity_type, work_mode, location, hours_label, compensation_label,
                       skills_text, eligibility, closes_at, moderation_status, published_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'APPROVED', CURRENT_TIMESTAMP)
                    """, id, UNIVERSITY_ID, publisherId, title, summary, description, type, workMode,
                    location, hours, compensation, skills, eligibility,
                    Date.valueOf(LocalDate.now(CAMPUS_ZONE).plusDays(closesInDays)));
        }
    }

    private void enrolExistingPilotMembers(JdbcTemplate jdbc) {
        List<UUID> members = jdbc.query("""
                SELECT account.id FROM user_accounts account
                WHERE account.university_id = ?
                  AND NOT EXISTS (SELECT 1 FROM enrolments enrolment WHERE enrolment.user_id = account.id)
                """, (result, row) -> result.getObject(1, UUID.class), UNIVERSITY_ID);
        for (UUID member : members) {
            Integer year = jdbc.queryForObject("""
                    SELECT COALESCE(year_of_study, 1) FROM user_accounts WHERE id = ?
                    """, Integer.class, member);
            jdbc.update("""
                    INSERT INTO enrolments
                      (id, university_id, user_id, programme_id, year_of_study, status, started_on)
                    VALUES (?, ?, ?, ?, ?, 'ACTIVE', CURRENT_DATE)
                    """, UUID.randomUUID(), UNIVERSITY_ID, member, PROGRAMME_ID, year == null ? 1 : year);
        }
    }

    private OffsetDateTime at(LocalDate date, int hour, int minute) {
        return ZonedDateTime.of(date, LocalTime.of(hour, minute), CAMPUS_ZONE).toOffsetDateTime();
    }

    private int count(JdbcTemplate jdbc, String table, UUID id) {
        Integer result = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?",
                Integer.class, id);
        return result == null ? 0 : result;
    }
}
