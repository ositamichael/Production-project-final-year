package uk.ac.unicircle.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK,
        properties = "OPENAI_API_KEY=")
@AutoConfigureMockMvc
class AssistantControllerTests {
    @Autowired
    MockMvc mockMvc;

    @Test
    void assistantIsAvailableToGuestsWithoutExposingPrivateAccountData() throws Exception {
        mockMvc.perform(get("/api/assistant/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/api/assistant/ask").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"Help me study\",\"mode\":\"STUDY_COACH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("STUDY_COACH"))
                .andExpect(jsonPath("$.live").value(false));
    }

    @Test
    void statusExplainsWhenServerSideAiConfigurationIsMissing() throws Exception {
        mockMvc.perform(get("/api/assistant/status").with(user("student@example.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.modes.length()").value(5))
                .andExpect(jsonPath("$.message").value(
                        "The AI workspace is installed but needs a server-side OpenAI API key."));
    }

    @Test
    void configuredWorkspaceShapeIsReturnedWithoutExposingASecret() throws Exception {
        mockMvc.perform(post("/api/assistant/ask")
                        .with(user("student@example.test")).with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"Explain dependency injection\",\"mode\":\"STUDY_COACH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("STUDY_COACH"))
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("server-side OpenAI API key")))
                .andExpect(jsonPath("$.imageDataUrl").doesNotExist());
    }

    @Test
    void assistantRejectsMissingAndOversizedQuestions() throws Exception {
        mockMvc.perform(post("/api/assistant/ask")
                        .with(user("student@example.test")).with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"   \",\"mode\":\"GENERAL\"}"))
                .andExpect(status().isBadRequest());

        String oversized = "a".repeat(4_001);
        mockMvc.perform(post("/api/assistant/ask")
                        .with(user("student@example.test")).with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"" + oversized + "\",\"mode\":\"GENERAL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assistantLimitsRepeatedRequestsPerAccount() throws Exception {
        for (int request = 0; request < 20; request++) {
            mockMvc.perform(post("/api/assistant/ask")
                            .with(user("rate-limit-test@example.test")).with(csrf())
                            .contentType("application/json")
                            .content("{\"message\":\"Help me practise\",\"mode\":\"STUDY_COACH\"}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/assistant/ask")
                        .with(user("rate-limit-test@example.test")).with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"One more request\",\"mode\":\"STUDY_COACH\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
