package uk.ac.unicircle.assistant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
class AssistantController {
    private final AssistantService service;

    AssistantController(AssistantService service) {
        this.service = service;
    }

    @GetMapping("/status")
    AssistantResponses.Status status() {
        return service.status();
    }

    @PostMapping("/ask")
    AssistantResponses.Answer ask(Authentication authentication,
                                  HttpServletRequest httpRequest,
                                  @Valid @RequestBody AssistantRequests.AskRequest request) {
        String clientKey = authenticatedClientKey(authentication);
        if (clientKey == null) {
            clientKey = "guest:" + httpRequest.getSession(true).getId();
        }
        return service.ask(clientKey, request);
    }

    private String authenticatedClientKey(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return "account:" + authentication.getName();
    }
}
