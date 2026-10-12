package uk.ac.unicircle.information;

import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("!prod")
class CampusInformationDataConfiguration {

    private static final ZoneId CAMPUS_ZONE = ZoneId.of("Europe/London");
    private static final UUID DEMO_UNIVERSITY = UUID.fromString("2c952dd9-5276-4e44-bf20-3fe98b345101");
    private static final UUID PILOT_UNIVERSITY = UUID.fromString("2c952dd9-5276-4e44-bf20-3fe98b345111");
    private static final UUID DEMO_STAFF = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be105");

    @Bean
    @Order(120)
    ApplicationRunner seedCampusInformation(JdbcTemplate jdbc,
            @Value("${unicircle.meetings.provider-base-url:}") String meetingProviderBaseUrl) {
        return arguments -> {
            if (exists(jdbc, "universities", DEMO_UNIVERSITY)) {
                seedUniversity(jdbc, DEMO_UNIVERSITY, DEMO_STAFF, "Alex Morgan",
                        "Academic Services", "demo", meetingProviderBaseUrl);
            }
            if (exists(jdbc, "universities", PILOT_UNIVERSITY)) {
                seedUniversity(jdbc, PILOT_UNIVERSITY, null, "Pilot Campus Team",
                        "Verified pilot publisher", "pilot", meetingProviderBaseUrl);
            }
        };
    }

    private void seedUniversity(JdbcTemplate jdbc, UUID universityId, UUID staffId,
            String authorName, String authorRole, String namespace, String meetingProviderBaseUrl) {
        announcement(jdbc, id(namespace, "announcement-urgent"), universityId, staffId,
                authorName, authorRole, "URGENT", "UNIVERSITY", "All students",
                "Essential systems maintenance this Wednesday",
                "The student portal will be unavailable from 20:00 to 22:00. Save your work before the maintenance window and contact the IT Service Desk through UniCircle if access problems continue afterwards.",
                "Open support", "#wellbeing-page", true, true, 0);
        announcement(jdbc, id(namespace, "announcement-placement"), universityId, staffId,
                "Careers and Employability", "Verified university service", "PLACEMENT",
                "UNIVERSITY", "All students", "New software engineering placements are open",
                "Applications are now open for supervised year-long placements. Attend the CV clinic, review the eligibility details and save suitable roles in your opportunity tracker.",
                "View placements", "#opportunities-page", true, false, 1);
        announcement(jdbc, id(namespace, "announcement-internship"), universityId, staffId,
                "Careers and Employability", "Verified university service", "INTERNSHIP",
                "UNIVERSITY", "All students", "Summer internship application workshop",
                "Join a practical session on searching for internships, adapting a CV and preparing evidence-based application answers. Booking is available through Campus Events.",
                "View events", "#events-page", false, false, 2);
        announcement(jdbc, id(namespace, "announcement-module"), universityId, staffId,
                "Dr Taylor", "Module lecturer", "MODULE", "UNIVERSITY", "All students",
                "This week’s lecture materials and project clinic",
                "Lecture notes are available before class. Bring one project question to the live clinic and use the class Q&A area so answers remain visible to everyone attending.",
                "Open live classes", "#live-classes-page", false, false, 3);

        LocalDate nextMonday = LocalDate.now(CAMPUS_ZONE)
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        meeting(jdbc, id(namespace, "meeting-lecture"), universityId, staffId, "Dr Taylor",
                "Software Engineering Live Lecture", namespace.equals("demo") ? "SE6001" : "UC201",
                "LECTURE", at(nextMonday, 10, 0), at(nextMonday, 11, 30),
                "Requirements, architecture decisions and sprint evidence. Questions can be posted throughout the class.",
                providerUrl(meetingProviderBaseUrl, "unicircle-" + namespace + "-software-engineering"));
        meeting(jdbc, id(namespace, "meeting-project"), universityId, null, "Project Team",
                "Final Year Project Team Meeting", namespace.equals("demo") ? "CP6002" : "UC301",
                "PROJECT", at(nextMonday.plusDays(1), 14, 0), at(nextMonday.plusDays(1), 15, 0),
                "Weekly check-in: completed work, blockers, decisions and next actions.",
                providerUrl(meetingProviderBaseUrl, "unicircle-" + namespace + "-project-team"));
        meeting(jdbc, id(namespace, "meeting-dropin"), universityId, staffId, "Careers Team",
                "Placement and Internship Drop-in", null, "DROP_IN",
                at(nextMonday.plusDays(2), 12, 0), at(nextMonday.plusDays(2), 13, 0),
                "Ask questions about eligibility, applications, interviews and workplace support.",
                providerUrl(meetingProviderBaseUrl, "unicircle-" + namespace + "-careers-dropin"));
    }

    private void announcement(JdbcTemplate jdbc, UUID id, UUID universityId, UUID authorId,
            String authorName, String authorRole, String category, String audienceType,
            String audienceLabel, String title, String body, String actionLabel, String actionUrl,
            boolean pinned, boolean urgent, int daysAgo) {
        if (exists(jdbc, "campus_announcements", id)) return;
        jdbc.update("""
                INSERT INTO campus_announcements
                  (id, university_id, author_id, author_name, author_role, category,
                   audience_type, audience_label, title, body, action_label, action_url,
                   pinned, urgent, published_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, universityId, authorId, authorName, authorRole, category,
                audienceType, audienceLabel, title, body, actionLabel, actionUrl,
                pinned, urgent, Timestamp.from(OffsetDateTime.now().minusDays(daysAgo).toInstant()));
    }

    private void meeting(JdbcTemplate jdbc, UUID id, UUID universityId, UUID hostId,
            String hostName, String title, String moduleCode, String meetingType,
            OffsetDateTime startsAt, OffsetDateTime endsAt, String agenda, String providerUrl) {
        if (exists(jdbc, "learning_meetings", id)) return;
        jdbc.update("""
                INSERT INTO learning_meetings
                  (id, university_id, host_id, host_name, title, module_code, meeting_type,
                   starts_at, ends_at, lobby_opens_at, agenda, provider_join_url)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, universityId, hostId, hostName, title, moduleCode, meetingType,
                Timestamp.from(startsAt.toInstant()), Timestamp.from(endsAt.toInstant()),
                Timestamp.from(startsAt.minusMinutes(15).toInstant()), agenda, providerUrl);
    }

    private OffsetDateTime at(LocalDate date, int hour, int minute) {
        return ZonedDateTime.of(date, LocalTime.of(hour, minute), CAMPUS_ZONE).toOffsetDateTime();
    }

    private UUID id(String namespace, String name) {
        return UUID.nameUUIDFromBytes(
                ("unicircle:" + namespace + ":" + name).getBytes(StandardCharsets.UTF_8));
    }

    private boolean exists(JdbcTemplate jdbc, String table, UUID id) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?",
                Integer.class, id);
        return count != null && count > 0;
    }

    private String providerUrl(String baseUrl, String roomName) {
        if (baseUrl == null || baseUrl.isBlank()) return null;
        return baseUrl.replaceAll("/+$", "") + "/" + roomName;
    }
}
