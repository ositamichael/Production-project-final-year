package uk.ac.unicircle.opportunity;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class OpportunityControllerTests {

    private static final UUID LIBRARY_ROLE = UUID.fromString("72f6bd9e-6ed8-47ef-b10f-ec908f417103");
    private static final UUID VOLUNTEER_ROLE = UUID.fromString("72f6bd9e-6ed8-47ef-b10f-ec908f417104");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void opportunitiesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/opportunities"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/opportunities/tracker"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approvedVerifiedCatalogueIsScopedSearchableAndFilterable() throws Exception {
        mockMvc.perform(get("/api/opportunities").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].title", hasItem("Student Ambassador")))
                .andExpect(jsonPath("$[?(@.title == 'Student Ambassador')].saved").value(true))
                .andExpect(jsonPath("$[?(@.title == 'Software Engineering Placement')].trackingStatus")
                        .value("APPLIED"));

        mockMvc.perform(get("/api/opportunities")
                        .param("type", "placement")
                        .param("query", "Java")
                        .with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Software Engineering Placement"));
    }

    @Test
    void listingsFromSuspendedPublishersAreHidden() throws Exception {
        jdbc.update("""
                UPDATE opportunity_publishers SET verification_status = 'SUSPENDED'
                WHERE name = 'Northstar Digital'
                """);

        mockMvc.perform(get("/api/opportunities").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", not(hasItem("Software Engineering Placement"))))
                .andExpect(jsonPath("$[*].title", not(hasItem("Junior Data Project Internship"))));
    }

    @Test
    void studentCanSaveAndUnsaveAnOpportunity() throws Exception {
        mockMvc.perform(post("/api/opportunities/{opportunityId}/save", LIBRARY_ROLE)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));

        mockMvc.perform(post("/api/opportunities/{opportunityId}/unsave", LIBRARY_ROLE)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(false));
    }

    @Test
    void studentCanTrackAndUpdateApplicationProgress() throws Exception {
        mockMvc.perform(post("/api/opportunities/{opportunityId}/track", VOLUNTEER_ROLE)
                        .contentType("application/json")
                        .content("{\"status\":\"planning\"}")
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingStatus").value("PLANNING"));

        mockMvc.perform(post("/api/opportunities/{opportunityId}/track", VOLUNTEER_ROLE)
                        .contentType("application/json")
                        .content("{\"status\":\"interview\"}")
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingStatus").value("INTERVIEW"));

        mockMvc.perform(get("/api/opportunities/tracker").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Peer Welcome Volunteer")));
    }

    @Test
    void trackerRejectsMissingCsrfAndInvalidStages() throws Exception {
        mockMvc.perform(post("/api/opportunities/{opportunityId}/track", VOLUNTEER_ROLE)
                        .contentType("application/json")
                        .content("{\"status\":\"APPLIED\"}")
                        .with(user("student")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/opportunities/{opportunityId}/track", VOLUNTEER_ROLE)
                        .contentType("application/json")
                        .content("{\"status\":\"SHARED_PUBLICLY\"}")
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
