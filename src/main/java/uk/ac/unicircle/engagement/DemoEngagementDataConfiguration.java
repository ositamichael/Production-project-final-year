package uk.ac.unicircle.engagement;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("!prod")
class DemoEngagementDataConfiguration {

    private static final ZoneId CAMPUS_ZONE = ZoneId.of("Europe/London");
    private static final UUID UNIVERSITY_ID = UUID.fromString("2c952dd9-5276-4e44-bf20-3fe98b345101");
    private static final UUID MICHAEL_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be101");
    private static final UUID AISHA_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be102");

    @Bean
    @Order(40)
    ApplicationRunner seedFictionalCampusLife(JdbcTemplate jdbc) {
        return arguments -> {
            LocalDate nextMonday = LocalDate.now(CAMPUS_ZONE)
                    .with(TemporalAdjusters.next(DayOfWeek.MONDAY));

            UUID careersEvent = insertEvent(jdbc, "711a6b0d-183b-4d9a-9c82-bbbd78553101",
                    "Spring Careers Fair", "Meet employers, explore placements and receive CV guidance.",
                    "CAREERS", "Main Hall", at(nextMonday, 10, 0), at(nextMonday, 16, 0), 120);
            insertEvent(jdbc, "711a6b0d-183b-4d9a-9c82-bbbd78553102",
                    "Mindfulness Workshop", "A guided session focused on practical study-week stress support.",
                    "WELLBEING", "Wellbeing Centre", at(nextMonday.plusDays(1), 14, 0),
                    at(nextMonday.plusDays(1), 15, 0), 24);
            insertEvent(jdbc, "711a6b0d-183b-4d9a-9c82-bbbd78553103",
                    "International Food Festival", "Student societies share food, music and cultural activities.",
                    "SOCIAL", "Central Plaza", at(nextMonday.plusDays(2), 12, 0),
                    at(nextMonday.plusDays(2), 16, 0), 300);
            insertEvent(jdbc, "711a6b0d-183b-4d9a-9c82-bbbd78553104",
                    "Student Research Showcase", "Final-year students present projects, methods and findings.",
                    "ACADEMIC", "Rose Bowl Atrium", at(nextMonday.plusDays(3), 11, 0),
                    at(nextMonday.plusDays(3), 15, 0), 80);
            insertRegistration(jdbc, careersEvent, MICHAEL_ID);

            UUID lecturer = insertService(jdbc, "8b24013e-b66a-4711-88e2-a8f375675101",
                    "APPOINTMENT", "Lecturer appointment", "Dr Taylor",
                    "Discuss module work, feedback or academic progress.", "Rose Bowl, RB.3.12");
            UUID studyRoom = insertService(jdbc, "8b24013e-b66a-4711-88e2-a8f375675102",
                    "FACILITY", "Library study room", "Learning Resources",
                    "A private collaborative room for up to four students.", "Library, Room 3");
            UUID careers = insertService(jdbc, "8b24013e-b66a-4711-88e2-a8f375675103",
                    "CAMPUS", "Careers guidance", "Careers Service",
                    "CV, placement and graduate-career guidance.", "Student Hub");

            insertSlot(jdbc, "9499216e-640c-4019-866b-9d8ad7d7c101", lecturer,
                    at(nextMonday, 11, 0), at(nextMonday, 11, 30), 1);
            insertSlot(jdbc, "9499216e-640c-4019-866b-9d8ad7d7c102", lecturer,
                    at(nextMonday.plusDays(1), 14, 0), at(nextMonday.plusDays(1), 14, 30), 1);
            UUID sharedRoom = insertSlot(jdbc, "9499216e-640c-4019-866b-9d8ad7d7c103", studyRoom,
                    at(nextMonday, 13, 0), at(nextMonday, 14, 0), 4);
            insertSlot(jdbc, "9499216e-640c-4019-866b-9d8ad7d7c104", studyRoom,
                    at(nextMonday.plusDays(2), 15, 0), at(nextMonday.plusDays(2), 16, 0), 4);
            insertSlot(jdbc, "9499216e-640c-4019-866b-9d8ad7d7c105", careers,
                    at(nextMonday.plusDays(4), 10, 0), at(nextMonday.plusDays(4), 10, 30), 1);
            insertBooking(jdbc, "a1c13a6d-fabe-4214-a0c2-dfc83ce9d101", sharedRoom, AISHA_ID);
        };
    }

    private UUID insertEvent(
            JdbcTemplate jdbc,
            String idValue,
            String title,
            String summary,
            String category,
            String venue,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            int capacity) {
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
        return id;
    }

    private UUID insertService(
            JdbcTemplate jdbc,
            String idValue,
            String type,
            String title,
            String provider,
            String description,
            String location) {
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

    private UUID insertSlot(
            JdbcTemplate jdbc,
            String idValue,
            UUID serviceId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            int capacity) {
        UUID id = UUID.fromString(idValue);
        if (count(jdbc, "service_slots", id) == 0) {
            jdbc.update("""
                    INSERT INTO service_slots (id, service_id, starts_at, ends_at, capacity)
                    VALUES (?, ?, ?, ?, ?)
                    """, id, serviceId, Timestamp.from(startsAt.toInstant()),
                    Timestamp.from(endsAt.toInstant()), capacity);
        }
        return id;
    }

    private void insertRegistration(JdbcTemplate jdbc, UUID eventId, UUID accountId) {
        Integer existing = jdbc.queryForObject("""
                SELECT COUNT(*) FROM event_registrations WHERE event_id = ? AND user_id = ?
                """, Integer.class, eventId, accountId);
        if (existing != null && existing == 0) {
            jdbc.update("INSERT INTO event_registrations (event_id, user_id) VALUES (?, ?)",
                    eventId, accountId);
        }
    }

    private void insertBooking(JdbcTemplate jdbc, String idValue, UUID slotId, UUID accountId) {
        UUID id = UUID.fromString(idValue);
        if (count(jdbc, "service_bookings", id) == 0) {
            jdbc.update("""
                    INSERT INTO service_bookings (id, university_id, slot_id, user_id, status)
                    VALUES (?, ?, ?, ?, 'CONFIRMED')
                    """, id, UNIVERSITY_ID, slotId, accountId);
        }
    }

    private OffsetDateTime at(LocalDate date, int hour, int minute) {
        return ZonedDateTime.of(date, LocalTime.of(hour, minute), CAMPUS_ZONE).toOffsetDateTime();
    }

    private int count(JdbcTemplate jdbc, String table, UUID id) {
        Integer result = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id);
        return result == null ? 0 : result;
    }
}
