package uk.ac.unicircle.social;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("!prod")
class DemoSocialDataConfiguration {

    private static final UUID UNIVERSITY_ID = UUID.fromString("2c952dd9-5276-4e44-bf20-3fe98b345101");
    private static final UUID MICHAEL_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be101");
    private static final UUID AISHA_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be102");
    private static final UUID CONVERSATION_ID = UUID.fromString("1fbc8fa5-2b31-447e-85c2-b895d6eef101");

    @Bean
    @Order(30)
    ApplicationRunner seedFictionalConversation(JdbcTemplate jdbc) {
        return arguments -> {
            Integer conversations = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM conversations WHERE id = ?", Integer.class, CONVERSATION_ID);
            if (conversations != null && conversations == 0) {
                jdbc.update("""
                        INSERT INTO conversations
                          (id, university_id, conversation_type, created_by, direct_pair_key, created_at)
                        VALUES (?, ?, 'DIRECT', ?, ?, ?)
                        """, CONVERSATION_ID, UNIVERSITY_ID, MICHAEL_ID,
                        SocialService.pairKey(MICHAEL_ID, AISHA_ID), Timestamp.from(Instant.now().minusSeconds(3600)));
                jdbc.update("INSERT INTO conversation_members (conversation_id, user_id) VALUES (?, ?)",
                        CONVERSATION_ID, MICHAEL_ID);
                jdbc.update("INSERT INTO conversation_members (conversation_id, user_id) VALUES (?, ?)",
                        CONVERSATION_ID, AISHA_ID);
            }

            seedMessage(jdbc, "85e9d4a9-5368-4685-a0da-8e9c0cc6a101", AISHA_ID,
                    "Hi Michael, are you still coming to the library after the seminar?", 2700);
            seedMessage(jdbc, "85e9d4a9-5368-4685-a0da-8e9c0cc6a102", MICHAEL_ID,
                    "Yes, I will be there. I can bring the project notes too.", 2100);
            seedMessage(jdbc, "85e9d4a9-5368-4685-a0da-8e9c0cc6a103", AISHA_ID,
                    "Perfect. I have reserved a table near the study rooms.", 1500);
            seedNotification(jdbc, "9ef264f2-117a-47ba-8937-9d21983fb101", MICHAEL_ID,
                    "CONNECTION_REQUEST", "New connection request",
                    "Sophie Patel would like to connect.", "CONNECTION",
                    UUID.fromString("6fda523f-aa98-47df-a6f0-7d322aa8a102"), 1200);
            seedNotification(jdbc, "9ef264f2-117a-47ba-8937-9d21983fb102", MICHAEL_ID,
                    "MESSAGE", "New message from Aisha Khan",
                    "Perfect. I have reserved a table near the study rooms.", "CONVERSATION",
                    CONVERSATION_ID, 1500);
        };
    }

    private void seedMessage(JdbcTemplate jdbc, String idValue, UUID sender, String body, long secondsAgo) {
        UUID id = UUID.fromString(idValue);
        Integer messages = jdbc.queryForObject("SELECT COUNT(*) FROM messages WHERE id = ?", Integer.class, id);
        if (messages != null && messages == 0) {
            jdbc.update("""
                    INSERT INTO messages
                      (id, university_id, conversation_id, sender_id, body, sent_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, id, UNIVERSITY_ID, CONVERSATION_ID, sender, body,
                    Timestamp.from(Instant.now().minusSeconds(secondsAgo)));
        }
    }

    private void seedNotification(
            JdbcTemplate jdbc,
            String idValue,
            UUID recipient,
            String type,
            String title,
            String body,
            String resourceType,
            UUID resourceId,
            long secondsAgo) {
        UUID id = UUID.fromString(idValue);
        Integer notifications = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE id = ?", Integer.class, id);
        if (notifications != null && notifications == 0) {
            jdbc.update("""
                    INSERT INTO notifications
                      (id, university_id, recipient_id, notification_type, title, body,
                       resource_type, resource_id, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, UNIVERSITY_ID, recipient, type, title, body, resourceType, resourceId,
                    Timestamp.from(Instant.now().minusSeconds(secondsAgo)));
        }
    }
}
