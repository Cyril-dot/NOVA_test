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
 * ─────────────────────────────────────────────────────────────────────────────
 * Meeting REST Controller — Daily.co Edition
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * New endpoints added for Daily.co:
 *
 *   POST /api/meetings/daily-token
 *     → Authenticated users call this after joinMeeting().
 *       Returns { token, roomUrl, roomName, meetingCode, isOwner }.
 *       The frontend passes `token` to callFrame.join({ token }) so Daily
 *       knows the participant's display name and owner permissions.
 *
 *   POST /api/meetings/daily-token/guest
 *     → Guests call this after joinMeeting/guest.
 *       No JWT needed — permitted in Spring Security config.
 *       Returns same shape as above.
 *
 * All existing endpoints are unchanged so nothing breaks.
 *
 * Security notes:
 *   - /api/meetings/join/guest        → permitAll()  (existing)
 *   - /api/meetings/validate/**       → permitAll()  (existing)
 *   - /api/meetings/daily-token/guest → permitAll()  (NEW — add to SecurityConfig)
 *   - Everything else                 → authenticated
 * ─────────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingController {

    private final MeetingService meetingService;

    // =========================================================================
    //  CREATE MEETING
    //  Now also creates the Daily.co room server-side so the API key never
    //  has to be exposed in the frontend bundle.
    // =========================================================================
    @PostMapping("/create")
    public ResponseEntity<?> createMeeting(@RequestBody CreateMeetingDTO dto) {
        try {
            String userEmail = userPrincipal().getEmail();
            MeetingResponseDTO response = meetingService.createMeeting(userEmail, dto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Meeting created successfully",
                    "data",    response
            ));
        } catch (Exception e) {
            log.error("Error creating meeting", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // =========================================================================
    //  START MEETING
    //  Flips status to ACTIVE and ensures the Daily room still exists.
    // =========================================================================
    @PostMapping("/start/{meetingCode}")
    public ResponseEntity<?> startMeeting(@PathVariable String meetingCode) {
        try {
            String userEmail = userPrincipal().getEmail();
            MeetingResponseDTO response = meetingService.startMeeting(userEmail, meetingCode);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Meeting started successfully",
                    "data",    response
            ));
        } catch (Exception e) {
            log.error("Error starting meeting", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // =========================================================================
    //  JOIN MEETING — Authenticated user
    // =========================================================================
    @PostMapping("/join")
    public ResponseEntity<?> joinMeeting(@RequestBody JoinMeetingDTO dto) {
        try {
            String userEmail = userPrincipal().getEmail();
            MeetingResponseDTO response = meetingService.joinMeetingAsUser(userEmail, dto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Joined meeting successfully",
                    "data",    response
            ));
        } catch (Exception e) {
            log.error("Error joining meeting", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // =========================================================================
    //  JOIN MEETING — Guest (no JWT required)
    //  Ensure your SecurityConfig has:
    //    .requestMatchers("/api/meetings/join/guest").permitAll()
    // =========================================================================
    @PostMapping("/join/guest")
    public ResponseEntity<?> joinMeetingAsGuest(@RequestBody JoinMeetingDTO dto) {
        try {
            MeetingResponseDTO response = meetingService.joinMeetingAsGuest(dto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Joined meeting as guest successfully",
                    "data",    response
            ));
        } catch (Exception e) {
            log.error("Error joining meeting as guest", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // =========================================================================
    //  GENERATE DAILY TOKEN — Authenticated user  (NEW)
    //
    //  Call this AFTER /join so the participant record exists in the DB.
    //  The response contains:
    //    token     — pass to callFrame.join({ token }) in the frontend
    //    roomUrl   — the full Daily room URL (https://domain/roomname)
    //    roomName  — just the room name portion
    //    isOwner   — true if the caller is the meeting host
    //
    //  Frontend usage (Meeting.vue joinDailyRoom):
    //    const res   = await fetch('/api/meetings/daily-token', { method: 'POST', body: { meetingCode } ... })
    //    const { token, roomUrl } = res.data
    //    await callFrame.join({ url: roomUrl, token })
    // =========================================================================
    @PostMapping("/daily-token")
    public ResponseEntity<?> getDailyToken(@RequestBody Map<String, String> body) {
        try {
            String userEmail  = userPrincipal().getEmail();
            String meetingCode = body.get("meetingCode");

            if (meetingCode == null || meetingCode.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "meetingCode is required"
                ));
            }

            DailyTokenResponseDTO tokenResponse =
                    meetingService.generateMeetingToken(userEmail, meetingCode);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data",    tokenResponse
            ));
        } catch (Exception e) {
            log.error("Error generating Daily token", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // =========================================================================
    //  GENERATE DAILY TOKEN — Guest  (NEW)
    //  No JWT required.
    //  Add to SecurityConfig:
    //    .requestMatchers("/api/meetings/daily-token/guest").permitAll()
    // =========================================================================
    @PostMapping("/daily-token/guest")
    public ResponseEntity<?> getDailyTokenAsGuest(@RequestBody JoinMeetingDTO dto) {
        try {
            if (dto.getGuestName() == null || dto.getGuestName().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "guestName is required"
                ));
            }
            if (dto.getMeetingCode() == null || dto.getMeetingCode().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "meetingCode is required"
                ));
            }

            DailyTokenResponseDTO tokenResponse =
                    meetingService.generateGuestMeetingToken(dto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data",    tokenResponse
            ));
        } catch (Exception e) {
            log.error("Error generating guest Daily token", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // =========================================================================
    //  END MEETING
    //  Also deletes the Daily.co room, which immediately disconnects
    //  all participants inside the Daily iframe.
    // =========================================================================
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

    // =========================================================================
    //  GET MEETING DETAILS  (public — guests need this too)
    // =========================================================================
    @GetMapping("/{meetingCode}")
    public ResponseEntity<?> getMeetingDetails(@PathVariable String meetingCode) {
        try {
            MeetingResponseDTO response = meetingService.getMeetingDetails(meetingCode);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data",    response
            ));
        } catch (Exception e) {
            log.error("Error getting meeting details", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // =========================================================================
    //  GET USER'S MEETINGS
    // =========================================================================
    @GetMapping("/my-meetings")
    public ResponseEntity<?> getMyMeetings(
            @RequestParam(required = false) MeetingStatus status) {
        try {
            String userEmail = userPrincipal().getEmail();
            List<MeetingResponseDTO> meetings =
                    meetingService.getUserMeetings(userEmail, status);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count",   meetings.size(),
                    "data",    meetings
            ));
        } catch (Exception e) {
            log.error("Error getting user meetings", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // =========================================================================
    //  UPDATE PARTICIPANT MEDIA STATUS
    // =========================================================================
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

    // =========================================================================
    //  KICK PARTICIPANT (Moderator only)
    // =========================================================================
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

    // =========================================================================
    //  VALIDATE MEETING CODE  (public — guests call this before joining)
    // =========================================================================
    @GetMapping("/validate/{meetingCode}")
    public ResponseEntity<?> validateMeetingCode(@PathVariable String meetingCode) {
        try {
            MeetingResponseDTO response = meetingService.getMeetingDetails(meetingCode);

            return ResponseEntity.ok(Map.of(
                    "success",          true,
                    "valid",            true,
                    "requiresPassword", response.getRequiresPassword(),
                    "allowGuests",      response.getAllowGuests(),
                    "status",           response.getStatus(),
                    "isFull",           response.getCurrentParticipants() >= response.getMaxParticipants()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "valid",   false,
                    "message", e.getMessage()
            ));
        }
    }

    // =========================================================================
    //  HELPER — extract UserPrincipal from SecurityContext
    //  Do NOT call on guest or public endpoints.
    // =========================================================================
    private UserPrincipal userPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("No authentication found in SecurityContext");
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            log.error("Invalid principal type: {}",
                    principal != null ? principal.getClass().getName() : "null");
            throw new RuntimeException("Invalid authentication principal");
        }

        UserPrincipal up = (UserPrincipal) principal;
        log.debug("Authenticated user: {} (ID: {})", up.getEmail(), up.getUserId());
        return up;
    }
}