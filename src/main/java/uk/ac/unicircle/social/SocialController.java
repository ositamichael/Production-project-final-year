package uk.ac.unicircle.social;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.unicircle.social.SocialResponses.ConnectionResponse;
import uk.ac.unicircle.social.SocialResponses.ConversationMessagesResponse;
import uk.ac.unicircle.social.SocialResponses.ConversationResponse;
import uk.ac.unicircle.social.SocialResponses.MessageResponse;
import uk.ac.unicircle.social.SocialResponses.NotificationResponse;
import uk.ac.unicircle.social.SocialResponses.SafetyActionResponse;
import uk.ac.unicircle.social.SocialResponses.StudentDirectoryResponse;

@RestController
@RequestMapping("/api/social")
class SocialController {

    private final SocialService social;

    SocialController(SocialService social) {
        this.social = social;
    }

    @GetMapping("/connections")
    List<ConnectionResponse> connections(Authentication authentication) {
        return social.connections(authentication);
    }

    @GetMapping("/directory")
    List<StudentDirectoryResponse> directory(
            Authentication authentication,
            @RequestParam(defaultValue = "") @Size(max = 80) String query) {
        return social.directory(authentication, query);
    }

    @PostMapping("/connections/{accountId}")
    ConnectionResponse requestConnection(Authentication authentication, @PathVariable UUID accountId) {
        return social.requestConnection(authentication, accountId);
    }

    @PostMapping("/connections/{connectionId}/accept")
    ConnectionResponse accept(Authentication authentication, @PathVariable UUID connectionId) {
        return social.accept(authentication, connectionId);
    }

    @PostMapping("/connections/{connectionId}/decline")
    ConnectionResponse decline(Authentication authentication, @PathVariable UUID connectionId) {
        return social.decline(authentication, connectionId);
    }

    @GetMapping("/conversations")
    List<ConversationResponse> conversations(Authentication authentication) {
        return social.conversations(authentication);
    }

    @PostMapping("/conversations/direct/{accountId}")
    ConversationResponse openDirectConversation(Authentication authentication, @PathVariable UUID accountId) {
        return social.openDirectConversation(authentication, accountId);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    ConversationMessagesResponse messages(Authentication authentication, @PathVariable UUID conversationId) {
        return social.messages(authentication, conversationId);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    MessageResponse send(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return social.send(authentication, conversationId, request.body());
    }

    @GetMapping("/notifications")
    List<NotificationResponse> notifications(Authentication authentication) {
        return social.notifications(authentication);
    }

    @PostMapping("/notifications/read-all")
    void markNotificationsRead(Authentication authentication) {
        social.markNotificationsRead(authentication);
    }

    @PostMapping("/safety/block/{accountId}")
    SafetyActionResponse block(Authentication authentication, @PathVariable UUID accountId) {
        return social.block(authentication, accountId);
    }

    @PostMapping("/safety/report/{accountId}")
    SafetyActionResponse report(
            Authentication authentication,
            @PathVariable UUID accountId,
            @Valid @RequestBody ReportAccountRequest request) {
        return social.report(authentication, accountId, request.reason(), request.details());
    }

    record SendMessageRequest(@NotBlank @Size(max = 4000) String body) {
    }

    record ReportAccountRequest(
            @NotBlank @Size(max = 40) String reason,
            @Size(max = 1000) String details) {
    }
}
