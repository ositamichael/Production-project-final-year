package uk.ac.unicircle.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
class DemoSignInJourneyTests {

    private static final String USERNAME = "student@demo.unicircle.test";
    private static final String PASSWORD = "CampusDemo!2026";

    @Autowired
    MockMvc mockMvc;

    @Test
    void signInEntryPointOpensOnlyTheFictionalSandboxInDevelopment() throws Exception {
        mockMvc.perform(get("/sign-in"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demo-login.html"));

        mockMvc.perform(get("/demo-login.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "FICTIONAL DEVELOPMENT SANDBOX")));
    }

    @Test
    void campusPhotographyLoadsBeforeSignIn() throws Exception {
        mockMvc.perform(get("/images/campus-events-v1.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
    }

    @Test
    void installableShellAndSafetyPagesLoadBeforeSignIn() throws Exception {
        mockMvc.perform(get("/manifest.webmanifest"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("UniCircle Campus")));
        mockMvc.perform(get("/service-worker.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SHELL_CACHE")));
        mockMvc.perform(get("/privacy.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Your privacy in UniCircle")));
    }

    @Test
    void publicPagesReceiveBrowserSecurityHeaders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("default-src 'self'")))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy",
                        org.hamcrest.Matchers.containsString("camera=(self)")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void validFictionalCredentialsCreateAProtectedSessionAndExposeTheDemoStudent() throws Exception {
        var login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", USERNAME)
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?signed-in=1#campus"))
                .andExpect(authenticated().withUsername(USERNAME))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/profile/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Michael Carter"))
                .andExpect(jsonPath("$.email").value("michael.carter@demo.university.example"));

        mockMvc.perform(get("/api/campus/me/context").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataSource").value("FICTIONAL_DEVELOPMENT_FIXTURE"))
                .andExpect(jsonPath("$.modules.length()").value(3))
                .andExpect(jsonPath("$.coursemates.length()").value(3));
    }

    @Test
    void incorrectCredentialsAreRejectedWithoutCreatingAStudentSession() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", USERNAME)
                        .param("password", "not-the-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demo-login.html?error=1"))
                .andExpect(unauthenticated());
    }

    @Test
    void logoutInvalidatesTheAuthenticatedJourney() throws Exception {
        var login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", USERNAME)
                        .param("password", PASSWORD))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/?signed-out=1#campus"))
                .andExpect(unauthenticated());
    }
}
