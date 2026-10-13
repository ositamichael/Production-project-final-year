package uk.ac.unicircle.assistant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
class AssistantService {
    private static final int REQUEST_LIMIT = 20;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final AssistantGateway gateway;
    private final Map<String, ArrayDeque<Instant>> requestWindows = new ConcurrentHashMap<>();

    AssistantService(AssistantGateway gateway) {
        this.gateway = gateway;
    }

    AssistantResponses.Status status() {
        return new AssistantResponses.Status(gateway.enabled(), gateway.advancedEnabled() ? "ADVANCED" : "KNOWLEDGE",
                Arrays.asList(AssistantMode.values()), gateway.advancedEnabled()
                        ? "Advanced AI, sourced web research, code analysis and image creation are available."
                        : "The no-cost knowledge assistant is active. It combines reviewed local guidance, optional sourced public-knowledge retrieval and private vector illustrations; unrestricted reasoning, code execution and photorealistic image generation are not enabled.");
    }

    AssistantResponses.Answer ask(String username, AssistantRequests.AskRequest request) {
        enforceRateLimit(username);
        return gateway.answer(request.mode(), request.message().trim(), Boolean.TRUE.equals(request.allowPublicLookup()));
    }

    private void enforceRateLimit(String username) {
        ArrayDeque<Instant> window = requestWindows.computeIfAbsent(username, ignored -> new ArrayDeque<>());
        synchronized (window) {
            Instant cutoff = Instant.now().minus(WINDOW);
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) window.removeFirst();
            if (window.size() >= REQUEST_LIMIT) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait before sending more AI requests.");
            }
            window.addLast(Instant.now());
        }
    }
}
