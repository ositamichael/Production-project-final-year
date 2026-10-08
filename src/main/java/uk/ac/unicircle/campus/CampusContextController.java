package uk.ac.unicircle.campus;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campus/me")
class CampusContextController {

    private final CampusContextService campusContext;

    CampusContextController(CampusContextService campusContext) {
        this.campusContext = campusContext;
    }

    @GetMapping("/context")
    UniversityContextResponse context(Authentication authentication) {
        return campusContext.current(authentication);
    }
}
