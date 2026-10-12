package uk.ac.unicircle.lostfound;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("!prod & !pilot")
class DemoLostFoundDataConfiguration {

    private static final UUID UNIVERSITY_ID = UUID.fromString("2c952dd9-5276-4e44-bf20-3fe98b345101");
    private static final UUID MICHAEL_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be101");
    private static final UUID AISHA_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be102");
    private static final UUID JAMES_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be103");
    private static final UUID SOPHIE_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be104");

    @Bean
    @Order(60)
    ApplicationRunner seedFictionalLostFoundReports(JdbcTemplate jdbc) {
        return arguments -> {
            report(jdbc, "4ddab979-7772-4a1c-9ded-6fb73123f101", MICHAEL_ID, "FOUND", "BAGS",
                    "Black laptop sleeve", "Black", "Plain padded laptop sleeve with a front pocket.",
                    "Small stitched initials inside the front pocket.", "Library, second floor",
                    0, "SERVICE_DESK");
            report(jdbc, "4ddab979-7772-4a1c-9ded-6fb73123f102", AISHA_ID, "FOUND", "KEYS",
                    "Blue key lanyard", "Blue", "Blue fabric lanyard with several keys attached.",
                    "Owner should identify the number and type of keys.", "Student Hub reception",
                    1, "WITH_REPORTER");
            report(jdbc, "4ddab979-7772-4a1c-9ded-6fb73123f103", SOPHIE_ID, "FOUND", "ACCESSORIES",
                    "Reusable water bottle", "Orange", "Insulated reusable bottle with a carry loop.",
                    "Owner should describe the sticker on the base.", "Sports Centre",
                    2, "CAMPUS_SECURITY");
            report(jdbc, "4ddab979-7772-4a1c-9ded-6fb73123f104", JAMES_ID, "LOST", "BAGS",
                    "Grey backpack", "Grey", "Medium grey backpack last seen after an afternoon seminar.",
                    "Contains a distinctive notebook; identifying details withheld.", "Engineering Building",
                    3, "NOT_APPLICABLE");
        };
    }

    private void report(
            JdbcTemplate jdbc,
            String idValue,
            UUID reporter,
            String type,
            String category,
            String item,
            String colour,
            String publicDescription,
            String privateDetails,
            String location,
            int daysAgo,
            String custody) {
        UUID id = UUID.fromString(idValue);
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM lost_found_reports WHERE id = ?", Integer.class, id);
        if (existing != null && existing == 0) {
            jdbc.update("""
                    INSERT INTO lost_found_reports
                      (id, university_id, reporter_id, report_type, category, item_name, colour,
                       public_description, private_identifying_details, last_seen_location, occurred_on,
                       custody_status, moderation_status, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'APPROVED', ?)
                    """, id, UNIVERSITY_ID, reporter, type, category, item, colour,
                    publicDescription, privateDetails, location, Date.valueOf(LocalDate.now().minusDays(daysAgo)),
                    custody, Timestamp.from(Instant.now().minusSeconds(daysAgo * 86_400L + 900)));
        }
    }
}
