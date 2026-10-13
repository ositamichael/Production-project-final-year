package uk.ac.unicircle.assistant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Opt-in retrieval of concise, attributed public knowledge. Only a shortened,
 * screened search topic is sent; the full conversation is never transmitted.
 */
@Component
class PublicKnowledgeAssistant {
    private static final String USER_AGENT =
            "UniCircle/0.1 (final-year project; https://github.com/ositamichael/Production-project-final-year)";
    private static final String KNOWLEDGE_NOTICE =
            "Public knowledge retrieved from English Wikipedia. It may be incomplete or out of date; check important or changing facts with an official source.";
    private static final String LIMITED_NOTICE =
            "No-cost mode cannot verify this changing or high-stakes information. Use the named official service or a qualified professional.";
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+");
    private static final Pattern EMAIL = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern LONG_NUMBER = Pattern.compile("(?<!\\d)\\d[\\d ()+-]{6,}\\d(?!\\d)");
    private static final Pattern STUDENT_ID = Pattern.compile("(?i)\\bc\\d{7,10}\\b");
    private static final Pattern URL = Pattern.compile("(?i)https?://|www\\.");

    private final ObjectMapper mapper;
    private final HttpClient client;
    private final URI endpoint;

    @Autowired
    PublicKnowledgeAssistant(
            ObjectMapper mapper,
            @Value("${UNICIRCLE_PUBLIC_KNOWLEDGE_URL:https://en.wikipedia.org/w/api.php}") String endpoint) {
        this(mapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), URI.create(endpoint));
    }

    PublicKnowledgeAssistant(ObjectMapper mapper, HttpClient client, URI endpoint) {
        this.mapper = mapper;
        this.client = client;
        this.endpoint = endpoint;
    }

    AssistantResponses.Answer answer(AssistantMode mode, String question, boolean authorisedByUser) {
        if (!authorisedByUser) return consentRequired(mode);

        String normal = question.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        AssistantResponses.Answer guarded = guardedAnswer(mode, normal);
        if (guarded != null) return guarded;
        if (containsSensitiveData(question)) return sensitiveDataBlocked(mode);

        try {
            String query = searchTopic(question, mode);
            if (query.isBlank()) return noReliableResult(mode);
            HttpRequest request = HttpRequest.newBuilder(searchUri(query))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return unavailable(mode);
            return parseResponse(mode, question, mapper.readTree(response.body()));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return unavailable(mode);
        } catch (IOException | IllegalArgumentException failure) {
            return unavailable(mode);
        }
    }

    AssistantResponses.Answer parseResponse(AssistantMode mode, String question, JsonNode response) {
        JsonNode pages = response.path("query").path("pages");
        if (!pages.isArray() || pages.isEmpty()) return noReliableResult(mode);

        JsonNode page = pages.path(0);
        String title = page.path("title").asText("").trim();
        String extract = page.path("extract").asText("").replaceAll("\\s+", " ").trim();
        String url = page.path("fullurl").asText("").trim();
        if (title.isBlank() || extract.isBlank() || !safeWikipediaUrl(url)) return noReliableResult(mode);

        return new AssistantResponses.Answer(selectAnswer(question, title, extract),
                List.of(new AssistantResponses.Source(title + " — Wikipedia", url)),
                null, mode, true, KNOWLEDGE_NOTICE);
    }

    String searchTopic(String question, AssistantMode mode) {
        String query = question.replaceAll("[?!.]+$", "").trim();
        String lower = query.toLowerCase(Locale.ROOT);
        String[] prefixes = {"can you tell me ", "please tell me ", "tell me about ", "who made ",
                "who created ", "who invented ", "who is ", "what is ", "what are ", "define ",
                "explain ", "where is ", "when was "};
        for (String prefix : prefixes) {
            if (lower.startsWith(prefix) && query.length() > prefix.length()) {
                query = query.substring(prefix.length()).trim();
                break;
            }
        }

        String subject = query.toLowerCase(Locale.ROOT);
        if ((mode == AssistantMode.CODE_LAB || asksAboutProgrammingLanguage(question))
                && List.of("java", "python", "c#", "c sharp", "javascript", "typescript", "kotlin", "swift")
                .contains(subject)) {
            query += " programming language";
        }
        query = query.replaceAll("[^A-Za-z0-9 #+.'()&-]", " ").replaceAll("\\s+", " ").trim();
        return query.substring(0, Math.min(query.length(), 160));
    }

    private AssistantResponses.Answer guardedAnswer(AssistantMode mode, String question) {
        if (containsAny(question, "latest", "today", "right now", "current price", "current score",
                "weather", "opening hours", "deadline", "entry requirement", "admission requirement",
                "tuition fee", "student visa")) {
            return new AssistantResponses.Answer(
                    "That information can change and this no-cost public-knowledge service cannot verify it reliably. Please use the relevant university, government, organisation or live service website. If you provide the official wording without personal data, UniCircle can help explain it.",
                    List.of(), null, mode, false, LIMITED_NOTICE);
        }
        if (containsAny(question, "diagnose me", "my symptoms", "what medicine should i", "legal advice",
                "should i invest", "which stock should i buy", "am i having a heart attack")) {
            return new AssistantResponses.Answer(
                    "I can explain general background, but I cannot make a personal medical, legal or financial decision for you. Use an appropriate qualified professional or official urgent service for advice about your situation.",
                    List.of(), null, mode, false, LIMITED_NOTICE);
        }
        return null;
    }

    private boolean containsSensitiveData(String question) {
        String lower = question.toLowerCase(Locale.ROOT);
        return EMAIL.matcher(question).find() || LONG_NUMBER.matcher(question).find()
                || STUDENT_ID.matcher(question).find() || URL.matcher(question).find()
                || containsAny(lower, "password", "authenticator", "one-time code", "otp", "api key",
                "secret key", "date of birth", "home address", "national insurance", "nhs number",
                "bank account", "credit card");
    }

    private URI searchUri(String query) {
        String separator = endpoint.toString().contains("?") ? "&" : "?";
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return URI.create(endpoint + separator
                + "action=query&generator=search&gsrnamespace=0&gsrlimit=1&gsrsearch=" + encoded
                + "&prop=extracts%7Cinfo&exintro=1&explaintext=1&inprop=url"
                + "&format=json&formatversion=2&redirects=1");
    }

    private boolean asksAboutProgrammingLanguage(String question) {
        String lower = question.toLowerCase(Locale.ROOT);
        return containsAny(lower, "programming", "coding", "java", "python", "c#", "c sharp",
                "javascript", "typescript", "kotlin", "swift");
    }

    private String selectAnswer(String question, String title, String extract) {
        String[] sentences = SENTENCE_END.split(extract);
        if (question.stripLeading().toLowerCase(Locale.ROOT).startsWith("who ")) {
            for (String sentence : sentences) {
                String candidate = sentence.toLowerCase(Locale.ROOT);
                if (containsAny(candidate, " created by ", " designed by ", " developed by ",
                        " invented by ", " founded by ", " written by ")) {
                    return sentence.trim();
                }
            }
        }

        StringBuilder result = new StringBuilder();
        for (String sentence : sentences) {
            if (!result.isEmpty()) result.append(' ');
            result.append(sentence.trim());
            if (result.length() >= 650 || sentenceCount(result) >= 3) break;
        }
        if (result.length() > 1_200) result.setLength(1_200);
        return title + ": " + result.toString().trim();
    }

    private long sentenceCount(StringBuilder value) {
        return value.toString().chars().filter(character -> character == '.' || character == '!' || character == '?').count();
    }

    private AssistantResponses.Answer consentRequired(AssistantMode mode) {
        return new AssistantResponses.Answer(
                "I do not have a reviewed local answer for that question. Turn on “Use public sources” below the message box, then ask again. UniCircle will send only a shortened, screened topic to English Wikipedia and show the source it used.",
                List.of(), null, mode, false,
                "Public lookup is optional and remains off until you choose it. Never include personal, confidential or account information.");
    }

    private AssistantResponses.Answer sensitiveDataBlocked(AssistantMode mode) {
        return new AssistantResponses.Answer(
                "I did not send that question to the public service because it appears to contain personal, account or confidential information. Remove those details and ask only the general topic.",
                List.of(), null, mode, false,
                "Sensitive-input protection stopped the external lookup.");
    }

    private AssistantResponses.Answer unavailable(AssistantMode mode) {
        return new AssistantResponses.Answer(
                "I could not reach the public knowledge service just now. Please try again shortly, rephrase the question with the exact topic, or check a reliable official source.",
                List.of(), null, mode, false,
                "The reviewed local assistant is still available, but the wider public-knowledge lookup was temporarily unavailable.");
    }

    private AssistantResponses.Answer noReliableResult(AssistantMode mode) {
        return new AssistantResponses.Answer(
                "I could not find a reliable public-knowledge result for that wording. Try a more specific question that names the person, place, subject or technology you mean.",
                List.of(), null, mode, false,
                "No source was returned, so UniCircle has not invented an answer.");
    }

    private boolean safeWikipediaUrl(String url) {
        try {
            URI parsed = URI.create(url);
            return "https".equalsIgnoreCase(parsed.getScheme()) && parsed.getHost() != null
                    && (parsed.getHost().equals("wikipedia.org") || parsed.getHost().endsWith(".wikipedia.org"));
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private boolean containsAny(String value, String... phrases) {
        for (String phrase : phrases) {
            if (value.contains(phrase)) return true;
        }
        return false;
    }
}
