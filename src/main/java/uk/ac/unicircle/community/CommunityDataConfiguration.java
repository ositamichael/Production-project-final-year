package uk.ac.unicircle.community;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("!prod")
class CommunityDataConfiguration {

    @Bean
    @Order(60)
    ApplicationRunner seedCampusCircles(JdbcTemplate jdbc) {
        return arguments -> {
            List<UniversityKey> universities = jdbc.query("""
                    SELECT id, slug FROM universities WHERE active = TRUE
                    """, (result, row) -> new UniversityKey(result.getObject(1, UUID.class), result.getString(2)));
            for (UniversityKey university : universities) {
                seed(jdbc, university, "Computer Science Society",
                        "Peer support, technical talks, hackathons and careers sessions.", "ACADEMIC");
                seed(jdbc, university, "Board Games Club",
                        "Weekly meet-ups for strategy, tabletop and casual games.", "SOCIAL");
                seed(jdbc, university, "Sustainable Campus",
                        "Projects and volunteering for a greener university community.", "COMMUNITY");
                seed(jdbc, university, "International Network",
                        "Welcome activities, practical support and cultural exchange.", "COMMUNITY");
                seed(jdbc, university, "Creative Careers",
                        "Portfolio reviews, employer events and creative collaboration.", "CAREERS");
                seed(jdbc, university, "Project Partners",
                        "Find verified students with complementary project skills.", "PROJECTS");
            }
        };
    }

    private void seed(JdbcTemplate jdbc, UniversityKey university, String name, String description, String category) {
        UUID id = UUID.nameUUIDFromBytes(("unicircle:" + university.slug() + ":circle:" + name)
                .getBytes(StandardCharsets.UTF_8));
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM campus_circles WHERE id = ?",
                Integer.class, id);
        if (existing == null || existing == 0) {
            jdbc.update("""
                    INSERT INTO campus_circles
                      (id, university_id, name, description, category)
                    VALUES (?, ?, ?, ?, ?)
                    """, id, university.id(), name, description, category);
        }
    }

    private record UniversityKey(UUID id, String slug) {
    }
}
