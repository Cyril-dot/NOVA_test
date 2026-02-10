package com.novaTech.Nova.controller;

import com.novaTech.Nova.Entities.meeting.Meeting;
import com.novaTech.Nova.Entities.meeting.MeetingParticipant;
import com.novaTech.Nova.Entities.meeting.ScheduledMeeting;
import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.ZoomServiceImpl.MeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingController {

    private final MeetingService meetingService;

    private UserPrincipal getUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new RuntimeException("User not authenticated");
        }
        return (UserPrincipal) authentication.getPrincipal();
    }

    /**
     * Create an instant meeting
     * POST /api/meetings/instant
     */
    @PostMapping("/instant")
    public ResponseEntity<MeetingResponse> createInstantMeeting(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean waitingRoomEnabled,
            @RequestParam(required = false) Boolean recordingEnabled,
            @RequestParam(required = false) UUID teamId) {

        UUID hostId = getUserPrincipal().getUserId();

        Meeting meeting = meetingService.createInstantMeeting(
                hostId, title, waitingRoomEnabled, recordingEnabled, teamId
        );

        // Get call credentials for the host
        Map<String, Object> callCredentials = meetingService.getCallCredentials(meeting.getId(), hostId);

        return ResponseEntity.ok(MeetingResponse.builder()
                .meetingId(meeting.getId())
                .meetingLink(meeting.getMeetingLink())
                .title(meeting.getTitle())
                .status(meeting.getStatus().toString())
                .callCredentials(callCredentials)
                .message("Meeting created successfully. Use the call credentials to join.")
                .build());
    }

    /**
     * Schedule a meeting
     */
    @PostMapping("/schedule")
    public ResponseEntity<ScheduledMeeting> scheduleMeeting(@RequestBody ScheduleMeetingRequest request) {
        UUID organizerId = getUserPrincipal().getUserId();

        ScheduledMeeting meeting = meetingService.scheduleMeeting(
                organizerId,
                request.getTitle(),
                request.getDescription(),
                request.getStartTime(),
                request.getEndTime(),
                request.getTimezone(),
                request.getInvitedUserIds(),
                request.getTeamId()
        );

        return ResponseEntity.ok(meeting);
    }

    /**
     * Get call credentials for joining a meeting
     * This endpoint automatically provisions the user in GetStream
     *
     * GET /api/meetings/{meetingId}/credentials
     */
    @GetMapping("/{meetingId}/credentials")
    public ResponseEntity<CallCredentialsResponse> getCallCredentials(@PathVariable Long meetingId) {
        UUID userId = getUserPrincipal().getUserId();

        Map<String, Object> credentials = meetingService.getCallCredentials(meetingId, userId);

        return ResponseEntity.ok(CallCredentialsResponse.builder()
                .credentials(credentials)
                .message("Use these credentials to initialize the GetStream Video call on your frontend")
                .build());
    }

    /**
     * Join a meeting (records participation in database)
     */
    @PostMapping("/{meetingId}/join")
    public ResponseEntity<JoinMeetingResponse> joinMeeting(@PathVariable Long meetingId) {
        UUID userId = getUserPrincipal().getUserId();

        // Record participation
        MeetingParticipant participant = meetingService.joinMeeting(meetingId, userId);

        // Get call credentials
        Map<String, Object> credentials = meetingService.getCallCredentials(meetingId, userId);

        return ResponseEntity.ok(JoinMeetingResponse.builder()
                .participant(participant)
                .callCredentials(credentials)
                .message("Successfully joined meeting. Use credentials to connect to video call.")
                .build());
    }

    /**
     * Leave a meeting
     */
    @PostMapping("/{meetingId}/leave")
    public ResponseEntity<Void> leaveMeeting(@PathVariable Long meetingId) {
        UUID userId = getUserPrincipal().getUserId();
        meetingService.leaveMeeting(meetingId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * End a meeting (host only)
     */
    @PostMapping("/{meetingId}/end")
    public ResponseEntity<Void> endMeeting(@PathVariable Long meetingId) {
        UUID userId = getUserPrincipal().getUserId();
        meetingService.endMeeting(meetingId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Admit participant from waiting room
     */
    @PostMapping("/{meetingId}/admit")
    public ResponseEntity<Void> admitFromWaitingRoom(
            @PathVariable Long meetingId,
            @RequestParam UUID participantUserId) {

        UUID hostId = getUserPrincipal().getUserId();
        meetingService.admitFromWaitingRoom(meetingId, participantUserId, hostId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get active participants
     */
    @GetMapping("/{meetingId}/participants")
    public ResponseEntity<List<MeetingParticipant>> getActiveParticipants(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingService.getActiveParticipants(meetingId));
    }

    /**
     * Get waiting room participants
     */
    @GetMapping("/{meetingId}/waiting-room")
    public ResponseEntity<List<MeetingParticipant>> getWaitingRoomParticipants(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingService.getWaitingRoomParticipants(meetingId));
    }

    /**
     * Get meeting details
     */
    @GetMapping("/{meetingId}")
    public ResponseEntity<Meeting> getMeeting(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingService.getMeetingById(meetingId));
    }

    /**
     * Get user's meetings
     */
    @GetMapping("/my")
    public ResponseEntity<List<Meeting>> getUserMeetings() {
        UUID userId = getUserPrincipal().getUserId();
        return ResponseEntity.ok(meetingService.getUserMeetings(userId));
    }

    // DTOs
    @lombok.Data
    @lombok.Builder
    public static class MeetingResponse {
        private Long meetingId;
        private String meetingLink;
        private String title;
        private String status;
        private Map<String, Object> callCredentials;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    public static class JoinMeetingResponse {
        private MeetingParticipant participant;
        private Map<String, Object> callCredentials;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    public static class CallCredentialsResponse {
        private Map<String, Object> credentials;
        private String message;
    }

    @lombok.Data
    public static class ScheduleMeetingRequest {
        private String title;
        private String description;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String timezone;
        private List<UUID> invitedUserIds;
        private UUID teamId;
    }
}