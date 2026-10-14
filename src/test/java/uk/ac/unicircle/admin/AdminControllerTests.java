package uk.ac.unicircle.admin;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
class AdminControllerTests {
    private static final String ADMIN = "admin@demo.university.example";
    private static final UUID MICHAEL_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be101");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void administratorWorkspaceIsRoleAndCampusScoped() throws Exception {
        mockMvc.perform(get("/api/admin/overview").with(user("student")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/overview").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isNumber())
                .andExpect(jsonPath("$.activeUsers").isNumber());

        mockMvc.perform(get("/api/admin/users").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].displayName", hasItem("Michael Carter")))
                .andExpect(jsonPath("$[*].displayName", hasItem("Jordan Ellis")));
    }

    @Test
    void createdEventCanBeRegisteredAndAppearsInTheStudentsSchedule() throws Exception {
        OffsetDateTime starts = OffsetDateTime.now(ZoneOffset.UTC).plusDays(8).withSecond(0).withNano(0);
        OffsetDateTime ends = starts.plusHours(2);
        String request = """
                {"title":"Test Campus Collaboration Lab",
                 "summary":"A genuine staging activity organised inside UniCircle for acceptance testing.",
                 "category":"ACADEMIC","venue":"Test Campus Room A",
                 "startsAt":"%s","endsAt":"%s","capacity":2,"published":true}
                """.formatted(starts, ends);

        String response = mockMvc.perform(post("/api/admin/events").with(user(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Campus Collaboration Lab"))
                .andReturn().getResponse().getContentAsString();
        UUID eventId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(response, "$.id"));

        mockMvc.perform(post("/api/campus-life/events/{id}/register", eventId)
                        .with(user("student")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(true));
        mockMvc.perform(get("/api/campus-life/schedule").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Test Campus Collaboration Lab")));

        Integer notices = jdbc.queryForObject("""
                SELECT COUNT(*) FROM notifications
                WHERE recipient_id = ? AND resource_id = ? AND notification_type = 'EVENT'
                """, Integer.class, MICHAEL_ID, eventId);
        org.assertj.core.api.Assertions.assertThat(notices).isGreaterThanOrEqualTo(1);
    }

    @Test
    void assignedMeetingAutomaticallyAppearsInPersonalSchedule() throws Exception {
        OffsetDateTime starts = OffsetDateTime.now(ZoneOffset.UTC).plusDays(9).withSecond(0).withNano(0);
        String request = """
                {"title":"Test Campus Project Review","moduleCode":"TEST6001",
                 "meetingType":"PROJECT","startsAt":"%s","endsAt":"%s",
                 "locationLabel":"UniCircle live room","agenda":"Review the pilot evidence.",
                 "providerJoinUrl":"","audienceType":"SELECTED","audienceLabel":"",
                 "published":true,"assigneeIds":["%s"]}
                """.formatted(starts, starts.plusHours(1), MICHAEL_ID);

        String response = mockMvc.perform(post("/api/admin/meetings").with(user(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedUsers").value(1))
                .andReturn().getResponse().getContentAsString();
        UUID meetingId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(response, "$.id"));

        mockMvc.perform(get("/api/campus-life/schedule").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Test Campus Project Review")));

        mockMvc.perform(get("/api/campus-information/meetings/{id}", meetingId)
                        .with(user("aisha.khan@students.unicircle.test")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/campus-life/schedule")
                        .with(user("aisha.khan@students.unicircle.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title",
                        org.hamcrest.Matchers.not(hasItem("Test Campus Project Review"))));
    }

    @Test
    void administratorCanPublishBookableServicesAndOpportunitiesForStudents() throws Exception {
        OffsetDateTime starts = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10).withSecond(0).withNano(0);
        String serviceResponse = mockMvc.perform(post("/api/admin/services").with(user(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceType":"APPOINTMENT","title":"Test Campus Portfolio Clinic",
                                 "providerName":"Careers Test Team",
                                 "description":"A staging-only appointment for tester acceptance evidence.",
                                 "location":"Test Campus Room B"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Campus Portfolio Clinic"))
                .andReturn().getResponse().getContentAsString();
        UUID serviceId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(serviceResponse, "$.id"));

        String slotResponse = mockMvc.perform(post("/api/admin/services/{id}/slots", serviceId)
                        .with(user(ADMIN)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startsAt":"%s","endsAt":"%s","capacity":1}
                                """.formatted(starts, starts.plusMinutes(30))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID slotId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(slotResponse, "$.id"));

        mockMvc.perform(get("/api/campus-life/services").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Test Campus Portfolio Clinic")));
        mockMvc.perform(post("/api/campus-life/service-slots/{id}/book", slotId)
                        .with(user("student")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(get("/api/campus-life/schedule").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Test Campus Portfolio Clinic")));

        UUID publisherId = jdbc.queryForObject("""
                SELECT id FROM opportunity_publishers
                WHERE verification_status = 'VERIFIED' ORDER BY name LIMIT 1
                """, UUID.class);
        mockMvc.perform(post("/api/admin/opportunities").with(user(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"publisherId":"%s","title":"Test Campus Junior Developer",
                                 "summary":"A clearly labelled staging opportunity.",
                                 "description":"Used to verify publishing, visibility and notification rules.",
                                 "opportunityType":"JOB","workMode":"HYBRID","location":"Test Campus",
                                 "hoursLabel":"8 hours per week","compensationLabel":"Paid test record",
                                 "skillsText":"Java, communication","eligibility":"Current Test Campus students",
                                 "closesAt":"%s","published":true}
                                """.formatted(publisherId, java.time.LocalDate.now().plusDays(21))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Campus Junior Developer"));
        mockMvc.perform(get("/api/opportunities").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Test Campus Junior Developer")));
    }

    @Test
    void administratorCanBuildAndPublishTheCampusInformationStructure() throws Exception {
        String programmeResponse = mockMvc.perform(post("/api/admin/programmes").with(user(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ADMIN-TEST","title":"Administrator Test Programme",
                                 "qualification":"BSc Test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ADMIN-TEST"))
                .andReturn().getResponse().getContentAsString();
        UUID programmeId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(programmeResponse, "$.id"));

        mockMvc.perform(post("/api/admin/modules").with(user(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"programmeId":"%s","code":"ADMIN6001","title":"Admin Module",
                                 "studyLevel":6,"yearOfStudy":3}
                                """.formatted(programmeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programmeTitle").value("Administrator Test Programme"));

        mockMvc.perform(post("/api/admin/circles").with(user(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Administrator Test Circle",
                                 "description":"A real multi-account circle created through the administrator workspace.",
                                 "category":"ACADEMIC"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(1));

        mockMvc.perform(post("/api/admin/announcements").with(user(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"UNIVERSITY","audienceType":"UNIVERSITY","audienceLabel":"",
                                 "title":"Administrator Test Announcement",
                                 "body":"This announcement verifies persistent campus publishing and notifications.",
                                 "actionLabel":"","actionUrl":"",
                                 "pinned":true,"urgent":false,"expiresAt":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Administrator Test Announcement"));

        mockMvc.perform(get("/api/admin/catalogue").with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].recordType", hasItems("ANNOUNCEMENT", "CIRCLE")));
    }

    @Test
    void suspensionTakesEffectForAnExistingAuthenticatedIdentity() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}", MICHAEL_ID).with(user(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"STUDENT\",\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(get("/api/campus-life/schedule").with(user("student")))
                .andExpect(status().isForbidden());
    }
}
