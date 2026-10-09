package uk.ac.unicircle.lostfound;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class LostFoundControllerTests {

    private static final UUID AISHA_LANYARD = UUID.fromString("4ddab979-7772-4a1c-9ded-6fb73123f102");
    private static final UUID MICHAEL_SLEEVE = UUID.fromString("4ddab979-7772-4a1c-9ded-6fb73123f101");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void lostAndFoundRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/lost-found"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/lost-found/mine"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/lost-found/claims"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approvedCatalogueIsSearchableAndFilterable() throws Exception {
        mockMvc.perform(get("/api/lost-found").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].itemName", hasItem("Black laptop sleeve")));

        mockMvc.perform(get("/api/lost-found")
                        .param("type", "found")
                        .param("category", "keys")
                        .param("query", "Student Hub")
                        .with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].itemName").value("Blue key lanyard"));
    }

    @Test
    void newReportIsPrivateUntilModeratedAndAppearsInMyReports() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/lost-found")
                        .with(user("student"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moderationStatus").value("PENDING"))
                .andExpect(jsonPath("$.mine").value(true))
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                .doesNotContain("handwritten ownership label");
        UUID reportId = responseId(result);

        mockMvc.perform(get("/api/lost-found").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(hasItem(reportId.toString()))));

        mockMvc.perform(get("/api/lost-found/mine").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(reportId.toString())));
        org.assertj.core.api.Assertions.assertThat(mockMvc.perform(get("/api/lost-found/mine")
                        .with(user("student"))).andReturn().getResponse().getContentAsString())
                .doesNotContain("handwritten ownership label");
    }

    @Test
    void photographsAreNotPublicAfterTheirReportIsWithdrawn() throws Exception {
        UUID reportId = responseId(mockMvc.perform(post("/api/lost-found")
                        .with(user("student")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(reportJson()))
                .andExpect(status().isCreated()).andReturn());
        BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        MockMultipartFile photo = new MockMultipartFile("photo", "item.png", "image/png", output.toByteArray());

        mockMvc.perform(multipart("/api/lost-found/{reportId}/photo", reportId)
                        .file(photo).with(user("student")).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/lost-found/staff/reports/{reportId}/decision", reportId)
                        .with(user("reviewer@demo.university.example")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"reason\":\"Public wording and photograph are suitable for the catalogue.\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/lost-found/{reportId}/photo", reportId)
                        .with(user("aisha.khan@students.unicircle.test")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lost-found/{reportId}/withdraw", reportId)
                        .with(user("student")).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/lost-found/{reportId}/photo", reportId)
                        .with(user("aisha.khan@students.unicircle.test")))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownershipClaimIsPrivateAccountSpecificAndNotifiesReporter() throws Exception {
        mockMvc.perform(post("/api/lost-found/{reportId}/claims", AISHA_LANYARD)
                        .with(user("student"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"privateOwnershipEvidence\":\"There are three keys and one has a green plastic cover.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claimStatus").value("SUBMITTED"));

        mockMvc.perform(get("/api/lost-found/claims").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].itemName", hasItem("Blue key lanyard")));

        Integer claims = jdbc.queryForObject("""
                SELECT COUNT(*) FROM lost_found_claims
                WHERE report_id = ? AND private_ownership_evidence LIKE '%green plastic cover%'
                """, Integer.class, AISHA_LANYARD);
        Integer notifications = jdbc.queryForObject("""
                SELECT COUNT(*) FROM notifications
                WHERE resource_id = ? AND notification_type = 'LOST_FOUND'
                """, Integer.class, AISHA_LANYARD);
        org.assertj.core.api.Assertions.assertThat(claims).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(notifications).isEqualTo(1);
    }

    @Test
    void duplicateClaimAndClaimingOwnReportAreRejected() throws Exception {
        String evidence = "{\"privateOwnershipEvidence\":\"Three keys including one with a green plastic cover.\"}";
        mockMvc.perform(post("/api/lost-found/{reportId}/claims", AISHA_LANYARD)
                        .with(user("student")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(evidence))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/lost-found/{reportId}/claims", AISHA_LANYARD)
                        .with(user("student")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(evidence))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/lost-found/{reportId}/claims", MICHAEL_SLEEVE)
                        .with(user("student")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(evidence))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportValidationAndStateChangesRequireCsrf() throws Exception {
        mockMvc.perform(post("/api/lost-found")
                        .with(user("student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson()))
                .andExpect(status().isForbidden());

        MvcResult report = mockMvc.perform(post("/api/lost-found")
                        .with(user("student")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(reportJson()))
                .andExpect(status().isCreated()).andReturn();

        mockMvc.perform(post("/api/lost-found/{reportId}/withdraw", responseId(report))
                        .with(user("student")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportStatus").value("WITHDRAWN"));
    }

    private String reportJson() {
        return """
                {
                  "reportType": "LOST",
                  "category": "ELECTRONICS",
                  "itemName": "Wireless headphones",
                  "colour": "Black",
                  "publicDescription": "Black over-ear headphones in a plain protective case.",
                  "privateIdentifyingDetails": "The case contains a handwritten ownership label.",
                  "lastSeenLocation": "Rose Bowl, fourth floor",
                  "occurredOn": "%s",
                  "custodyStatus": "NOT_APPLICABLE"
                }
                """.formatted(LocalDate.now());
    }

    private UUID responseId(MvcResult result) throws Exception {
        Matcher matcher = Pattern.compile("\\\"id\\\":\\\"([0-9a-f-]{36})\\\"")
                .matcher(result.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(matcher.find()).isTrue();
        return UUID.fromString(matcher.group(1));
    }
}
