package uk.ac.unicircle.engagement;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import uk.ac.unicircle.engagement.EngagementResponses.BookingResponse;
import uk.ac.unicircle.engagement.EngagementResponses.EventResponse;
import uk.ac.unicircle.engagement.EngagementResponses.ScheduleItemResponse;
import uk.ac.unicircle.engagement.EngagementResponses.ServiceOfferingResponse;
import uk.ac.unicircle.engagement.EngagementResponses.ServiceSlotResponse;
import uk.ac.unicircle.identity.CurrentIdentity;
import uk.ac.unicircle.identity.CurrentIdentity.SignedInAccount;

@Service
class EngagementService {

    private static final List<String> EVENT_CATEGORIES =
            List.of("ACADEMIC", "CAREERS", "WELLBEING", "SOCIAL");

    private final CurrentIdentity currentIdentity;
    private final JdbcTemplate jdbc;

    EngagementService(CurrentIdentity currentIdentity, JdbcTemplate jdbc) {
        this.currentIdentity = currentIdentity;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    List<EventResponse> events(Authentication authentication, String category) {
        SignedInAccount current = currentIdentity.require(authentication);
        String cleanCategory = category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
        if (!cleanCategory.isEmpty() && !EVENT_CATEGORIES.contains(cleanCategory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid event category.");
        }
        return jdbc.query("""
                SELECT e.id, e.title, e.summary, e.category, e.venue, e.starts_at, e.ends_at,
                       e.capacity,
                       e.capacity - (SELECT COUNT(*) FROM event_registrations registered
                                     WHERE registered.event_id = e.id AND registered.status = 'REGISTERED'),
                       CASE WHEN mine.status = 'REGISTERED' THEN TRUE ELSE FALSE END
                FROM campus_events e
                LEFT JOIN event_registrations mine ON mine.event_id = e.id AND mine.user_id = ?
                WHERE e.university_id = ? AND e.published = TRUE
                  AND (? = '' OR e.category = ?)
                ORDER BY e.starts_at, e.title
                """, (result, row) -> eventResponse(result),
                current.accountId(), current.universityId(), cleanCategory, cleanCategory);
    }

    @Transactional
    EventResponse register(Authentication authentication, UUID eventId) {
        SignedInAccount current = currentIdentity.require(authentication);
        EventLock event = lockEvent(eventId);
        if (!event.universityId().equals(current.universityId()) || !event.published()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This event is unavailable.");
        }
        String existing = registrationStatus(eventId, current.accountId());
        if ("REGISTERED".equals(existing)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already registered for this event.");
        }
        Long registered = jdbc.queryForObject("""
                SELECT COUNT(*) FROM event_registrations
                WHERE event_id = ? AND status = 'REGISTERED'
                """, Long.class, eventId);
        if (registered != null && registered >= event.capacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This event is fully booked.");
        }
        if (existing == null) {
            jdbc.update("""
                    INSERT INTO event_registrations (event_id, user_id, status)
                    VALUES (?, ?, 'REGISTERED')
                    """, eventId, current.accountId());
        } else {
            jdbc.update("""
                    UPDATE event_registrations
                    SET status = 'REGISTERED', registered_at = CURRENT_TIMESTAMP, cancelled_at = NULL
                    WHERE event_id = ? AND user_id = ?
                    """, eventId, current.accountId());
        }
        createNotification(current, "EVENT", "Event registration confirmed",
                event.title() + " has been added to your UniCircle schedule.", "EVENT", eventId);
        return event(current, eventId);
    }

    @Transactional
    EventResponse cancelRegistration(Authentication authentication, UUID eventId) {
        SignedInAccount current = currentIdentity.require(authentication);
        EventLock event = lockEvent(eventId);
        if (!event.universityId().equals(current.universityId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This event is unavailable.");
        }
        int changed = jdbc.update("""
                UPDATE event_registrations
                SET status = 'CANCELLED', cancelled_at = CURRENT_TIMESTAMP
                WHERE event_id = ? AND user_id = ? AND status = 'REGISTERED'
                """, eventId, current.accountId());
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active registration was found.");
        }
        return event(current, eventId);
    }

    @Transactional(readOnly = true)
    List<ServiceOfferingResponse> services(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        return jdbc.query("""
                SELECT id, service_type, title, provider_name, description, location
                FROM service_offerings
                WHERE university_id = ? AND active = TRUE
                ORDER BY CASE service_type WHEN 'APPOINTMENT' THEN 1 WHEN 'FACILITY' THEN 2 ELSE 3 END,
                         title
                """, (result, row) -> new ServiceOfferingResponse(
                        result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                        result.getString(4), result.getString(5), result.getString(6),
                        serviceSlots(current, result.getObject(1, UUID.class))), current.universityId());
    }

    @Transactional
    BookingResponse book(Authentication authentication, UUID slotId) {
        SignedInAccount current = currentIdentity.require(authentication);
        SlotLock slot = lockSlot(slotId);
        if (!slot.universityId().equals(current.universityId()) || !slot.active()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This service slot is unavailable.");
        }
        String existing = bookingStatus(slotId, current.accountId());
        if ("CONFIRMED".equals(existing)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already booked this time.");
        }
        Long overlap = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM service_bookings booking
                JOIN service_slots other_slot ON other_slot.id = booking.slot_id
                WHERE booking.user_id = ? AND booking.status = 'CONFIRMED'
                  AND other_slot.starts_at < ? AND other_slot.ends_at > ?
                """, Long.class, current.accountId(), slot.endsAt(), slot.startsAt());
        if (overlap != null && overlap > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This time overlaps another confirmed service booking.");
        }
        Long confirmed = jdbc.queryForObject("""
                SELECT COUNT(*) FROM service_bookings
                WHERE slot_id = ? AND status = 'CONFIRMED'
                """, Long.class, slotId);
        if (confirmed != null && confirmed >= slot.capacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This service slot is fully booked.");
        }
        UUID bookingId;
        if (existing == null) {
            bookingId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO service_bookings (id, university_id, slot_id, user_id, status)
                    VALUES (?, ?, ?, ?, 'CONFIRMED')
                    """, bookingId, current.universityId(), slotId, current.accountId());
        } else {
            bookingId = jdbc.queryForObject("""
                    SELECT id FROM service_bookings WHERE slot_id = ? AND user_id = ?
                    """, UUID.class, slotId, current.accountId());
            jdbc.update("""
                    UPDATE service_bookings
                    SET status = 'CONFIRMED', booked_at = CURRENT_TIMESTAMP, cancelled_at = NULL
                    WHERE id = ?
                    """, bookingId);
        }
        createNotification(current, "BOOKING", "Booking confirmed",
                slot.title() + " is confirmed for " + slot.startsAt().toLocalDate() + ".",
                "BOOKING", bookingId);
        return booking(current, bookingId);
    }

    @Transactional
    BookingResponse cancelBooking(Authentication authentication, UUID bookingId) {
        SignedInAccount current = currentIdentity.require(authentication);
        int changed = jdbc.update("""
                UPDATE service_bookings
                SET status = 'CANCELLED', cancelled_at = CURRENT_TIMESTAMP
                WHERE id = ? AND university_id = ? AND user_id = ? AND status = 'CONFIRMED'
                """, bookingId, current.universityId(), current.accountId());
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active booking was found.");
        }
        return booking(current, bookingId);
    }

    @Transactional(readOnly = true)
    List<BookingResponse> bookings(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        return jdbc.query("""
                SELECT booking.id, slot.id, service.title, service.provider_name, service.location,
                       slot.starts_at, slot.ends_at, booking.status
                FROM service_bookings booking
                JOIN service_slots slot ON slot.id = booking.slot_id
                JOIN service_offerings service ON service.id = slot.service_id
                WHERE booking.university_id = ? AND booking.user_id = ?
                ORDER BY slot.starts_at, service.title
                """, (result, row) -> bookingResponse(result), current.universityId(), current.accountId());
    }

    @Transactional(readOnly = true)
    List<ScheduleItemResponse> schedule(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        return jdbc.query("""
                SELECT e.id, 'EVENT', e.title, e.venue, e.starts_at, e.ends_at, 'REGISTERED'
                FROM event_registrations registration
                JOIN campus_events e ON e.id = registration.event_id
                WHERE registration.user_id = ? AND registration.status = 'REGISTERED'
                  AND e.university_id = ?
                UNION ALL
                SELECT booking.id, 'BOOKING', service.title, service.location,
                       slot.starts_at, slot.ends_at, booking.status
                FROM service_bookings booking
                JOIN service_slots slot ON slot.id = booking.slot_id
                JOIN service_offerings service ON service.id = slot.service_id
                WHERE booking.user_id = ? AND booking.university_id = ?
                  AND booking.status = 'CONFIRMED'
                ORDER BY 5
                """, (result, row) -> new ScheduleItemResponse(
                        result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                        result.getString(4), result.getObject(5, OffsetDateTime.class),
                        result.getObject(6, OffsetDateTime.class), result.getString(7)),
                current.accountId(), current.universityId(), current.accountId(), current.universityId());
    }

    private List<ServiceSlotResponse> serviceSlots(SignedInAccount current, UUID serviceId) {
        return jdbc.query("""
                SELECT slot.id, slot.starts_at, slot.ends_at, slot.capacity,
                       slot.capacity - (SELECT COUNT(*) FROM service_bookings confirmed
                                        WHERE confirmed.slot_id = slot.id AND confirmed.status = 'CONFIRMED'),
                       CASE WHEN mine.status = 'CONFIRMED' THEN TRUE ELSE FALSE END
                FROM service_slots slot
                LEFT JOIN service_bookings mine ON mine.slot_id = slot.id AND mine.user_id = ?
                WHERE slot.service_id = ? AND slot.active = TRUE
                ORDER BY slot.starts_at
                """, (result, row) -> new ServiceSlotResponse(
                        result.getObject(1, UUID.class), result.getObject(2, OffsetDateTime.class),
                        result.getObject(3, OffsetDateTime.class), result.getInt(4),
                        result.getLong(5), result.getBoolean(6)), current.accountId(), serviceId);
    }

    private EventResponse event(SignedInAccount current, UUID eventId) {
        try {
            return jdbc.queryForObject("""
                    SELECT e.id, e.title, e.summary, e.category, e.venue, e.starts_at, e.ends_at,
                           e.capacity,
                           e.capacity - (SELECT COUNT(*) FROM event_registrations registered
                                         WHERE registered.event_id = e.id AND registered.status = 'REGISTERED'),
                           CASE WHEN mine.status = 'REGISTERED' THEN TRUE ELSE FALSE END
                    FROM campus_events e
                    LEFT JOIN event_registrations mine ON mine.event_id = e.id AND mine.user_id = ?
                    WHERE e.id = ? AND e.university_id = ? AND e.published = TRUE
                    """, (result, row) -> eventResponse(result),
                    current.accountId(), eventId, current.universityId());
        } catch (EmptyResultDataAccessException error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This event is unavailable.", error);
        }
    }

    private BookingResponse booking(SignedInAccount current, UUID bookingId) {
        try {
            return jdbc.queryForObject("""
                    SELECT booking.id, slot.id, service.title, service.provider_name, service.location,
                           slot.starts_at, slot.ends_at, booking.status
                    FROM service_bookings booking
                    JOIN service_slots slot ON slot.id = booking.slot_id
                    JOIN service_offerings service ON service.id = slot.service_id
                    WHERE booking.id = ? AND booking.university_id = ? AND booking.user_id = ?
                    """, (result, row) -> bookingResponse(result),
                    bookingId, current.universityId(), current.accountId());
        } catch (EmptyResultDataAccessException error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This booking is unavailable.", error);
        }
    }

    private EventLock lockEvent(UUID eventId) {
        try {
            return jdbc.queryForObject("""
                    SELECT university_id, capacity, published, title
                    FROM campus_events WHERE id = ? FOR UPDATE
                    """, (result, row) -> new EventLock(
                            result.getObject(1, UUID.class), result.getInt(2),
                            result.getBoolean(3), result.getString(4)), eventId);
        } catch (EmptyResultDataAccessException error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This event is unavailable.", error);
        }
    }

    private SlotLock lockSlot(UUID slotId) {
        try {
            return jdbc.queryForObject("""
                    SELECT service.university_id, slot.capacity, slot.active AND service.active,
                           slot.starts_at, slot.ends_at, service.title
                    FROM service_slots slot
                    JOIN service_offerings service ON service.id = slot.service_id
                    WHERE slot.id = ? FOR UPDATE
                    """, (result, row) -> new SlotLock(
                            result.getObject(1, UUID.class), result.getInt(2), result.getBoolean(3),
                            result.getObject(4, OffsetDateTime.class),
                            result.getObject(5, OffsetDateTime.class), result.getString(6)), slotId);
        } catch (EmptyResultDataAccessException error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This service slot is unavailable.", error);
        }
    }

    private String registrationStatus(UUID eventId, UUID accountId) {
        return jdbc.query("""
                SELECT status FROM event_registrations WHERE event_id = ? AND user_id = ?
                """, result -> result.next() ? result.getString(1) : null, eventId, accountId);
    }

    private String bookingStatus(UUID slotId, UUID accountId) {
        return jdbc.query("""
                SELECT status FROM service_bookings WHERE slot_id = ? AND user_id = ?
                """, result -> result.next() ? result.getString(1) : null, slotId, accountId);
    }

    private EventResponse eventResponse(java.sql.ResultSet result) throws java.sql.SQLException {
        return new EventResponse(
                result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                result.getString(4), result.getString(5), result.getObject(6, OffsetDateTime.class),
                result.getObject(7, OffsetDateTime.class), result.getInt(8),
                result.getLong(9), result.getBoolean(10));
    }

    private BookingResponse bookingResponse(java.sql.ResultSet result) throws java.sql.SQLException {
        return new BookingResponse(
                result.getObject(1, UUID.class), result.getObject(2, UUID.class), result.getString(3),
                result.getString(4), result.getString(5), result.getObject(6, OffsetDateTime.class),
                result.getObject(7, OffsetDateTime.class), result.getString(8));
    }

    private void createNotification(
            SignedInAccount current,
            String type,
            String title,
            String body,
            String resourceType,
            UUID resourceId) {
        jdbc.update("""
                INSERT INTO notifications
                  (id, university_id, recipient_id, notification_type, title, body,
                   resource_type, resource_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), current.universityId(), current.accountId(), type, title, body,
                resourceType, resourceId);
    }

    private record EventLock(UUID universityId, int capacity, boolean published, String title) {
    }

    private record SlotLock(
            UUID universityId,
            int capacity,
            boolean active,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String title) {
    }
}
