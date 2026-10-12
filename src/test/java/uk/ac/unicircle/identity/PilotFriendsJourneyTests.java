package uk.ac.unicircle.identity;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("pilot")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:unicircle-pilot;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "server.servlet.session.cookie.secure=false"
})
@Transactional
class PilotFriendsJourneyTests {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void pilotRegistrationRequiresCsrfAndDoesNotExposeFictionalDemoAccounts() throws Exception {
        mockMvc.perform(get("/sign-in"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pilot-login.html"));
        mockMvc.perform(get("/demo-login.html")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/pilot/register").contentType(MediaType.APPLICATION_JSON)
                .content(registration("alice@example.net", "Alice")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/social/directory")).andExpect(status().isUnauthorized());
    }

    @Test
    void registrationValidationIdentifiesTheRejectedField() throws Exception {
        mockMvc.perform(post("/api/pilot/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"tester@example.net","displayName":"Pilot Tester",
                                 "courseName":"Computer Science","yearOfStudy":3,
                                 "password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.password").value("size must be between 12 and 128"));
    }

    @Test
    void publicErrorEndpointDoesNotReplaceRegistrationGuidanceWithUnauthorized() throws Exception {
        mockMvc.perform(get("/error").requestAttr("jakarta.servlet.error.status_code", 400)
                        .requestAttr("jakarta.servlet.error.request_uri", "/api/pilot/register")
                        .requestAttr("jakarta.servlet.error.message", "Invalid registration request"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void twoFriendsCanJoinConnectChatAndShareOnlyWithMutualConsent() throws Exception {
        register("alice@example.net", "Alice Hill");
        register("bob@example.net", "Bob Dean");
        MockHttpSession alice = signIn("alice@example.net");
        MockHttpSession bob = signIn("bob@example.net");

        String bobId = mockMvc.perform(get("/api/social/directory").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Bob Dean"))
                .andReturn().getResponse().getContentAsString();
        UUID targetId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(bobId, "$[0].accountId"));

        String connectionBody = mockMvc.perform(post("/api/social/connections/{id}", targetId)
                        .session(alice).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        UUID connectionId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(connectionBody, "$.connectionId"));
        mockMvc.perform(post("/api/social/connections/{id}/accept", connectionId)
                .session(bob).with(csrf())).andExpect(status().isOk());

        String conversationBody = mockMvc.perform(post("/api/social/conversations/direct/{id}", targetId)
                        .session(alice).with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID conversationId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(conversationBody, "$.conversationId"));
        mockMvc.perform(post("/api/social/conversations/{id}/messages", conversationId)
                        .session(alice).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Meet you at the library.\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/social/conversations/{id}/messages", conversationId).session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].body").value("Meet you at the library."));

        mockMvc.perform(get("/api/location/connections").session(bob))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        share(alice, 53.8013, -1.5486);
        mockMvc.perform(get("/api/location/connections").session(bob))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        share(bob, 53.8051, -1.5500);
        mockMvc.perform(get("/api/location/connections").session(bob))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].displayName").value("Alice Hill"));
        mockMvc.perform(post("/api/social/safety/block/{id}", targetId)
                .session(alice).with(csrf())).andExpect(status().isOk());
        mockMvc.perform(get("/api/location/connections").session(bob))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(delete("/api/location/me").session(alice).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/location/connections").session(bob))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        Integer stored = jdbc.queryForObject("SELECT COUNT(*) FROM location_shares", Integer.class);
        org.assertj.core.api.Assertions.assertThat(stored).isEqualTo(1);
    }

    @Test
    void uploadedPilotProfilePhotoIsStoredAndVisibleToAnotherMember() throws Exception {
        register("dana@example.net", "Dana Gray");
        register("erin@example.net", "Erin Ford");
        MockHttpSession dana = signIn("dana@example.net");
        MockHttpSession erin = signIn("erin@example.net");
        BufferedImage image = new BufferedImage(160, 160, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        var photo = new MockMultipartFile("photo", "profile.png", "image/png", bytes.toByteArray());

        mockMvc.perform(multipart("/api/profile/me/photo").file(photo).session(dana).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasPhoto").value(true));
        UUID danaId = jdbc.queryForObject("SELECT id FROM user_accounts WHERE email = ?",
                UUID.class, "dana@example.net");
        mockMvc.perform(get("/api/profile/{id}/photo", danaId).session(erin))
                .andExpect(status().isOk());
        Integer stored = jdbc.queryForObject("SELECT COUNT(*) FROM pilot_media WHERE media_kind = 'PROFILE'",
                Integer.class);
        org.assertj.core.api.Assertions.assertThat(stored).isEqualTo(1);
    }

    @Test
    void invalidLocationIsRejectedAndExpiredShareIsNotVisible() throws Exception {
        register("carol@example.net", "Carol Lee");
        MockHttpSession carol = signIn("carol@example.net");
        mockMvc.perform(post("/api/location/me").session(carol).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":120,\"longitude\":-1.5,\"accuracyMetres\":5}"))
                .andExpect(status().isBadRequest());
        share(carol, 53.8013, -1.5486);
        mockMvc.perform(get("/api/location/me").session(carol))
                .andExpect(jsonPath("$.sharing").value(true));
        jdbc.update("UPDATE location_shares SET expires_at = ?",
                java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1));
        mockMvc.perform(get("/api/location/me").session(carol))
                .andExpect(jsonPath("$.sharing").value(false));
    }

    @Test
    void campusPilotExposesProfileContextEventsServicesAndOpportunities() throws Exception {
        register("michael@example.net", "Michael Carter");
        MockHttpSession michael = signIn("michael@example.net");

        mockMvc.perform(get("/api/campus/me/context").session(michael))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataSource").value("SELF_DECLARED_PILOT_PROFILE"))
                .andExpect(jsonPath("$.student.programmeTitle").value("Computer Science"))
                .andExpect(jsonPath("$.student.yearOfStudy").value(3))
                .andExpect(jsonPath("$.modules.length()").value(3));
        mockMvc.perform(get("/api/campus-life/events").session(michael))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].title").isNotEmpty());
        mockMvc.perform(get("/api/campus-life/services").session(michael))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].slots.length()").isNumber());
        mockMvc.perform(get("/api/opportunities").session(michael))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].publisherVerified").value(true));
    }

    @Test
    void pilotTesterCanRecordAcceptanceEvidenceAgainstTheCompleteApp() throws Exception {
        register("tester@example.net", "Pilot Tester");
        MockHttpSession tester = signIn("tester@example.net");

        mockMvc.perform(get("/api/pilot-tests").session(tester))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(15))
                .andExpect(jsonPath("$.passed").value(0))
                .andExpect(jsonPath("$.notRun").value(15))
                .andExpect(jsonPath("$.scenarios[0].code").value("ACCOUNT_PROFILE"));

        mockMvc.perform(put("/api/pilot-tests/{code}", "DISCOVERY_REQUEST")
                        .session(tester).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PASS","deviceLabel":"iPhone and Chrome",
                                 "evidenceNote":"Second test account appeared and received one request."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(1))
                .andExpect(jsonPath("$.notRun").value(14))
                .andExpect(jsonPath("$.scenarios[1].status").value("PASS"))
                .andExpect(jsonPath("$.scenarios[1].deviceLabel").value("iPhone and Chrome"));

        Integer stored = jdbc.queryForObject("SELECT COUNT(*) FROM pilot_acceptance_results",
                Integer.class);
        org.assertj.core.api.Assertions.assertThat(stored).isEqualTo(1);
    }

    private void register(String email, String name) throws Exception {
        mockMvc.perform(post("/api/pilot/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(registration(email, name))).andExpect(status().isCreated());
    }

    private MockHttpSession signIn(String email) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/login").with(csrf())
                .param("username", email).param("password", "UniquePilotPassword!2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?signed-in=1#campus"))
                .andReturn().getRequest().getSession(false);
    }

    private void share(MockHttpSession session, double latitude, double longitude) throws Exception {
        mockMvc.perform(post("/api/location/me").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\":" + latitude + ",\"longitude\":" + longitude
                        + ",\"accuracyMetres\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sharing").value(true));
    }

    private String registration(String email, String name) {
        return "{\"email\":\"" + email + "\",\"displayName\":\"" + name
                + "\",\"password\":\"UniquePilotPassword!2026\","
                + "\"courseName\":\"Computer Science\",\"yearOfStudy\":3}";
    }
}
