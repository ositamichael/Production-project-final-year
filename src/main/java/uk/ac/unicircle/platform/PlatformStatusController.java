package uk.ac.unicircle.platform;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
public class PlatformStatusController {

    private final PlatformStatusService statusService;

    public PlatformStatusController(PlatformStatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/status")
    PlatformStatus status() {
        return statusService.currentStatus();
    }
}
