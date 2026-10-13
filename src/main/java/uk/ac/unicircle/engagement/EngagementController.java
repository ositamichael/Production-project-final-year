package uk.ac.unicircle.engagement;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
    private static final DateTimeFormatter CALENDAR_TIME = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

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

    @GetMapping(value = "/schedule.ics", produces = "text/calendar;charset=UTF-8")
    ResponseEntity<String> calendar(Authentication authentication) {
        List<ScheduleItemResponse> items = engagement.schedule(authentication);
        StringBuilder calendar = new StringBuilder()
                .append("BEGIN:VCALENDAR\r\n")
                .append("VERSION:2.0\r\n")
                .append("PRODID:-//UniCircle//Campus Schedule//EN\r\n")
                .append("CALSCALE:GREGORIAN\r\n")
                .append("METHOD:PUBLISH\r\n");
        String generatedAt = CALENDAR_TIME.format(java.time.Instant.now());
        for (ScheduleItemResponse item : items) {
            calendar.append("BEGIN:VEVENT\r\n")
                    .append("UID:").append(item.resourceType()).append('-').append(item.resourceId())
                    .append("@unicircle\r\n")
                    .append("DTSTAMP:").append(generatedAt).append("\r\n")
                    .append("DTSTART:").append(CALENDAR_TIME.format(item.startsAt())).append("\r\n")
                    .append("DTEND:").append(CALENDAR_TIME.format(item.endsAt())).append("\r\n")
                    .append("SUMMARY:").append(escapeCalendarText(item.title())).append("\r\n")
                    .append("LOCATION:").append(escapeCalendarText(item.location())).append("\r\n")
                    .append("DESCRIPTION:").append(escapeCalendarText(
                            "Added from UniCircle · " + item.resourceType())).append("\r\n")
                    .append("END:VEVENT\r\n");
        }
        calendar.append("END:VCALENDAR\r\n");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=unicircle-campus-schedule.ics")
                .body(calendar.toString());
    }

    private String escapeCalendarText(String value) {
        return value.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }
}
