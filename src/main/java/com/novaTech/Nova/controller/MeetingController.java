package com.novaTech.Nova.controller;

import com.novaTech.Nova.Entities.meeting.Meeting;
import com.novaTech.Nova.Entities.meeting.MeetingParticipant;
import com.novaTech.Nova.Entities.meeting.ScheduledMeeting;
import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.ZoomServiceImpl.MeetingService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
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

    @PostMapping("/instant")
    public ResponseEntity<Map<String, Object>> createInstantMeeting(@RequestBody InstantMeetingRequest request) {
        log.info("Creating instant meeting with request: {}", request);

        UUID hostId = getUserPrincipal().getUserId();

        Meeting meeting = meetingService.createInstantMeeting(
                hostId,
                request.getTitle(),
                request.getWaitingRoomEnabled(),
                request.getRecordingEnabled(),
                request.getTeamId()
        );

        Map<String, Object> callCredentials = meetingService.getCallCredentials(meeting.getId(), hostId);

        Map<String, Object> response = new HashMap<>();
        response.put("meetingId", meeting.getId());
        response.put("meetingLink", meeting.getMeetingLink());
        response.put("title", meeting.getTitle());
        response.put("status", meeting.getStatus().toString());
        response.put("callCredentials", callCredentials);
        response.put("message", "Meeting created successfully");

        return ResponseEntity.ok(response);
    }

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

    @GetMapping("/{meetingId}/credentials")
    public ResponseEntity<Map<String, Object>> getCallCredentials(@PathVariable Long meetingId) {
        UUID userId = getUserPrincipal().getUserId();

        Map<String, Object> credentials = meetingService.getCallCredentials(meetingId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("credentials", credentials);
        response.put("message", "Use these credentials to initialize the GetStream Video call");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{meetingId}/join")
    public ResponseEntity<Map<String, Object>> joinMeeting(@PathVariable Long meetingId) {
        UUID userId = getUserPrincipal().getUserId();

        log.info("User {} joining meeting {}", userId, meetingId);

        MeetingParticipant participant = meetingService.joinMeeting(meetingId, userId);
        Map<String, Object> credentials = meetingService.getCallCredentials(meetingId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("participant", participant);
        response.put("callCredentials", credentials);
        response.put("message", "Successfully joined meeting");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{meetingId}/leave")
    public ResponseEntity<Map<String, String>> leaveMeeting(@PathVariable Long meetingId) {
        UUID userId = getUserPrincipal().getUserId();
        log.info("User {} leaving meeting {}", userId, meetingId);
        meetingService.leaveMeeting(meetingId, userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully left meeting");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{meetingId}/end")
    public ResponseEntity<Map<String, String>> endMeeting(@PathVariable Long meetingId) {
        UUID userId = getUserPrincipal().getUserId();
        meetingService.endMeeting(meetingId, userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Meeting ended successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{meetingId}/admit")
    public ResponseEntity<Map<String, String>> admitFromWaitingRoom(
            @PathVariable Long meetingId,
            @RequestParam UUID participantUserId) {

        UUID hostId = getUserPrincipal().getUserId();
        meetingService.admitFromWaitingRoom(meetingId, participantUserId, hostId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Participant admitted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{meetingId}/participants")
    public ResponseEntity<List<MeetingParticipant>> getActiveParticipants(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingService.getActiveParticipants(meetingId));
    }

    @GetMapping("/{meetingId}/waiting-room")
    public ResponseEntity<List<MeetingParticipant>> getWaitingRoomParticipants(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingService.getWaitingRoomParticipants(meetingId));
    }

    @GetMapping("/{meetingId}")
    public ResponseEntity<Meeting> getMeeting(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingService.getMeetingById(meetingId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Meeting>> getUserMeetings() {
        UUID userId = getUserPrincipal().getUserId();
        return ResponseEntity.ok(meetingService.getUserMeetings(userId));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstantMeetingRequest {
        private String title;
        private Boolean waitingRoomEnabled;
        private Boolean recordingEnabled;
        private UUID teamId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
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