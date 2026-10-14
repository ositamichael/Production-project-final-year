package uk.ac.unicircle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class FrontendStructureTests {

    private static final String[] PAGE_NAMES = {
        "campus", "today", "circles", "messages", "events", "services",
        "opportunities", "map", "updates", "live-classes", "wellbeing",
        "lost-found", "pilot-tests", "assistant"
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
    void openingAFeatureWorkspaceAlwaysResolvesItsLiveOrSignedOutState() throws IOException {
        var resource = getClass().getResourceAsStream("/static/js/app.js");
        assertThat(resource).isNotNull();

        var script = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(script)
                .contains("nextPage.dataset.page === 'today') void loadCampusSchedule()")
                .contains("nextPage.dataset.page === 'events') void loadCampusEvents()")
                .contains("nextPage.dataset.page === 'services') void Promise.all([loadCampusServices(), loadCampusBookings()])")
                .contains("nextPage.dataset.page === 'opportunities') void loadOpportunities()")
                .contains("nextPage.dataset.page === 'lost-found') void loadLostFound()");
    }

    @Test
    void mobileNavigationMatchesTheFivePrimaryPhoneDestinations() throws IOException {
        var resource = getClass().getResourceAsStream("/static/index.html");
        assertThat(resource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("class=\"mobile-brand\"")
            .contains("class=\"mobile-tabbar\"")
            .contains("id=\"sidebarClose\"")
            .contains("id=\"sidebarScrim\"")
            .contains("id=\"mobileMessageUnreadBadge\"")
            .contains("id=\"mobileMoreButton\"")
            .contains(">My Campus</span>")
            .contains(">Circles</span>")
            .contains(">Messages</span>")
            .contains(">Events</span>")
            .contains(">More</span>");
    }

    @Test
    void stagingPilotIncludesAcceptanceEvidenceInsideTheCompleteApplication() throws IOException {
        var htmlResource = getClass().getResourceAsStream("/static/index.html");
        var scriptResource = getClass().getResourceAsStream("/static/js/app.js");
        assertThat(htmlResource).isNotNull();
        assertThat(scriptResource).isNotNull();

        var html = new String(htmlResource.readAllBytes(), StandardCharsets.UTF_8);
        var script = new String(scriptResource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
                .contains("id=\"pilotTestNav\"")
                .contains("id=\"pilot-tests-page\"")
                .contains("Pilot Test Centre")
                .contains("This is one UniCircle application")
                .contains("Real testers · fictional campus data");
        assertThat(script)
                .contains("/api/pilot-tests")
                .contains("loadPilotTests")
                .contains("UniCircle Staging Campus");
    }

    @Test
    void prototypeLimitationsRemainVisible() throws IOException {
        var resource = getClass().getResourceAsStream("/static/index.html");
        assertThat(resource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("no official university systems are connected")
            .contains("Transactional sandbox booking")
            .contains("Messages are stored in the local development database")
            .contains("Location sharing is off by default")
            .contains("id=\"locationStart\"")
            .contains("id=\"locationStop\"")
            .contains("both chosen to share");
    }

    @Test
    void guestDashboardDoesNotPresentFictionalRecordsAsPersonalActivity() throws IOException {
        var htmlResource = getClass().getResourceAsStream("/static/index.html");
        var scriptResource = getClass().getResourceAsStream("/static/js/app.js");
        assertThat(htmlResource).isNotNull();
        assertThat(scriptResource).isNotNull();

        var html = new String(htmlResource.readAllBytes(), StandardCharsets.UTF_8);
        var script = new String(scriptResource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("data-time-greeting")
            .contains("id=\"todayWeekday\"")
            .contains("id=\"todayDate\"")
            .contains("Sign in to view your schedule")
            .contains("Private messages are protected")
            .contains("Sign in to view pilot events")
            .doesNotContain("Good morning, <span data-current-user-first-name>Student")
            .doesNotContain("Hi Michael")
            .doesNotContain("<h3>8 October</h3>")
            .doesNotContain("<small>MAR</small>")
            .doesNotContain("Emily Watson");
        assertThat(script)
            .contains("renderSignedOutAccountStates")
            .contains("Sign in required")
            .contains("Europe/London")
            .contains("Fictional pilot timetable")
            .contains("Local guidance")
            .doesNotContain("advanced ? 'Advanced' : 'Sourced'");
    }

    @Test
    void friendLocationIsNotMisrepresentedAsCampusNavigation() throws IOException {
        var resource = getClass().getResourceAsStream("/static/index.html");
        assertThat(resource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("Friend Location</a>")
            .contains("It is not a campus navigation map or emergency tracker")
            .contains("Voluntary sharing controls")
            .doesNotContain("<strong>Campus Map</strong><small>Explore campus</small>");
    }

    @Test
    void nonProfileVisualsUseContextualIconsAndCampusPhotography() throws IOException {
        var resource = getClass().getResourceAsStream("/static/index.html");
        var stylesheetResource = getClass().getResourceAsStream("/static/css/app.css");
        assertThat(resource).isNotNull();
        assertThat(stylesheetResource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        var stylesheet = new String(stylesheetResource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
            .contains("<symbol id=\"laptop\"")
            .contains("<symbol id=\"key\"")
            .contains("<symbol id=\"bottle\"")
            .contains("<symbol id=\"mindfulness\"")
            .contains("<symbol id=\"graduation\"")
            .contains("<symbol id=\"presentation\"")
            .contains("id=\"circleCatalogue\"")
            .contains("id=\"dashboardCircleList\"")
            .contains("<use href=\"#graduation\"/>");

        assertThat(stylesheet)
            .contains("/images/campus-circles-v1.jpg")
            .contains("/images/campus-circles-more-v1.jpg")
            .contains("/images/campus-events-v1.jpg");

        assertThat(html)
            .doesNotContain("class=\"event-thumb careers\">CF")
            .doesNotContain("class=\"feature-avatar blue-photo\">CS")
            .doesNotContain("class=\"company-mark blue-photo\">UC");
    }

    @Test
    void deploymentShellIsInstallableAndPublishesSafetyInformation() throws IOException {
        var htmlResource = getClass().getResourceAsStream("/static/index.html");
        var manifestResource = getClass().getResourceAsStream("/static/manifest.webmanifest");
        var workerResource = getClass().getResourceAsStream("/static/service-worker.js");
        var privacyResource = getClass().getResourceAsStream("/static/privacy.html");
        assertThat(htmlResource).isNotNull();
        assertThat(manifestResource).isNotNull();
        assertThat(workerResource).isNotNull();
        assertThat(privacyResource).isNotNull();

        var html = new String(htmlResource.readAllBytes(), StandardCharsets.UTF_8);
        var worker = new String(workerResource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
                .contains("rel=\"manifest\"")
                .contains("/privacy.html")
                .contains("/acceptable-use.html")
                .contains("/accessibility.html")
                .contains("id=\"circleCreateButton\"")
                .contains("id=\"calendarDownloadButton\"");
        assertThat(worker)
                .contains("/offline.html")
                .contains("url.pathname.startsWith('/api/')")
                .doesNotContain("caches.open(SHELL_CACHE).then((cache) => cache.add('/')");
    }

    @Test
    void profileEditorIsSignInGatedAndSupportsValidatedPhotographs() throws IOException {
        var resource = getClass().getResourceAsStream("/static/index.html");
        var scriptResource = getClass().getResourceAsStream("/static/js/app.js");
        assertThat(resource).isNotNull();
        assertThat(scriptResource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        var script = new String(scriptResource.readAllBytes(), StandardCharsets.UTF_8);
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

        assertThat(script)
            .contains("if (redirectWhenSignedOut) window.location.assign('/sign-in')")
            .contains("if (await loadProfile(true)) openModal(profileDialog)");
    }

    @Test
    void askUniCircleProvidesResponsibleAiWorkspacesAndSourcedResults() throws IOException {
        var htmlResource = getClass().getResourceAsStream("/static/index.html");
        var scriptResource = getClass().getResourceAsStream("/static/js/app.js");
        assertThat(htmlResource).isNotNull();
        assertThat(scriptResource).isNotNull();

        var html = new String(htmlResource.readAllBytes(), StandardCharsets.UTF_8);
        var script = new String(scriptResource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(html)
                .contains("AI STUDY &amp; CAMPUS ASSISTANT")
                .contains("data-assistant-mode=\"UNIVERSITY_RESEARCH\"")
                .contains("data-assistant-mode=\"STUDY_COACH\"")
                .contains("data-assistant-mode=\"CODE_LAB\"")
                .contains("data-assistant-mode=\"IMAGE_STUDIO\"")
                .contains("id=\"assistantStatusBadge\"")
                .contains("id=\"assistantLiveIndicator\"")
                .contains("id=\"assistantNewChat\"")
                .contains("id=\"assistantConversationList\"")
                .contains("id=\"assistantChatTitle\"")
                .contains("Follow your university's assessment rules")
                .contains("do not enter passwords, personal data or confidential information");
        assertThat(script)
                .contains("setNavigationOpen")
                .contains("/api/assistant/status")
                .contains("/api/assistant/ask")
                .contains("/api/assistant/conversations")
                .contains("result.sources")
                .contains("result.imageDataUrl")
                .contains("unicircle-illustration.svg");
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
    void campusInformationAndLiveLearningHaveCompleteInteractiveWorkspaces() throws IOException {
        var html = new String(getClass().getResourceAsStream("/static/index.html").readAllBytes(), StandardCharsets.UTF_8);
        var script = new String(getClass().getResourceAsStream("/static/js/app.js").readAllBytes(), StandardCharsets.UTF_8);

        assertThat(html)
            .contains("id=\"announcementFeed\"")
            .contains("id=\"announcementFilters\"")
            .contains("id=\"announcementPublisherPanel\"")
            .contains("id=\"announcementPublisherForm\"")
            .contains("id=\"meetingList\"")
            .contains("id=\"classroom\"")
            .contains("id=\"meetingJoin\"")
            .contains("id=\"meetingHand\"")
            .contains("id=\"classMessageForm\"")
            .contains("id=\"supportCaseForm\"")
            .contains("id=\"supportCaseList\"")
            .contains("approved meeting provider")
            .contains("approved university mail connection");
        assertThat(script)
            .contains("/api/campus-information/announcements")
            .contains("staffReviewAccess = ['STAFF', 'ADMIN'].includes(profile.role)")
            .contains("/api/campus-information/meetings")
            .contains("/api/campus-information/support-cases")
            .contains("messageType: selectedClassMessageType, body");
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

    @Test
    void dashboardMarkupDoesNotReuseHtmlIdentifiers() throws IOException {
        var resource = getClass().getResourceAsStream("/static/index.html");
        assertThat(resource).isNotNull();

        var html = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        var matcher = Pattern.compile("\\bid=\"([^\"]+)\"").matcher(html);
        var identifiers = new HashSet<String>();
        var duplicates = new HashSet<String>();
        while (matcher.find()) {
            if (!identifiers.add(matcher.group(1))) duplicates.add(matcher.group(1));
        }

        assertThat(duplicates).as("duplicate HTML identifiers").isEmpty();
    }

    @Test
    void authenticationLayoutCanShrinkToTheSmallestSupportedPhone() throws IOException {
        var resource = getClass().getResourceAsStream("/static/css/auth.css");
        assertThat(resource).isNotNull();

        var stylesheet = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(stylesheet)
            .contains(".auth-form-panel { display: grid; min-width: 0;")
            .contains(".auth-form-wrap { width: 100%; max-width: 430px; min-width: 0;")
            .contains("@media (max-width: 360px)")
            .contains("grid-template-columns: auto minmax(0,1fr)");
    }
}
