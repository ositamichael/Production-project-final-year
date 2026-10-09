package uk.ac.unicircle.engagement;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class EngagementControllerTests {

    private static final UUID MICHAEL_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be101");
    private static final UUID MINDFULNESS_EVENT = UUID.fromString("711a6b0d-183b-4d9a-9c82-bbbd78553102");
    private static final UUID LECTURER_SLOT = UUID.fromString("9499216e-640c-4019-866b-9d8ad7d7c101");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void campusLifeDataRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/campus-life/events"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/campus-life/services"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/campus-life/bookings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publishedEventsAreScopedAndFilterable() throws Exception {
        mockMvc.perform(get("/api/campus-life/events").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].title", hasItem("Spring Careers Fair")));

        mockMvc.perform(get("/api/campus-life/events")
                        .param("category", "wellbeing")
                        .with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Mindfulness Workshop"));
    }

    @Test
    void studentCanRegisterAndCancelAnEvent() throws Exception {
        mockMvc.perform(post("/api/campus-life/events/{eventId}/register", MINDFULNESS_EVENT)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(true));

        Integer registered = jdbc.queryForObject("""
                SELECT COUNT(*) FROM event_registrations
                WHERE event_id = ? AND user_id = ? AND status = 'REGISTERED'
                """, Integer.class, MINDFULNESS_EVENT, MICHAEL_ID);
        org.assertj.core.api.Assertions.assertThat(registered).isEqualTo(1);

        mockMvc.perform(post("/api/campus-life/events/{eventId}/cancel", MINDFULNESS_EVENT)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(false));
    }

    @Test
    void duplicateEventRegistrationIsRejected() throws Exception {
        mockMvc.perform(post("/api/campus-life/events/{eventId}/register", MINDFULNESS_EVENT)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/campus-life/events/{eventId}/register", MINDFULNESS_EVENT)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void serviceAvailabilityReflectsAnotherStudentsBooking() throws Exception {
        mockMvc.perform(get("/api/campus-life/services").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.title == 'Library study room')].slots[0].placesRemaining")
                        .value(3));
    }

    @Test
    void studentCanBookAndCancelAServiceSlot() throws Exception {
        String response = mockMvc.perform(post("/api/campus-life/service-slots/{slotId}/book", LECTURER_SLOT)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceTitle").value("Lecturer appointment"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn().getResponse().getContentAsString();

        String bookingId = response.replaceFirst(".*\\\"bookingId\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/api/campus-life/bookings/{bookingId}/cancel", bookingId)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void duplicateServiceBookingAndMissingCsrfAreRejected() throws Exception {
        mockMvc.perform(post("/api/campus-life/service-slots/{slotId}/book", LECTURER_SLOT)
                        .with(user("student")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/campus-life/service-slots/{slotId}/book", LECTURER_SLOT)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/campus-life/service-slots/{slotId}/book", LECTURER_SLOT)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmedActivityAppearsInTheStudentsSchedule() throws Exception {
        mockMvc.perform(post("/api/campus-life/service-slots/{slotId}/book", LECTURER_SLOT)
                        .with(user("student"))
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/campus-life/schedule").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Spring Careers Fair")))
                .andExpect(jsonPath("$[*].title", hasItem("Lecturer appointment")));
    }
}
