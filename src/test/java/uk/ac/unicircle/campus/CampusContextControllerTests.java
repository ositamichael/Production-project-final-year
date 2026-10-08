package uk.ac.unicircle.campus;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class CampusContextControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void universityContextRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/campus/me/context"))
                .andExpect(status().isForbidden());
    }

    @Test
    void signedInStudentReceivesClearlyLabelledFictionalAcademicContext() throws Exception {
        mockMvc.perform(get("/api/campus/me/context").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataSource").value("FICTIONAL_DEVELOPMENT_FIXTURE"))
                .andExpect(jsonPath("$.dataNotice").value(org.hamcrest.Matchers.containsString("Fictional sandbox records")))
                .andExpect(jsonPath("$.university.name").value("Leeds Beckett–style Fictional Sandbox"))
                .andExpect(jsonPath("$.student.programmeCode").value("UC-BSC-CS"))
                .andExpect(jsonPath("$.student.yearOfStudy").value(3))
                .andExpect(jsonPath("$.modules", hasSize(3)))
                .andExpect(jsonPath("$.coursemates", hasSize(3)))
                .andExpect(jsonPath("$.coursemates[0].displayName").value("Aisha Khan"))
                .andExpect(jsonPath("$.coursemates[0].connectionState").value("CONNECTED"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("@leedsbeckett.ac.uk"))));
    }

    @Test
    void browserCannotSelectAnotherUniversityWithAQueryParameter() throws Exception {
        mockMvc.perform(get("/api/campus/me/context")
                        .param("universityId", "00000000-0000-0000-0000-000000000000")
                        .with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.university.name").value("Leeds Beckett–style Fictional Sandbox"))
                .andExpect(jsonPath("$.student.email").value("michael.carter@demo.university.example"));
    }
}
