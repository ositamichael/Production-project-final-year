package uk.ac.unicircle.assistant;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Component
class OpenAiAssistantGateway implements AssistantGateway {
    private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");
    private static final URI MODERATION_URI = URI.create("https://api.openai.com/v1/moderations");
    private static final String SYSTEM_INSTRUCTIONS = """
            You are Ask UniCircle, a careful study, university-life and general-purpose assistant.
            Explain clearly, adapt to the learner's level and be honest about uncertainty.
            For university facts, admissions, fees, visas, policies, deadlines, services and other
            time-sensitive claims, search the web, prefer official university or government sources,
            include citations and tell the user when an institution must confirm the answer.
            Never claim access to a university's private systems, student records or live room data.
            Help students learn: explain concepts, create practice questions, review drafts, suggest
            improvements and debug code. Do not impersonate a student, fabricate research or claim
            that unverified work is the student's own. For assessed work, teach the method and help
            the student produce and disclose their own work in line with their institution's rules.
            Protect personal data, never request passwords or authenticator codes, and do not reveal
            secrets. Use concise headings and practical steps when they improve the answer.
            """;

    private final ObjectMapper mapper;
    private final HttpClient client;
    private final String apiKey;
    private final String model;
    private final String imageModel;

    OpenAiAssistantGateway(
            ObjectMapper mapper,
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${unicircle.assistant.model:gpt-6.1-sol}") String model,
            @Value("${unicircle.assistant.image-model:gpt-image-2.5-flare}") String imageModel) {
        this.mapper = mapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.imageModel = imageModel;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    @Override
    public boolean enabled() {
        return !apiKey.isBlank();
    }

    @Override
    public AssistantResponses.Answer answer(AssistantMode mode, String message) {
        if (!enabled()) {
            return new AssistantResponses.Answer(
                    "The secure AI service has been built but is not enabled on this deployment yet. "
                            + "A server-side OpenAI API key must be added before live AI answers and image creation can run.",
                    List.of(), null, mode, false,
                    "No university password or Authenticator code is ever required.");
        }
        if (isFlagged(message)) {
            return new AssistantResponses.Answer(
                    "I cannot help with that request as written. I can still help with a safe learning, wellbeing or campus-support alternative.",
                    List.of(), null, mode, true, "The request was stopped by the safety check.");
        }

        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", model);
        payload.put("max_output_tokens", 2_200);
        payload.put("store", false);
        ArrayNode input = payload.putArray("input");
        input.add(message("developer", SYSTEM_INSTRUCTIONS + "\nCurrent mode: " + modeInstructions(mode)));
        input.add(message("user", message));

        ArrayNode tools = payload.putArray("tools");
        if (mode == AssistantMode.GENERAL || mode == AssistantMode.UNIVERSITY_RESEARCH) {
            tools.addObject().put("type", "web_search");
        }
        if (mode == AssistantMode.CODE_LAB) {
            ObjectNode interpreter = tools.addObject();
            interpreter.put("type", "code_interpreter");
            interpreter.putObject("container").put("type", "auto");
        }
        if (mode == AssistantMode.IMAGE_STUDIO) {
            ObjectNode imageTool = tools.addObject();
            imageTool.put("type", "image_generation");
            imageTool.put("model", imageModel);
            payload.putObject("tool_choice").put("type", "image_generation");
        }

        JsonNode response = post(RESPONSES_URI, payload);
        return parseResponse(response, mode);
    }

    private boolean isFlagged(String message) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", "omni-moderation-latest");
        payload.put("input", message);
        JsonNode response = post(MODERATION_URI, payload);
        return response.path("results").path(0).path("flagged").asBoolean(false);
    }

    private ObjectNode message(String role, String text) {
        ObjectNode item = mapper.createObjectNode();
        item.put("role", role);
        item.putArray("content").addObject().put("type", "input_text").put("text", text);
        return item;
    }

    private String modeInstructions(AssistantMode mode) {
        return switch (mode) {
            case GENERAL -> "Answer a broad question. Search when current facts would materially improve accuracy.";
            case UNIVERSITY_RESEARCH -> "Research across universities and compare only verifiable facts. Prefer official sources and clearly distinguish institutions.";
            case STUDY_COACH -> "Teach step by step, check understanding and support independent student work.";
            case CODE_LAB -> "Explain, design, debug and test code. State assumptions and discuss security and testing.";
            case IMAGE_STUDIO -> "Create an original, useful image from the user's description. Avoid misleading official university branding.";
        };
    }

    private JsonNode post(URI uri, JsonNode payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(100))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(BAD_GATEWAY, "The AI provider could not complete the request.");
            }
            return mapper.readTree(response.body());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(BAD_GATEWAY, "The AI request was interrupted.", interrupted);
        } catch (IOException failure) {
            throw new ResponseStatusException(BAD_GATEWAY, "The AI service is temporarily unavailable.", failure);
        }
    }

    private AssistantResponses.Answer parseResponse(JsonNode response, AssistantMode mode) {
        StringBuilder answer = new StringBuilder();
        String imageDataUrl = null;
        Map<String, AssistantResponses.Source> sources = new LinkedHashMap<>();

        for (JsonNode output : response.path("output")) {
            if ("message".equals(output.path("type").asText())) {
                for (JsonNode content : output.path("content")) {
                    if ("output_text".equals(content.path("type").asText())) {
                        if (!answer.isEmpty()) answer.append("\n\n");
                        answer.append(content.path("text").asText());
                        collectSources(content.path("annotations"), sources);
                    }
                }
            }
            if ("image_generation_call".equals(output.path("type").asText()) && output.hasNonNull("result")) {
                imageDataUrl = "data:image/png;base64," + output.path("result").asText();
            }
        }

        if (answer.isEmpty()) {
            answer.append(imageDataUrl == null
                    ? "I could not produce a complete answer. Please rephrase the question and try again."
                    : "Your original image is ready. You can open or save it from the result below.");
        }
        return new AssistantResponses.Answer(answer.toString(), new ArrayList<>(sources.values()), imageDataUrl,
                mode, true, "AI can make mistakes. Check important university decisions against the cited official source.");
    }

    private void collectSources(JsonNode annotations, Map<String, AssistantResponses.Source> sources) {
        for (JsonNode annotation : annotations) {
            if (!"url_citation".equals(annotation.path("type").asText())) continue;
            String url = annotation.path("url").asText();
            if (!isSafeSourceUrl(url)) continue;
            String title = annotation.path("title").asText("Source");
            sources.putIfAbsent(url, new AssistantResponses.Source(title, url));
        }
    }

    private boolean isSafeSourceUrl(String url) {
        if (url.isBlank()) return false;
        try {
            String scheme = URI.create(url).getScheme();
            return "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme);
        } catch (IllegalArgumentException invalidUrl) {
            return false;
        }
    }
}
