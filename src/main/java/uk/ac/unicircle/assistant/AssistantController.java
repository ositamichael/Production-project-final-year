package uk.ac.unicircle.assistant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/assistant")
class AssistantController {
    private final AssistantService service;
    private final AssistantConversationService conversations;

    AssistantController(AssistantService service, AssistantConversationService conversations) {
        this.service = service;
        this.conversations = conversations;
    }

    @GetMapping("/status")
    AssistantResponses.Status status() {
        return service.status();
    }

    @PostMapping("/ask")
    AssistantResponses.Answer ask(Authentication authentication,
                                  HttpServletRequest httpRequest,
                                  @Valid @RequestBody AssistantRequests.AskRequest request) {
        return service.ask(clientKey(authentication, httpRequest), request);
    }

    @GetMapping("/conversations")
    List<AssistantResponses.ConversationSummary> conversations(Authentication authentication,
                                                                HttpServletRequest request) {
        return conversations.list(clientKey(authentication, request));
    }

    @PostMapping("/conversations")
    AssistantResponses.ConversationDetail createConversation(Authentication authentication,
                                                               HttpServletRequest request,
                                                               @Valid @RequestBody AssistantRequests.CreateConversationRequest body) {
        return conversations.create(clientKey(authentication, request), body);
    }

    @GetMapping("/conversations/{conversationId}")
    AssistantResponses.ConversationDetail conversation(Authentication authentication,
                                                        HttpServletRequest request,
                                                        @PathVariable UUID conversationId) {
        return conversations.detail(clientKey(authentication, request), conversationId);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    AssistantResponses.ConversationDetail askInConversation(Authentication authentication,
                                                             HttpServletRequest request,
                                                             @PathVariable UUID conversationId,
                                                             @Valid @RequestBody AssistantRequests.AskRequest body) {
        return conversations.ask(clientKey(authentication, request), conversationId, body);
    }

    private String clientKey(Authentication authentication, HttpServletRequest request) {
        String authenticated = authenticatedClientKey(authentication);
        return authenticated == null ? "guest:" + request.getSession(true).getId() : authenticated;
    }

    private String authenticatedClientKey(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return "account:" + authentication.getName();
    }
}
