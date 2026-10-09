package uk.ac.unicircle.lostfound;

import static org.hamcrest.Matchers.hasItem;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class LostFoundStaffReviewControllerTests {

    private static final String REVIEWER = "reviewer@demo.university.example";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void privateQueueRequiresStaffAndDoesNotLeakToStudents() throws Exception {
        UUID report = createReport("student");

        mockMvc.perform(get("/api/lost-found/staff/reports"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/lost-found/staff/reports").with(user("student")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/lost-found/staff/reports").with(user(REVIEWER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(report.toString())))
                .andExpect(jsonPath("$[0].privateIdentifyingDetails").value("A hidden stitched serial detail."));
        mockMvc.perform(get("/api/lost-found").with(user("aisha.khan@students.unicircle.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(hasItem(report.toString()))));
    }

    @Test
    void approvalPublishesReportNotifiesOwnerAndCreatesAuditRecord() throws Exception {
        UUID report = createReport("student");
        String payload = decision("APPROVED", "Public text and image checked against campus publishing guidance.");

        mockMvc.perform(post("/api/lost-found/staff/reports/{id}/decision", report)
                        .with(user("student")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/lost-found/staff/reports/{id}/decision", report)
                        .with(user(REVIEWER))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/lost-found/staff/reports/{id}/decision", report)
                        .with(user(REVIEWER)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("APPROVED"));
        mockMvc.perform(get("/api/lost-found").with(user("aisha.khan@students.unicircle.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(report.toString())));
        mockMvc.perform(post("/api/lost-found/staff/reports/{id}/decision", report)
                        .with(user(REVIEWER)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict());

        Integer audits = jdbc.queryForObject(
                "SELECT COUNT(*) FROM lost_found_review_audit WHERE report_id = ? AND decision = 'APPROVED'",
                Integer.class, report);
        Integer notices = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE resource_id = ? AND title = 'Item report approved'",
                Integer.class, report);
        org.assertj.core.api.Assertions.assertThat(audits).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(notices).isEqualTo(1);
    }

    @Test
    void rejectionKeepsReportPrivateAndShowsNoteOnlyToReporter() throws Exception {
        UUID report = createReport("student");
        String note = "Remove the exact ownership mark from the public description before resubmitting.";
        mockMvc.perform(post("/api/lost-found/staff/reports/{id}/decision", report)
                        .with(user(REVIEWER)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(decision("REJECTED", note)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lost-found/mine").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].myReviewNote").value(note));
        mockMvc.perform(get("/api/lost-found").with(user("aisha.khan@students.unicircle.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(hasItem(report.toString()))));
        Integer audits = jdbc.queryForObject(
                "SELECT COUNT(*) FROM lost_found_review_audit WHERE report_id = ? AND decision = 'REJECTED'",
                Integer.class, report);
        org.assertj.core.api.Assertions.assertThat(audits).isEqualTo(1);
    }

    @Test
    void reviewerCannotModerateOwnReportOrUseAnInvalidReason() throws Exception {
        UUID own = createReport(REVIEWER);
        mockMvc.perform(post("/api/lost-found/staff/reports/{id}/decision", own)
                        .with(user(REVIEWER)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decision("APPROVED", "This report is ready for public display.")))
                .andExpect(status().isForbidden());

        UUID studentReport = createReport("student");
        mockMvc.perform(post("/api/lost-found/staff/reports/{id}/decision", studentReport)
                        .with(user(REVIEWER)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(decision("APPROVED", "too short")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherUniversityCannotViewOrDecideThisUniversitiesReports() throws Exception {
        UUID report = createReport("student");
        UUID otherUniversity = UUID.randomUUID();
        jdbc.update("INSERT INTO universities (id, slug, display_name, email_domain) VALUES (?, ?, ?, ?)",
                otherUniversity, "another-demo", "Another Fictional University", "another.example");
        jdbc.update("""
                INSERT INTO user_accounts (id, university_id, external_subject, email, display_name, role)
                VALUES (?, ?, ?, ?, ?, 'STAFF')
                """, UUID.randomUUID(), otherUniversity, "other-reviewer", "reviewer@another.example", "Other Reviewer");

        mockMvc.perform(get("/api/lost-found/staff/reports").with(user("reviewer@another.example")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(post("/api/lost-found/staff/reports/{id}/decision", report)
                        .with(user("reviewer@another.example")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decision("APPROVED", "This is an appropriately worded review reason.")))
                .andExpect(status().isNotFound());
    }

    @Test
    void unreviewedPhotographIsStaffOnlyAndCannotBeChangedAfterApproval() throws Exception {
        UUID report = createReport("student");
        BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        MockMultipartFile photo = new MockMultipartFile("photo", "item.png", "image/png", output.toByteArray());

        mockMvc.perform(multipart("/api/lost-found/{id}/photo", report)
                        .file(photo).with(user("student")).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/lost-found/staff/reports/{id}/photo", report)
                        .with(user("student")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/lost-found/staff/reports/{id}/photo", report)
                        .with(user(REVIEWER)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lost-found/staff/reports/{id}/decision", report)
                        .with(user(REVIEWER)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decision("APPROVED", "Photograph and public wording checked for sensitive details.")))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/lost-found/{id}/photo", report)
                        .file(photo).with(user("student")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    private UUID createReport(String username) throws Exception {
        String body = """
                {"reportType":"LOST","category":"BAGS","itemName":"Canvas tote bag",
                 "colour":"Blue","publicDescription":"Plain canvas bag left after a seminar.",
                 "privateIdentifyingDetails":"A hidden stitched serial detail.",
                 "lastSeenLocation":"Library service area","occurredOn":"%s",
                 "custodyStatus":"NOT_APPLICABLE"}
                """.formatted(LocalDate.now());
        String response = mockMvc.perform(post("/api/lost-found")
                        .with(user(username)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(response.split("\\\"id\\\":\\\"")[1].substring(0, 36));
    }

    private String decision(String status, String reason) {
        return "{\"decision\":\"" + status + "\",\"reason\":\"" + reason + "\"}";
    }
}
