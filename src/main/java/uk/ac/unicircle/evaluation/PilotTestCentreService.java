package uk.ac.unicircle.evaluation;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import uk.ac.unicircle.evaluation.PilotTestResponses.PilotScenarioResponse;
import uk.ac.unicircle.evaluation.PilotTestResponses.PilotTestCentreResponse;
import uk.ac.unicircle.identity.CurrentIdentity;
import uk.ac.unicircle.identity.CurrentIdentity.SignedInAccount;

@Service
@Profile("pilot")
class PilotTestCentreService {

    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario("ACCOUNT_PROFILE", "Account", "Create and complete a profile",
                    "Register a fresh invited account, sign in, add course and year, and upload a profile photograph.",
                    "The account remains available after signing out and back in, including its saved profile photograph."),
            new Scenario("DISCOVERY_REQUEST", "Connections", "Find a student and send a request",
                    "Use two accounts. Find the second tester in Find Students and send a connection request.",
                    "Only permitted profile information is visible and the receiver gets one pending request."),
            new Scenario("REQUEST_ACCEPT", "Connections", "Accept a connection request",
                    "Sign in as the receiver and accept the pending request.",
                    "Both testers become accepted connections and can open a private conversation."),
            new Scenario("LIVE_MESSAGES", "Communication", "Exchange live messages",
                    "Keep both devices open and send messages in both directions.",
                    "Messages arrive for the correct accounts, unread indicators update and history survives refresh."),
            new Scenario("VOICE_CALL", "Communication", "Complete a voice call",
                    "Start a voice call from an accepted conversation, accept it on the second device, mute and end it.",
                    "Both testers hear each other and either person can safely end the call."),
            new Scenario("VIDEO_CALL", "Communication", "Complete a video call",
                    "Start a video call, accept camera permission, switch the camera off and on, then end the call.",
                    "Audio and video connect only after consent and the controls work on both devices."),
            new Scenario("LOCATION_CONSENT", "Safety", "Share location by mutual consent",
                    "Enable five-minute location sharing on both accepted accounts, then stop it on one device.",
                    "Neither location is visible until both opt in, and the stopped or expired location disappears."),
            new Scenario("BLOCK_REPORT", "Safety", "Block and report an account",
                    "Use test content to report an account and then block it.",
                    "Messaging, calling and location access stop for the blocked connection."),
            new Scenario("CAMPUS_UPDATE", "Campus information", "Receive a targeted announcement",
                    "Publish a staging announcement for the tester's course or year and mark it as read.",
                    "Only the intended audience receives the labelled test announcement and its read state is saved."),
            new Scenario("EVENT_BOOKING", "Campus services", "Register and book a service",
                    "Register for a staging event and reserve one available service slot.",
                    "Both commitments appear in Today and a slot cannot be double-booked."),
            new Scenario("OPPORTUNITY", "Opportunities", "Track an opportunity",
                    "Save a staging opportunity and update its private application progress.",
                    "The opportunity remains saved and the progress is visible only to that account."),
            new Scenario("SUPPORT_CASE", "Student support", "Create a private support case",
                    "Create a non-sensitive test support request and add a follow-up reply.",
                    "A reference number is produced and the request remains private to the signed-in tester."),
            new Scenario("LIVE_CLASS", "Learning", "Join and participate in a live class",
                    "Join a staging class, post in chat, ask a question, raise your hand and leave.",
                    "Attendance and participation are recorded for the correct account without claiming unconnected video."),
            new Scenario("LOST_FOUND", "Campus services", "Report a lost item",
                    "Create a fictional lost-item report with a safe test image and review its status.",
                    "The report is stored, displayed with the correct ownership controls and can enter staff review."),
            new Scenario("MOBILE_ACCESS", "Quality", "Complete a mobile usability check",
                    "Repeat the main dashboard, connection, message and booking journeys on a phone.",
                    "Navigation remains usable without sideways scrolling and important controls remain readable."));

    private final CurrentIdentity currentIdentity;
    private final JdbcTemplate jdbc;

    PilotTestCentreService(CurrentIdentity currentIdentity, JdbcTemplate jdbc) {
        this.currentIdentity = currentIdentity;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    PilotTestCentreResponse testCentre(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        Map<String, StoredResult> stored = new LinkedHashMap<>();
        jdbc.query("""
                SELECT scenario_code, status, evidence_note, device_label, tested_at
                FROM pilot_acceptance_results
                WHERE university_id = ? AND tester_id = ?
                """, (result, row) -> Map.entry(result.getString(1), new StoredResult(
                        result.getString(2), result.getString(3), result.getString(4),
                        result.getObject(5, OffsetDateTime.class))),
                current.universityId(), current.accountId())
                .forEach(entry -> stored.put(entry.getKey(), entry.getValue()));

        List<PilotScenarioResponse> responses = SCENARIOS.stream().map(scenario -> {
            StoredResult result = stored.getOrDefault(scenario.code(), StoredResult.EMPTY);
            return new PilotScenarioResponse(scenario.code(), scenario.area(), scenario.title(),
                    scenario.instruction(), scenario.expectedResult(), result.status(),
                    result.evidenceNote(), result.deviceLabel(), result.testedAt());
        }).toList();
        return summary(responses);
    }

    @Transactional
    PilotTestCentreResponse record(Authentication authentication, String scenarioCode,
            RecordPilotResultRequest request) {
        SignedInAccount current = currentIdentity.require(authentication);
        String cleanCode = scenarioCode == null ? "" : scenarioCode.strip().toUpperCase();
        SCENARIOS.stream().filter(item -> item.code().equals(cleanCode)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "This pilot test scenario is unavailable."));
        String note = clean(request.evidenceNote());
        String device = clean(request.deviceLabel());
        OffsetDateTime testedAt = "NOT_RUN".equals(request.status()) ? null : OffsetDateTime.now();
        int changed = jdbc.update("""
                UPDATE pilot_acceptance_results
                SET status = ?, evidence_note = ?, device_label = ?, tested_at = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE university_id = ? AND tester_id = ? AND scenario_code = ?
                """, request.status(), note, device, testedAt,
                current.universityId(), current.accountId(), cleanCode);
        if (changed == 0) {
            jdbc.update("""
                    INSERT INTO pilot_acceptance_results
                      (id, university_id, tester_id, scenario_code, status,
                       evidence_note, device_label, tested_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), current.universityId(), current.accountId(), cleanCode,
                    request.status(), note, device, testedAt);
        }
        return testCentre(authentication);
    }

    private PilotTestCentreResponse summary(List<PilotScenarioResponse> scenarios) {
        int passed = count(scenarios, "PASS");
        int failed = count(scenarios, "FAIL");
        int blocked = count(scenarios, "BLOCKED");
        int notRun = count(scenarios, "NOT_RUN");
        return new PilotTestCentreResponse(scenarios.size(), passed, failed, blocked, notRun, scenarios);
    }

    private int count(List<PilotScenarioResponse> scenarios, String status) {
        return (int) scenarios.stream().filter(item -> status.equals(item.status())).count();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private record Scenario(String code, String area, String title, String instruction,
            String expectedResult) {
    }

    private record StoredResult(String status, String evidenceNote, String deviceLabel,
            OffsetDateTime testedAt) {
        private static final StoredResult EMPTY = new StoredResult("NOT_RUN", null, null, null);
    }
}
