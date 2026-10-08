package uk.ac.unicircle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class FrontendStructureTests {

    private static final String[] PAGE_NAMES = {
        "campus", "today", "circles", "messages", "events", "services",
        "opportunities", "map", "wellbeing", "lost-found", "assistant"
    };

    @Test
    void everyPrimaryNavigationItemHasADedicatedWorkspace() throws IOException {
        var resource = getClass().getResourceAsStream("/static/index.html");
        assertThat(resource).as("dashboard HTML resource").isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        for (var pageName : PAGE_NAMES) {
            assertThat(html)
                .as("navigation link for %s", pageName)
                .contains("data-page-link=\"" + pageName + "\"");
            assertThat(html)
                .as("workspace for %s", pageName)
                .contains("data-page=\"" + pageName + "\"");
        }
    }

    @Test
    void prototypeLimitationsRemainVisible() throws IOException {
        var resource = getClass().getResourceAsStream("/static/index.html");
        assertThat(resource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("no live university systems are connected yet")
            .contains("No live booking connection")
            .contains("No message is sent or stored")
            .contains("does not request or store device location");
    }

    @Test
    void nonProfileVisualsUseContextualSvgIcons() throws IOException {
        var resource = getClass().getResourceAsStream("/static/index.html");
        assertThat(resource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("<symbol id=\"laptop\"")
            .contains("<symbol id=\"key\"")
            .contains("<symbol id=\"bottle\"")
            .contains("<symbol id=\"mindfulness\"")
            .contains("<symbol id=\"graduation\"")
            .contains("<use href=\"#code\"/>")
            .contains("<use href=\"#presentation\"/>");

        assertThat(html)
            .doesNotContain("class=\"event-thumb careers\">CF")
            .doesNotContain("class=\"feature-avatar blue-photo\">CS")
            .doesNotContain("class=\"company-mark blue-photo\">UC");
    }

    @Test
    void profileEditorIsSignInGatedAndSupportsValidatedPhotographs() throws IOException {
        var resource = getClass().getResourceAsStream("/static/index.html");
        assertThat(resource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("id=\"profileButton\"")
            .contains("class=\"account-card\" id=\"sidebarProfileButton\" type=\"button\" data-open-profile")
            .contains("id=\"universityAccountButton\" type=\"button\" data-open-profile")
            .contains("data-current-university")
            .contains("data-current-account-detail")
            .contains("id=\"universityContext\"")
            .contains("id=\"academicModules\"")
            .contains("id=\"academicCoursemates\"")
            .contains("Fictional sandbox")
            .contains("id=\"profileDialog\"")
            .contains("id=\"profileForm\"")
            .contains("accept=\"image/png,image/jpeg\"")
            .contains("Profile changes require an authenticated university session")
            .contains("remove embedded metadata");
    }
}
