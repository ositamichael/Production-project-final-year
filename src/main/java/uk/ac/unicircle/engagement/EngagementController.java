package uk.ac.unicircle.engagement;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.unicircle.engagement.EngagementResponses.BookingResponse;
import uk.ac.unicircle.engagement.EngagementResponses.EventResponse;
import uk.ac.unicircle.engagement.EngagementResponses.ScheduleItemResponse;
import uk.ac.unicircle.engagement.EngagementResponses.ServiceOfferingResponse;

@RestController
@RequestMapping("/api/campus-life")
class EngagementController {

    private final EngagementService engagement;

    EngagementController(EngagementService engagement) {
        this.engagement = engagement;
    }

    @GetMapping("/events")
    List<EventResponse> events(
            Authentication authentication,
            @RequestParam(defaultValue = "") String category) {
        return engagement.events(authentication, category);
    }

    @PostMapping("/events/{eventId}/register")
    EventResponse register(Authentication authentication, @PathVariable UUID eventId) {
        return engagement.register(authentication, eventId);
    }

    @PostMapping("/events/{eventId}/cancel")
    EventResponse cancelRegistration(Authentication authentication, @PathVariable UUID eventId) {
        return engagement.cancelRegistration(authentication, eventId);
    }

    @GetMapping("/services")
    List<ServiceOfferingResponse> services(Authentication authentication) {
        return engagement.services(authentication);
    }

    @PostMapping("/service-slots/{slotId}/book")
    BookingResponse book(Authentication authentication, @PathVariable UUID slotId) {
        return engagement.book(authentication, slotId);
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    BookingResponse cancelBooking(Authentication authentication, @PathVariable UUID bookingId) {
        return engagement.cancelBooking(authentication, bookingId);
    }

    @GetMapping("/bookings")
    List<BookingResponse> bookings(Authentication authentication) {
        return engagement.bookings(authentication);
    }

    @GetMapping("/schedule")
    List<ScheduleItemResponse> schedule(Authentication authentication) {
        return engagement.schedule(authentication);
    }
}
