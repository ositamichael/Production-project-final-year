package uk.ac.unicircle.social;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class SocialControllerTests {

    private static final UUID MICHAEL_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be101");
    private static final UUID AISHA_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be102");
    private static final UUID JAMES_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be103");
    private static final UUID SOPHIE_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be104");
    private static final UUID SOPHIE_REQUEST_ID = UUID.fromString("6fda523f-aa98-47df-a6f0-7d322aa8a102");
    private static final UUID AISHA_CONVERSATION_ID = UUID.fromString("1fbc8fa5-2b31-447e-85c2-b895d6eef101");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void socialDataRequiresAnAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/api/social/directory"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/social/conversations"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/social/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void directoryShowsNewAndExistingPermittedStudentsWithConnectionState() throws Exception {
        mockMvc.perform(get("/api/social/directory").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.displayName == 'Aisha Khan')].connectionStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$[?(@.displayName == 'James Wilson')].connectionDirection").value("NONE"))
                .andExpect(jsonPath("$[?(@.displayName == 'Sophie Patel')].connectionDirection").value("INCOMING"));

        mockMvc.perform(get("/api/social/directory").param("query", "James").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accountId").value(JAMES_ID.toString()));
    }

    @Test
    void studentCanRequestAConnectionButCannotCreateAReverseDuplicate() throws Exception {
        mockMvc.perform(post("/api/social/connections/{accountId}", JAMES_ID)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(JAMES_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.direction").value("OUTGOING"));

        mockMvc.perform(post("/api/social/connections/{accountId}", JAMES_ID)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void incomingRequestCanBeAcceptedAndThenUsedToOpenAConversation() throws Exception {
        mockMvc.perform(post("/api/social/connections/{connectionId}/accept", SOPHIE_REQUEST_ID)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(SOPHIE_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(post("/api/social/conversations/direct/{accountId}", SOPHIE_ID)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantDisplayName").value("Sophie Patel"));
    }

    @Test
    void directMessagingIsBlockedUntilAConnectionIsAccepted() throws Exception {
        mockMvc.perform(post("/api/social/conversations/direct/{accountId}", JAMES_ID)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptedConnectionCanReadAndPersistAValidatedMessage() throws Exception {
        mockMvc.perform(get("/api/social/conversations/{conversationId}/messages", AISHA_CONVERSATION_ID)
                        .with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversation.participantAccountId").value(AISHA_ID.toString()))
                .andExpect(jsonPath("$.messages", hasSize(3)));

        mockMvc.perform(post("/api/social/conversations/{conversationId}/messages", AISHA_CONVERSATION_ID)
                        .with(user("student"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"I am on my way now.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("I am on my way now."))
                .andExpect(jsonPath("$.senderAccountId").value(MICHAEL_ID.toString()))
                .andExpect(jsonPath("$.sentByCurrentAccount").value(true));

        Integer stored = jdbc.queryForObject(
                "SELECT COUNT(*) FROM messages WHERE conversation_id = ? AND body = ?",
                Integer.class, AISHA_CONVERSATION_ID, "I am on my way now.");
        org.assertj.core.api.Assertions.assertThat(stored).isEqualTo(1);
    }

    @Test
    void writesRequireCsrfAndMessagesCannotBeBlank() throws Exception {
        mockMvc.perform(post("/api/social/connections/{accountId}", JAMES_ID)
                        .with(user("student")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/social/conversations/{conversationId}/messages", AISHA_CONVERSATION_ID)
                        .with(user("student"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void studentCannotReadAConversationTheyDoNotBelongTo() throws Exception {
        UUID privateConversation = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO conversations
                  (id, university_id, conversation_type, created_by, direct_pair_key)
                VALUES (?, ?, 'DIRECT', ?, ?)
                """, privateConversation, UUID.fromString("2c952dd9-5276-4e44-bf20-3fe98b345101"), AISHA_ID,
                SocialService.pairKey(AISHA_ID, JAMES_ID));
        jdbc.update("INSERT INTO conversation_members (conversation_id, user_id) VALUES (?, ?)",
                privateConversation, AISHA_ID);
        jdbc.update("INSERT INTO conversation_members (conversation_id, user_id) VALUES (?, ?)",
                privateConversation, JAMES_ID);

        mockMvc.perform(get("/api/social/conversations/{conversationId}/messages", privateConversation)
                        .with(user("student")))
                .andExpect(status().isNotFound());
    }

    @Test
    void messagesCreateAccountSpecificNotificationsForTheRecipient() throws Exception {
        mockMvc.perform(post("/api/social/conversations/{conversationId}/messages", AISHA_CONVERSATION_ID)
                        .with(user("student"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"This should arrive live.\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/social/notifications")
                        .with(user("aisha.khan@students.unicircle.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("MESSAGE"))
                .andExpect(jsonPath("$[0].title").value("New message from Michael Carter"))
                .andExpect(jsonPath("$[0].body").value("This should arrive live."))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void blockingImmediatelyRemovesMessagingPermission() throws Exception {
        mockMvc.perform(post("/api/social/safety/block/{accountId}", AISHA_ID)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        mockMvc.perform(post("/api/social/conversations/{conversationId}/messages", AISHA_CONVERSATION_ID)
                        .with(user("student"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"This must not be stored.\"}"))
                .andExpect(status().isNotFound());

        String status = jdbc.queryForObject(
                "SELECT status FROM connections WHERE pair_key = ?", String.class,
                SocialService.pairKey(MICHAEL_ID, AISHA_ID));
        org.assertj.core.api.Assertions.assertThat(status).isEqualTo("BLOCKED");
    }

    @Test
    void confidentialSafetyReportIsValidatedAndPersisted() throws Exception {
        mockMvc.perform(post("/api/social/safety/report/{accountId}", AISHA_ID)
                        .with(user("student"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"SPAM\",\"details\":\"Repeated unwanted messages\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPORTED"));

        Integer reports = jdbc.queryForObject("""
                SELECT COUNT(*) FROM safety_reports
                WHERE reporter_id = ? AND reported_account_id = ? AND reason = 'SPAM'
                """, Integer.class, MICHAEL_ID, AISHA_ID);
        org.assertj.core.api.Assertions.assertThat(reports).isEqualTo(1);

        mockMvc.perform(post("/api/social/safety/report/{accountId}", AISHA_ID)
                        .with(user("student"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"NOT_A_REASON\",\"details\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
