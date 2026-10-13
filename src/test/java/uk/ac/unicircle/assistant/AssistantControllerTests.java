package uk.ac.unicircle.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

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

    @Autowired
    PublicKnowledgeAssistant publicKnowledgeAssistant;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void assistantIsAvailableToGuestsWithoutExposingPrivateAccountData() throws Exception {
        mockMvc.perform(get("/api/assistant/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.level").value("KNOWLEDGE"));

        mockMvc.perform(post("/api/assistant/ask").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"Help me study\",\"mode\":\"STUDY_COACH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("STUDY_COACH"))
                .andExpect(jsonPath("$.live").value(false));
    }

    @Test
    void statusExplainsThatTheNoCostBasicAssistantIsActive() throws Exception {
        mockMvc.perform(get("/api/assistant/status").with(user("student@example.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.level").value("KNOWLEDGE"))
                .andExpect(jsonPath("$.modes.length()").value(5))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                        "no-cost knowledge assistant is active")));
    }

    @Test
    void basicAssistantAnswersACommonComputingQuestionWithoutAProviderKey() throws Exception {
        mockMvc.perform(post("/api/assistant/ask")
                        .with(user("student@example.test")).with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"What is a computer?\",\"mode\":\"GENERAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("GENERAL"))
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("electronic device")))
                .andExpect(jsonPath("$.imageDataUrl").doesNotExist());
    }

    @Test
    void basicAssistantExplainsSpringBootAndPerformsSimpleArithmetic() throws Exception {
        mockMvc.perform(post("/api/assistant/ask").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"Explain Spring Boot\",\"mode\":\"CODE_LAB\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("Java framework")));

        mockMvc.perform(post("/api/assistant/ask").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"What is 12 * 7?\",\"mode\":\"GENERAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("12 * 7 = 84."));

        mockMvc.perform(post("/api/assistant/ask").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"What is the best programming language?\",\"mode\":\"GENERAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString(
                        "no single best programming language")));
    }

    @Test
    void basicAssistantAnswersSupportedBeginnerCodeQuestions() throws Exception {
        mockMvc.perform(post("/api/assistant/ask").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"Show me a Java for loop\",\"mode\":\"CODE_LAB\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("for (int i = 0"),
                        org.hamcrest.Matchers.containsString("prints 0 to 4"))));

        mockMvc.perform(post("/api/assistant/ask").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"What is Python?\",\"mode\":\"CODE_LAB\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("high-level programming language"),
                        org.hamcrest.Matchers.containsString("Hello, world!"))));
    }

    @Test
    void noCostImageStudioCreatesADownloadablePrivateVectorIllustration() throws Exception {
        mockMvc.perform(post("/api/assistant/ask").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"Create students studying in a modern library\","
                                + "\"mode\":\"IMAGE_STUDIO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString(
                        "downloadable vector illustration")))
                .andExpect(jsonPath("$.imageDataUrl").value(org.hamcrest.Matchers.startsWith(
                        "data:image/svg+xml;base64,")))
                .andExpect(jsonPath("$.notice").value(org.hamcrest.Matchers.containsString(
                        "not sent to an external image service")));
    }

    @Test
    void widerPublicKnowledgeRequiresExplicitConsent() throws Exception {
        mockMvc.perform(post("/api/assistant/ask").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"Who made Java?\",\"mode\":\"CODE_LAB\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString(
                        "Use public sources")));
    }

    @Test
    void publicLookupBlocksSensitiveStudentIdentifiersBeforeExternalRequest() throws Exception {
        mockMvc.perform(post("/api/assistant/ask").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"Who is c3707780?\",\"mode\":\"GENERAL\","
                                + "\"allowPublicLookup\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.sources.length()").value(0))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString(
                        "did not send")));
    }

    @Test
    void publicKnowledgeResultIsConciseAndHasAVisibleSource() throws Exception {
        var response = objectMapper.readTree("""
                {"query":{"pages":[{"title":"Java (programming language)",
                "extract":"Java is a programming language. Java was designed by James Gosling at Sun Microsystems. It was released in 1995.",
                "fullurl":"https://en.wikipedia.org/wiki/Java_(programming_language)"}]}}
                """);

        var answer = publicKnowledgeAssistant.parseResponse(AssistantMode.CODE_LAB, "Who made Java?", response);

        org.junit.jupiter.api.Assertions.assertEquals("Java was designed by James Gosling at Sun Microsystems.",
                answer.answer());
        org.junit.jupiter.api.Assertions.assertTrue(answer.live());
        org.junit.jupiter.api.Assertions.assertEquals(1, answer.sources().size());
        org.junit.jupiter.api.Assertions.assertEquals("Java programming language",
                publicKnowledgeAssistant.searchTopic("Who made Java?", AssistantMode.CODE_LAB));
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
