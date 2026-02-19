package com.novaTech.Nova.Services.ZoomServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Daily.co-backed meetings.
 *
 * Public endpoints (matching SecurityConfig.permitAll rules):
 *   GET  /api/meetings/{code}              – fetch room info
 *   POST /api/meetings/join/guest          – join as guest (no auth)
 *   GET  /api/meetings/daily-token/guest   – get a guest token
 *   GET  /api/meetings/validate/{token}    – validate a token
 *
 * Authenticated endpoints:
 *   POST /api/meetings/create              – create a new room
 *   POST /api/meetings/{code}/token        – get a host/member token
 *   DELETE /api/meetings/{code}            – delete a room
 *   GET  /api/meetings/{code}/presence     – who is live right now
 *   GET  /api/meetings                     – list recent meetings
 *   POST /api/meetings/{code}/eject        – remove participants
 *   POST /api/meetings/{code}/message      – broadcast in-call message
 *   POST /api/meetings/{code}/recording/start
 *   POST /api/meetings/{code}/recording/stop
 */
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingController {

    private final DailyMeetingService meetingService;

    // ─────────────────────────────────────────────
    // PUBLIC ENDPOINTS
    // ─────────────────────────────────────────────

    /**
     * GET /api/meetings/{code}
     * Returns basic room info (privacy, expiry, url).
     * Permitted without authentication per SecurityConfig.
     */
    @GetMapping("/{code}")
    public ResponseEntity<?> getRoomInfo(@PathVariable String code) {
        Map<String, Object> room = meetingService.getRoom(code);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        // Expose only safe fields to anonymous callers
        Map<String, Object> safe = new HashMap<>();
        safe.put("name", room.get("name"));
        safe.put("url", room.get("url"));
        safe.put("privacy", room.get("privacy"));
        safe.put("created_at", room.get("created_at"));
        return ResponseEntity.ok(safe);
    }

    /**
     * POST /api/meetings/join/guest
     * Body: { "roomCode": "...", "displayName": "..." }
     * Returns a short-lived guest token + the room URL.
     */
    @PostMapping("/join/guest")
    public ResponseEntity<?> joinAsGuest(@RequestBody Map<String, String> body) {
        String roomCode = body.get("roomCode");
        String displayName = body.getOrDefault("displayName", "Guest");

        if (roomCode == null || roomCode.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "roomCode is required"));
        }

        Map<String, Object> room = meetingService.getRoom(roomCode);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }

        String token = meetingService.createGuestToken(roomCode, displayName);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "roomUrl", room.get("url"),
                "roomName", roomCode
        ));
    }

    /**
     * GET /api/meetings/daily-token/guest?roomCode=xxx&displayName=yyy
     * Alternative GET version of the guest token endpoint (for simple clients).
     */
    @GetMapping("/daily-token/guest")
    public ResponseEntity<?> getGuestToken(
            @RequestParam String roomCode,
            @RequestParam(defaultValue = "Guest") String displayName) {

        Map<String, Object> room = meetingService.getRoom(roomCode);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }

        String token = meetingService.createGuestToken(roomCode, displayName);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "roomUrl", room.get("url")
        ));
    }

    /**
     * GET /api/meetings/validate/{token}
     * Validates a Daily meeting token.
     */
    @GetMapping("/validate/{token}")
    public ResponseEntity<?> validateToken(@PathVariable String token) {
        Map<String, Object> result = meetingService.validateToken(token);
        if (result == null) {
            return ResponseEntity.status(401).body(Map.of("valid", false, "error", "Token invalid or expired"));
        }
        return ResponseEntity.ok(Map.of("valid", true, "properties", result));
    }

    // ─────────────────────────────────────────────
    // AUTHENTICATED ENDPOINTS
    // ─────────────────────────────────────────────

    /**
     * POST /api/meetings/create
     * Body: { "roomName": "...", "private": true }
     * Returns the full room object including the Daily room URL.
     */
    @PostMapping("/create")
    public ResponseEntity<?> createRoom(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {

        String roomName = (String) body.getOrDefault("roomName", null);
        boolean isPrivate = Boolean.TRUE.equals(body.get("private"));

        Map<String, Object> room = meetingService.createRoom(roomName, isPrivate);
        log.info("✅ Room created by '{}': {}", principal.getUsername(), room.get("name"));
        return ResponseEntity.ok(room);
    }

    /**
     * POST /api/meetings/{code}/token
     * Body: { "isOwner": true/false }
     * Returns a Daily meeting token for the authenticated user.
     */
    @PostMapping("/{code}/token")
    public ResponseEntity<?> getMeetingToken(
            @PathVariable String code,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {

        boolean isOwner = body != null && Boolean.TRUE.equals(body.get("isOwner"));

        // Verify room exists first
        Map<String, Object> room = meetingService.getRoom(code);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }

        String token = meetingService.createMeetingToken(
                code,
                principal.getUsername(),   // user_id
                principal.getUsername(),   // user_name (override in your User entity if needed)
                isOwner
        );

        return ResponseEntity.ok(Map.of(
                "token", token,
                "roomUrl", room.get("url"),
                "roomName", code,
                "isOwner", isOwner
        ));
    }

    /**
     * DELETE /api/meetings/{code}
     * Deletes a Daily room permanently.
     */
    @DeleteMapping("/{code}")
    public ResponseEntity<?> deleteRoom(
            @PathVariable String code,
            @AuthenticationPrincipal UserDetails principal) {

        boolean deleted = meetingService.deleteRoom(code);
        if (!deleted) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete room"));
        }
        log.info("🗑️ Room '{}' deleted by '{}'", code, principal.getUsername());
        return ResponseEntity.ok(Map.of("deleted", true, "name", code));
    }

    /**
     * GET /api/meetings/{code}/presence
     * Returns who is currently in the room.
     */
    @GetMapping("/{code}/presence")
    public ResponseEntity<?> getPresence(@PathVariable String code) {
        return ResponseEntity.ok(meetingService.getRoomPresence(code));
    }

    /**
     * GET /api/meetings?limit=20&room=xxx
     * Lists meeting history (completed + ongoing sessions).
     */
    @GetMapping
    public ResponseEntity<?> listMeetings(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String room) {

        return ResponseEntity.ok(meetingService.listMeetings(room, limit));
    }

    /**
     * POST /api/meetings/{code}/eject
     * Body: { "participantIds": ["id1", "id2"] }
     */
    @PostMapping("/{code}/eject")
    public ResponseEntity<?> ejectParticipants(
            @PathVariable String code,
            @RequestBody Map<String, Object> body) {

        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("participantIds");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "participantIds required"));
        }

        meetingService.ejectParticipants(code, ids);
        return ResponseEntity.ok(Map.of("ejected", ids.size()));
    }

    /**
     * POST /api/meetings/{code}/message
     * Body: { "data": {...}, "recipient": "*" }
     * Sends an app-message to participants via Daily.
     */
    @PostMapping("/{code}/message")
    public ResponseEntity<?> sendMessage(
            @PathVariable String code,
            @RequestBody Map<String, Object> body) {

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        String recipient = (String) body.getOrDefault("recipient", "*");

        if (data == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "data is required"));
        }

        meetingService.sendAppMessage(code, data, recipient);
        return ResponseEntity.ok(Map.of("sent", true));
    }

    /**
     * POST /api/meetings/{code}/recording/start
     */
    @PostMapping("/{code}/recording/start")
    public ResponseEntity<?> startRecording(@PathVariable String code) {
        meetingService.startRecording(code);
        return ResponseEntity.ok(Map.of("recording", "started", "room", code));
    }

    /**
     * POST /api/meetings/{code}/recording/stop
     */
    @PostMapping("/{code}/recording/stop")
    public ResponseEntity<?> stopRecording(@PathVariable String code) {
        meetingService.stopRecording(code);
        return ResponseEntity.ok(Map.of("recording", "stopped", "room", code));
    }
}