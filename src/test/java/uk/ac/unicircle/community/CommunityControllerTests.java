package uk.ac.unicircle.community;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class CommunityControllerTests {
    private static final UUID COMPUTING = UUID.nameUUIDFromBytes(
            "unicircle:demo-university:circle:Computer Science Society".getBytes(StandardCharsets.UTF_8));

    @Autowired
    MockMvc mockMvc;

    @Test
    void circlesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/community/circles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void catalogueCanBeSearchedAndFiltered() throws Exception {
        mockMvc.perform(get("/api/community/circles").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));

        mockMvc.perform(get("/api/community/circles")
                        .param("query", "computer")
                        .param("category", "academic")
                        .with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Computer Science Society"));
    }

    @Test
    void studentCanJoinAndLeaveACircle() throws Exception {
        mockMvc.perform(post("/api/community/circles/{circleId}/join", COMPUTING)
                        .with(user("student")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joined").value(true))
                .andExpect(jsonPath("$.memberCount").value(1));

        mockMvc.perform(post("/api/community/circles/{circleId}/leave", COMPUTING)
                        .with(user("student")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joined").value(false));
    }

    @Test
    void studentCanCreateAnOwnedCircle() throws Exception {
        mockMvc.perform(post("/api/community/circles")
                        .with(user("student")).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"name":"Java Learners","description":"A friendly place to practise Java together.","category":"ACADEMIC"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Java Learners"))
                .andExpect(jsonPath("$.joined").value(true))
                .andExpect(jsonPath("$.owner").value(true));
    }

    @Test
    void invalidCreationAndMissingCsrfAreRejected() throws Exception {
        String valid = """
                {"name":"Testing Circle","description":"A complete description for this test circle.","category":"PROJECTS"}
                """;
        mockMvc.perform(post("/api/community/circles").with(user("student"))
                        .contentType("application/json").content(valid))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/community/circles").with(user("student")).with(csrf())
                        .contentType("application/json")
                        .content("{\"name\":\"x\",\"description\":\"short\",\"category\":\"ACADEMIC\"}"))
                .andExpect(status().isBadRequest());
    }
}
