package uk.ac.unicircle.assistant;

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
                                  @Valid @RequestBody AssistantRequests.AskRequest request) {
        return service.ask(authentication.getName(), request);
    }
}
