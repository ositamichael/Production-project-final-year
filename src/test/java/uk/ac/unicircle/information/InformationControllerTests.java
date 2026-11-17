package uk.ac.unicircle.information;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class InformationControllerTests {

    private static final UUID ANNOUNCEMENT_ID = id("demo", "announcement-placement");
    private static final UUID MEETING_ID = id("demo", "meeting-lecture");
    private static final String MICHAEL = "michael.carter@demo.university.example";
    private static final String AISHA = "aisha.khan@students.unicircle.test";
    private static final String REVIEWER = "reviewer@demo.university.example";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void informationAreasRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/campus-information/announcements"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/campus-information/support-cases"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/campus-information/meetings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void announcementsAreTargetedAndReadStateIsPerStudent() throws Exception {
        mockMvc.perform(get("/api/campus-information/announcements").with(user(MICHAEL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].category", hasItem("PLACEMENT")))
                .andExpect(jsonPath("$[?(@.id == '%s')].read".formatted(ANNOUNCEMENT_ID))
                        .value(false));

        mockMvc.perform(post("/api/campus-information/announcements/{id}/read", ANNOUNCEMENT_ID)
                        .with(user(MICHAEL)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(get("/api/campus-information/announcements").with(user(AISHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].read".formatted(ANNOUNCEMENT_ID))
                        .value(false));
    }

    @Test
    void onlyVerifiedStaffCanPublishTargetedAnnouncements() throws Exception {
        String announcement = """
                {"category":"MODULE","audienceType":"UNIVERSITY",
                 "title":"Room change for tomorrow's lecture",
                 "body":"The class will meet in the Innovation Hub at 10:00.",
                 "actionLabel":"Open live classes","actionUrl":"#live-classes-page",
                 "pinned":true,"urgent":false}
                """;

        mockMvc.perform(post("/api/campus-information/announcements")
                        .with(user(MICHAEL)).with(csrf())
                        .contentType("application/json").content(announcement))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/campus-information/announcements")
                        .with(user(REVIEWER)).with(csrf())
                        .contentType("application/json").content(announcement))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorName").value("Alex Morgan"))
                .andExpect(jsonPath("$.authorRole").value("Verified university staff"))
                .andExpect(jsonPath("$.audienceLabel").value("All students"));

        mockMvc.perform(get("/api/campus-information/announcements").with(user(MICHAEL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Room change for tomorrow's lecture")));

        Integer notifications = jdbc.queryForObject("""
                SELECT COUNT(*) FROM notifications notification
                JOIN user_accounts account ON account.id = notification.recipient_id
                WHERE account.email = ? AND notification.notification_type = 'ANNOUNCEMENT'
                  AND notification.title = ?
                """, Integer.class, MICHAEL, "Room change for tomorrow's lecture");
        org.assertj.core.api.Assertions.assertThat(notifications).isEqualTo(1);
    }

    @Test
    void supportRequestsAreTrackedPrivatelyPerAccount() throws Exception {
        String response = mockMvc.perform(post("/api/campus-information/support-cases")
                        .with(user(MICHAEL)).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"category":"IT","subject":"Cannot reach module portal",
                                 "description":"The module page returns an access error after sign-in."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedTeam").value("IT Service Desk"))
                .andExpect(jsonPath("$.deliveryMode").value(
                        "IN_APP_TRACKING — university email connector not configured"))
                .andReturn().getResponse().getContentAsString();

        String caseId = response.replaceFirst(".*\"id\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(post("/api/campus-information/support-cases/{id}/replies", caseId)
                        .with(user(MICHAEL)).with(csrf())
                        .contentType("application/json")
                        .content("{\"body\":\"I have also tried another browser.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replies", hasSize(1)));

        mockMvc.perform(get("/api/campus-information/support-cases").with(user(AISHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void liveClassRecordsAttendanceHandRaiseAndQuestions() throws Exception {
        mockMvc.perform(get("/api/campus-information/meetings").with(user(MICHAEL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].title", hasItem("Software Engineering Live Lecture")));

        mockMvc.perform(post("/api/campus-information/meetings/{id}/messages", MEETING_ID)
                        .with(user(MICHAEL)).with(csrf())
                        .contentType("application/json")
                        .content("{\"messageType\":\"QUESTION\",\"body\":\"Can you explain the sprint evidence?\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/campus-information/meetings/{id}/join", MEETING_ID)
                        .with(user(MICHAEL)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meeting.joined").value(true))
                .andExpect(jsonPath("$.participants", hasSize(1)));

        mockMvc.perform(post("/api/campus-information/meetings/{id}/hand", MEETING_ID)
                        .with(user(MICHAEL)).with(csrf())
                        .contentType("application/json")
                        .content("{\"raised\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meeting.handRaised").value(true));

        mockMvc.perform(post("/api/campus-information/meetings/{id}/messages", MEETING_ID)
                        .with(user(MICHAEL)).with(csrf())
                        .contentType("application/json")
                        .content("{\"messageType\":\"QUESTION\",\"body\":\"Can you explain the sprint evidence?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageType").value("QUESTION"));

        Integer attendance = jdbc.queryForObject("""
                SELECT COUNT(*) FROM meeting_attendance WHERE meeting_id = ?
                """, Integer.class, MEETING_ID);
        org.assertj.core.api.Assertions.assertThat(attendance).isEqualTo(1);
    }

    private static UUID id(String namespace, String name) {
        return UUID.nameUUIDFromBytes(
                ("unicircle:" + namespace + ":" + name).getBytes(StandardCharsets.UTF_8));
    }
}
