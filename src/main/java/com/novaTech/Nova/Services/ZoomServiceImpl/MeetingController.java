package com.novaTech.Nova.Services.ZoomServiceImpl;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.MeetingStatus;
import com.novaTech.Nova.Security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Meeting REST Controller
 *
 * /api/meetings/join/guest  — no auth required  (guests)
 * /api/meetings/join        — JWT required       (registered users)
 * All other write endpoints — JWT required
 */
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingController {

    private final MeetingService meetingService;

    // ========================
    // CREATE MEETING
    // ========================
    @PostMapping("/create")
    public ResponseEntity<?> createMeeting(@RequestBody CreateMeetingDTO dto) {
        try {
            String userEmail = userPrincipal().getEmail();
            MeetingResponseDTO response = meetingService.createMeeting(userEmail, dto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Meeting created successfully",
                    "data", response
            ));
        } catch (Exception e) {
            log.error("Error creating meeting", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ========================
    // START MEETING
    // ========================
    @PostMapping("/start/{meetingCode}")
    public ResponseEntity<?> startMeeting(@PathVariable String meetingCode) {
        try {
            String userEmail = userPrincipal().getEmail();
            MeetingResponseDTO response = meetingService.startMeeting(userEmail, meetingCode);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Meeting started successfully",
                    "data", response
            ));
        } catch (Exception e) {
            log.error("Error starting meeting", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ========================
    // JOIN MEETING — Authenticated users only
    // Requires: Authorization: Bearer <jwt>
    // ========================
    @PostMapping("/join")
    public ResponseEntity<?> joinMeeting(@RequestBody JoinMeetingDTO dto) {
        try {
            String userEmail = userPrincipal().getEmail();
            MeetingResponseDTO response = meetingService.joinMeetingAsUser(userEmail, dto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Joined meeting successfully",
                    "data", response
            ));
        } catch (Exception e) {
            log.error("Error joining meeting", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ========================
    // JOIN MEETING — Guest (no auth required)
    // Make sure Spring Security permits this path without a JWT:
    //   .requestMatchers("/api/meetings/join/guest").permitAll()
    // ========================
    @PostMapping("/join/guest")
    public ResponseEntity<?> joinMeetingAsGuest(@RequestBody JoinMeetingDTO dto) {
        try {
            MeetingResponseDTO response = meetingService.joinMeetingAsGuest(dto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Joined meeting as guest successfully",
                    "data", response
            ));
        } catch (Exception e) {
            log.error("Error joining meeting as guest", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ========================
    // END MEETING
    // ========================
    @PostMapping("/end/{meetingCode}")
    public ResponseEntity<?> endMeeting(@PathVariable String meetingCode) {
        try {
            String userEmail = userPrincipal().getEmail();
            meetingService.endMeeting(userEmail, meetingCode);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Meeting ended successfully"
            ));
        } catch (Exception e) {
            log.error("Error ending meeting", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ========================
    // GET MEETING DETAILS (public — guests need this too)
    // ========================
    @GetMapping("/{meetingCode}")
    public ResponseEntity<?> getMeetingDetails(@PathVariable String meetingCode) {
        try {
            MeetingResponseDTO response = meetingService.getMeetingDetails(meetingCode);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", response
            ));
        } catch (Exception e) {
            log.error("Error getting meeting details", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ========================
    // GET USER'S MEETINGS
    // ========================
    @GetMapping("/my-meetings")
    public ResponseEntity<?> getMyMeetings(
            @RequestParam(required = false) MeetingStatus status) {
        try {
            String userEmail = userPrincipal().getEmail();
            List<MeetingResponseDTO> meetings = meetingService.getUserMeetings(userEmail, status);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", meetings.size(),
                    "data", meetings
            ));
        } catch (Exception e) {
            log.error("Error getting user meetings", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ========================
    // UPDATE PARTICIPANT MEDIA STATUS
    // ========================
    @PutMapping("/participant/status")
    public ResponseEntity<?> updateParticipantStatus(
            @RequestParam String sessionId,
            @RequestParam(required = false) Boolean videoEnabled,
            @RequestParam(required = false) Boolean audioEnabled,
            @RequestParam(required = false) Boolean screenSharing) {
        try {
            meetingService.updateParticipantStatus(sessionId, videoEnabled, audioEnabled, screenSharing);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Participant status updated"
            ));
        } catch (Exception e) {
            log.error("Error updating participant status", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ========================
    // KICK PARTICIPANT (Moderator only)
    // ========================
    @DeleteMapping("/participant/{participantId}")
    public ResponseEntity<?> kickParticipant(@PathVariable UUID participantId) {
        try {
            String userEmail = userPrincipal().getEmail();
            meetingService.kickParticipant(userEmail, participantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Participant removed from meeting"
            ));
        } catch (Exception e) {
            log.error("Error kicking participant", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ========================
    // VALIDATE MEETING CODE (public — guests need this before joining)
    // ========================
    @GetMapping("/validate/{meetingCode}")
    public ResponseEntity<?> validateMeetingCode(@PathVariable String meetingCode) {
        try {
            MeetingResponseDTO response = meetingService.getMeetingDetails(meetingCode);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "valid", true,
                    "requiresPassword",  response.getRequiresPassword(),
                    "allowGuests",       response.getAllowGuests(),
                    "status",            response.getStatus(),
                    "isFull",            response.getCurrentParticipants() >= response.getMaxParticipants()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "valid", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ========================
    // HELPER — extract UserPrincipal from SecurityContext
    // Only call this on endpoints that require authentication.
    // Do NOT call on /join/guest, /validate/*, or GET /{meetingCode}.
    // ========================
    private UserPrincipal userPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("No authentication found in SecurityContext");
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            log.error("Invalid principal type: {}", principal != null ? principal.getClass().getName() : "null");
            throw new RuntimeException("Invalid authentication principal");
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;
        log.debug("Authenticated user: {} (ID: {})", userPrincipal.getEmail(), userPrincipal.getUserId());
        return userPrincipal;
    }
}