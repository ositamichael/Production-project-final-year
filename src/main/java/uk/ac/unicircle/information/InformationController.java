package uk.ac.unicircle.information;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.unicircle.information.InformationRequests.AddSupportReplyRequest;
import uk.ac.unicircle.information.InformationRequests.CreateSupportCaseRequest;
import uk.ac.unicircle.information.InformationRequests.MeetingMessageRequest;
import uk.ac.unicircle.information.InformationRequests.PublishAnnouncementRequest;
import uk.ac.unicircle.information.InformationRequests.RaiseHandRequest;
import uk.ac.unicircle.information.InformationResponses.AnnouncementResponse;
import uk.ac.unicircle.information.InformationResponses.MeetingMessageResponse;
import uk.ac.unicircle.information.InformationResponses.MeetingRoomResponse;
import uk.ac.unicircle.information.InformationResponses.MeetingSummaryResponse;
import uk.ac.unicircle.information.InformationResponses.SupportCaseResponse;

@RestController
@RequestMapping("/api/campus-information")
class InformationController {

    private final InformationService information;

    InformationController(InformationService information) {
        this.information = information;
    }

    @GetMapping("/announcements")
    List<AnnouncementResponse> announcements(Authentication authentication,
            @RequestParam(defaultValue = "") String category) {
        return information.announcements(authentication, category);
    }

    @PostMapping("/announcements/{announcementId}/read")
    AnnouncementResponse markAnnouncementRead(Authentication authentication,
            @PathVariable UUID announcementId) {
        return information.markAnnouncementRead(authentication, announcementId);
    }

    @PostMapping("/announcements")
    AnnouncementResponse publishAnnouncement(Authentication authentication,
            @Valid @RequestBody PublishAnnouncementRequest request) {
        return information.publishAnnouncement(authentication, request);
    }

    @GetMapping("/support-cases")
    List<SupportCaseResponse> supportCases(Authentication authentication) {
        return information.supportCases(authentication);
    }

    @PostMapping("/support-cases")
    SupportCaseResponse createSupportCase(Authentication authentication,
            @Valid @RequestBody CreateSupportCaseRequest request) {
        return information.createSupportCase(authentication, request);
    }

    @PostMapping("/support-cases/{caseId}/replies")
    SupportCaseResponse addSupportReply(Authentication authentication, @PathVariable UUID caseId,
            @Valid @RequestBody AddSupportReplyRequest request) {
        return information.addSupportReply(authentication, caseId, request);
    }

    @GetMapping("/meetings")
    List<MeetingSummaryResponse> meetings(Authentication authentication) {
        return information.meetings(authentication);
    }

    @GetMapping("/meetings/{meetingId}")
    MeetingRoomResponse meeting(Authentication authentication, @PathVariable UUID meetingId) {
        return information.meetingRoom(authentication, meetingId);
    }

    @PostMapping("/meetings/{meetingId}/join")
    MeetingRoomResponse joinMeeting(Authentication authentication, @PathVariable UUID meetingId) {
        return information.joinMeeting(authentication, meetingId);
    }

    @PostMapping("/meetings/{meetingId}/leave")
    MeetingRoomResponse leaveMeeting(Authentication authentication, @PathVariable UUID meetingId) {
        return information.leaveMeeting(authentication, meetingId);
    }

    @PostMapping("/meetings/{meetingId}/hand")
    MeetingRoomResponse raiseHand(Authentication authentication, @PathVariable UUID meetingId,
            @Valid @RequestBody RaiseHandRequest request) {
        return information.raiseHand(authentication, meetingId, request.raised());
    }

    @PostMapping("/meetings/{meetingId}/messages")
    MeetingMessageResponse addMeetingMessage(Authentication authentication, @PathVariable UUID meetingId,
            @Valid @RequestBody MeetingMessageRequest request) {
        return information.addMeetingMessage(authentication, meetingId, request);
    }
}
