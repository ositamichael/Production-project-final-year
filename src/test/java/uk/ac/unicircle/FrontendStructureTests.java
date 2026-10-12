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
            .contains("no live university systems are connected")
            .contains("Transactional sandbox booking")
            .contains("Messages are stored in the local development database")
            .contains("Location sharing is off by default")
            .contains("id=\"locationStart\"")
            .contains("id=\"locationStop\"")
            .contains("both chosen to share");
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
            .contains("<symbol id=\"presentation\"")
            .contains("<use href=\"#code\"/>")
            .contains("<use href=\"#graduation\"/>");

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
            .contains("id=\"profileSignIn\"")
            .contains("href=\"/sign-in\"")
            .contains("id=\"profileSignOut\"")
            .contains("accept=\"image/png,image/jpeg\"")
            .contains("Profile changes require an authenticated university session")
            .contains("remove embedded metadata");
    }

    @Test
    void fictionalSignInPageIsExplicitlySeparatedFromUniversityAuthentication() throws IOException {
        var resource = getClass().getResourceAsStream("/static/demo-login.html");
        assertThat(resource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("FICTIONAL DEVELOPMENT SANDBOX")
            .contains("student@demo.unicircle.test")
            .contains("Never enter your Leeds Beckett password")
            .contains("does not contact Leeds Beckett, Microsoft, or Authenticator")
            .contains("action=\"/login\"")
            .contains("id=\"csrfField\"");
    }

    @Test
    void messagingWorkspaceSupportsConsentBasedStudentDiscovery() throws IOException {
        var resource = getClass().getResourceAsStream("/static/index.html");
        assertThat(resource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("id=\"studentDirectoryButton\"")
            .contains("id=\"studentDirectoryDialog\"")
            .contains("id=\"studentDirectoryList\"")
            .contains("id=\"conversationList\"")
            .contains("id=\"conversationMessages\"")
            .contains("id=\"messageComposer\"")
            .contains("id=\"notificationList\"")
            .contains("id=\"conversationSafetyButton\"")
            .contains("id=\"safetyDialog\"")
            .contains("id=\"blockAccountButton\"")
            .contains("id=\"reportAccountForm\"")
            .contains("Messaging activates after a request is accepted");
    }

    @Test
    void acceptedConnectionsHaveRealTimeVoiceAndVideoCallControls() throws IOException {
        var html = new String(getClass().getResourceAsStream("/static/index.html").readAllBytes(), StandardCharsets.UTF_8);
        var script = new String(getClass().getResourceAsStream("/static/js/app.js").readAllBytes(), StandardCharsets.UTF_8);

        assertThat(html)
            .contains("id=\"audioCallButton\"")
            .contains("id=\"videoCallButton\"")
            .contains("id=\"callDialog\"")
            .contains("id=\"callAccept\"")
            .contains("id=\"callDecline\"")
            .contains("id=\"callMute\"")
            .contains("id=\"callCamera\"")
            .contains("UniCircle does not record calls");
        assertThat(script)
            .contains("new RTCPeerConnection")
            .contains("navigator.mediaDevices.getUserMedia")
            .contains("CALL_OFFER")
            .contains("CALL_ANSWER")
            .contains("CALL_ICE_CANDIDATE")
            .contains("Calls require a secure HTTPS address");
    }

    @Test
    void demonstrationSignInOffersSeparateFictionalStudentAccounts() throws IOException {
        var resource = getClass().getResourceAsStream("/static/demo-login.html");
        assertThat(resource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("Michael Carter — connected with Aisha")
            .contains("aisha.khan@students.unicircle.test")
            .contains("James Wilson — new student")
            .contains("Use separate browser sessions to test reports, review decisions and live chat");
    }

    @Test
    void eventsAndServiceBookingsHaveAccountSpecificWorkspaces() throws IOException {
        var resource = getClass().getResourceAsStream("/static/index.html");
        assertThat(resource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("id=\"eventCatalogue\"")
            .contains("id=\"eventFilters\"")
            .contains("id=\"serviceCatalogue\"")
            .contains("id=\"bookingActivity\"")
            .contains("id=\"bookingDateStrip\"")
            .contains("id=\"bookingTimeStrip\"")
            .contains("id=\"todayCommitmentList\"")
            .contains("id=\"todayCampusCommitmentCount\"")
            .contains("Account-specific registrations")
            .contains("Transactional sandbox booking");
    }

    @Test
    void verifiedOpportunitiesHaveSearchDetailsAndPrivateTracking() throws IOException {
        var resource = getClass().getResourceAsStream("/static/index.html");
        assertThat(resource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("id=\"opportunityCatalogue\"")
            .contains("id=\"opportunitySearch\"")
            .contains("id=\"opportunityDialog\"")
            .contains("id=\"opportunityTrackingStatus\"")
            .contains("id=\"dashboardOpportunityList\"")
            .contains("Verified publisher")
            .contains("it does not submit an application to an employer");
    }

    @Test
    void lostAndFoundSeparatesPublicReportsFromPrivateOwnershipEvidence() throws IOException {
        var resource = getClass().getResourceAsStream("/static/index.html");
        assertThat(resource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("id=\"lostFoundCatalogue\"")
            .contains("id=\"lostFoundReportDialog\"")
            .contains("id=\"lostFoundDetailDialog\"")
            .contains("id=\"lostFoundPrivateDetails\"")
            .contains("id=\"lostFoundClaimForm\"")
            .contains("id=\"lostFoundPhoto\"")
            .contains("Submit for moderation")
            .contains("Ownership evidence is never published")
            .contains("Photographs are reprocessed to remove embedded metadata")
            .doesNotContain("Lost-item report opened in prototype mode");
    }

    @Test
    void staffReviewHasASeparateWorkspaceAndTwoStepDecision() throws IOException {
        var html = new String(getClass().getResourceAsStream("/static/index.html").readAllBytes(), StandardCharsets.UTF_8);
        var script = new String(getClass().getResourceAsStream("/static/js/app.js").readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("data-page-link=\"lost-found-review\"")
            .contains("data-page=\"lost-found-review\"")
            .contains("id=\"staffReviewList\"")
            .contains("id=\"staffReviewDialog\"")
            .contains("name=\"staffDecision\"")
            .contains("id=\"staffReviewReason\"");
        assertThat(script)
            .contains("['STAFF', 'ADMIN'].includes(profile.role)")
            .contains("/api/lost-found/staff/reports/")
            .contains("staffReviewNav.hidden = !staffReviewAccess")
            .contains("REJECTED: 'NOT PUBLISHED'")
            .contains("detailStatus.textContent = report.moderationStatus === 'APPROVED'");
    }
}
