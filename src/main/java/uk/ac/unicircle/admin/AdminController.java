package uk.ac.unicircle.admin;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.unicircle.admin.AdminRequests.CreateEventRequest;
import uk.ac.unicircle.admin.AdminRequests.CreateMeetingRequest;
import uk.ac.unicircle.admin.AdminRequests.CreateOpportunityRequest;
import uk.ac.unicircle.admin.AdminRequests.CreateServiceRequest;
import uk.ac.unicircle.admin.AdminRequests.CreateSlotRequest;
import uk.ac.unicircle.admin.AdminRequests.UpdateSafetyReportRequest;
import uk.ac.unicircle.admin.AdminRequests.UpdateSupportCaseRequest;
import uk.ac.unicircle.admin.AdminRequests.UpdateUserRequest;
import uk.ac.unicircle.admin.AdminResponses.AuditResponse;
import uk.ac.unicircle.admin.AdminResponses.EventAdminResponse;
import uk.ac.unicircle.admin.AdminResponses.MeetingAdminResponse;
import uk.ac.unicircle.admin.AdminResponses.OpportunityAdminResponse;
import uk.ac.unicircle.admin.AdminResponses.OverviewResponse;
import uk.ac.unicircle.admin.AdminResponses.PublisherResponse;
import uk.ac.unicircle.admin.AdminResponses.SafetyAdminResponse;
import uk.ac.unicircle.admin.AdminResponses.ServiceAdminResponse;
import uk.ac.unicircle.admin.AdminResponses.SlotAdminResponse;
import uk.ac.unicircle.admin.AdminResponses.SupportAdminResponse;
import uk.ac.unicircle.admin.AdminResponses.UserResponse;

@RestController
@RequestMapping("/api/admin")
class AdminController {
    private final AdminService admin;

    AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/overview")
    OverviewResponse overview(Authentication authentication) {
        return admin.overview(authentication);
    }

    @GetMapping("/users")
    List<UserResponse> users(Authentication authentication,
            @RequestParam(defaultValue = "") String query) {
        return admin.users(authentication, query);
    }

    @PatchMapping("/users/{accountId}")
    UserResponse updateUser(Authentication authentication, @PathVariable UUID accountId,
            @Valid @RequestBody UpdateUserRequest request) {
        return admin.updateUser(authentication, accountId, request);
    }

    @PostMapping("/events")
    EventAdminResponse createEvent(Authentication authentication,
            @Valid @RequestBody CreateEventRequest request) {
        return admin.createEvent(authentication, request);
    }

    @PatchMapping("/events/{eventId}/published")
    EventAdminResponse setEventPublished(Authentication authentication, @PathVariable UUID eventId,
            @RequestParam boolean value) {
        return admin.setEventPublished(authentication, eventId, value);
    }

    @PostMapping("/services")
    ServiceAdminResponse createService(Authentication authentication,
            @Valid @RequestBody CreateServiceRequest request) {
        return admin.createService(authentication, request);
    }

    @PatchMapping("/services/{serviceId}/active")
    ServiceAdminResponse setServiceActive(Authentication authentication, @PathVariable UUID serviceId,
            @RequestParam boolean value) {
        return admin.setServiceActive(authentication, serviceId, value);
    }

    @PostMapping("/services/{serviceId}/slots")
    SlotAdminResponse createSlot(Authentication authentication, @PathVariable UUID serviceId,
            @Valid @RequestBody CreateSlotRequest request) {
        return admin.createSlot(authentication, serviceId, request);
    }

    @PostMapping("/meetings")
    MeetingAdminResponse createMeeting(Authentication authentication,
            @Valid @RequestBody CreateMeetingRequest request) {
        return admin.createMeeting(authentication, request);
    }

    @GetMapping("/opportunity-publishers")
    List<PublisherResponse> publishers(Authentication authentication) {
        return admin.publishers(authentication);
    }

    @PostMapping("/opportunities")
    OpportunityAdminResponse createOpportunity(Authentication authentication,
            @Valid @RequestBody CreateOpportunityRequest request) {
        return admin.createOpportunity(authentication, request);
    }

    @GetMapping("/support-cases")
    List<SupportAdminResponse> supportCases(Authentication authentication) {
        return admin.supportCases(authentication);
    }

    @PatchMapping("/support-cases/{caseId}")
    SupportAdminResponse updateSupportCase(Authentication authentication, @PathVariable UUID caseId,
            @Valid @RequestBody UpdateSupportCaseRequest request) {
        return admin.updateSupportCase(authentication, caseId, request);
    }

    @GetMapping("/safety-reports")
    List<SafetyAdminResponse> safetyReports(Authentication authentication) {
        return admin.safetyReports(authentication);
    }

    @PatchMapping("/safety-reports/{reportId}")
    SafetyAdminResponse updateSafetyReport(Authentication authentication, @PathVariable UUID reportId,
            @Valid @RequestBody UpdateSafetyReportRequest request) {
        return admin.updateSafetyReport(authentication, reportId, request);
    }

    @GetMapping("/audit")
    List<AuditResponse> audit(Authentication authentication) {
        return admin.auditLog(authentication);
    }
}
