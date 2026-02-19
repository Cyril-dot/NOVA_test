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

/**
 * REST controller for Daily.co-backed meetings.
 *
 * All responses are returned DIRECTLY via ResponseEntity.ok(object).
 * There is NO { success, data, message } wrapper — clients must read
 * fields from the JSON root level.
 *
 * Public endpoints (matching SecurityConfig.permitAll rules):
 *   GET  /api/meetings/{code}              – fetch room info
 *   POST /api/meetings/join/guest          – join as guest (no auth)
 *   GET  /api/meetings/daily-token/guest   – get a guest token (GET variant)
 *   GET  /api/meetings/validate/{token}    – validate a Daily token
 *
 * Authenticated endpoints:
 *   POST   /api/meetings/create                    – create a new room
 *   POST   /api/meetings/{code}/token              – get a host/member token
 *   DELETE /api/meetings/{code}                    – delete a room
 *   GET    /api/meetings/{code}/presence           – who is live right now
 *   GET    /api/meetings                           – list recent meetings
 *   POST   /api/meetings/{code}/eject              – remove participants
 *   POST   /api/meetings/{code}/message            – broadcast in-call message
 *   POST   /api/meetings/{code}/recording/start    – start cloud recording
 *   POST   /api/meetings/{code}/recording/stop     – stop cloud recording
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
     *
     * Returns basic room info (name, url, privacy, created_at).
     * Permitted without authentication per SecurityConfig.
     *
     * Response shape:
     *   { "name": "...", "url": "...", "privacy": "public|private", "created_at": "..." }
     */
    @GetMapping("/{code}")
    public ResponseEntity<?> getRoomInfo(@PathVariable String code) {
        Map<String, Object> room = meetingService.getRoom(code);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        // Expose only safe fields to anonymous callers
        Map<String, Object> safe = new HashMap<>();
        safe.put("name",       room.get("name"));
        safe.put("url",        room.get("url"));
        safe.put("privacy",    room.get("privacy"));
        safe.put("created_at", room.get("created_at"));
        return ResponseEntity.ok(safe);
    }

    /**
     * POST /api/meetings/join/guest
     *
     * Body:   { "roomCode": "...", "displayName": "..." }
     * Response: { "token": "...", "roomUrl": "...", "roomName": "..." }
     */
    @PostMapping("/join/guest")
    public ResponseEntity<?> joinAsGuest(@RequestBody Map<String, String> body) {
        String roomCode    = body.get("roomCode");
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
                "token",    token,
                "roomUrl",  room.get("url"),
                "roomName", roomCode
        ));
    }

    /**
     * GET /api/meetings/daily-token/guest?roomCode=xxx&displayName=yyy
     *
     * Alternative GET variant of the guest token endpoint.
     * Response: { "token": "...", "roomUrl": "..." }
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
                "token",   token,
                "roomUrl", room.get("url")
        ));
    }

    /**
     * GET /api/meetings/validate/{token}
     *
     * Validates a Daily meeting token.
     * Response (valid):   { "valid": true,  "properties": { ... } }
     * Response (invalid): { "valid": false, "error": "Token invalid or expired" }
     */
    @GetMapping("/validate/{token}")
    public ResponseEntity<?> validateToken(@PathVariable String token) {
        Map<String, Object> result = meetingService.validateToken(token);
        if (result == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "valid", false,
                    "error", "Token invalid or expired"
            ));
        }
        return ResponseEntity.ok(Map.of("valid", true, "properties", result));
    }

    // ─────────────────────────────────────────────
    // AUTHENTICATED ENDPOINTS
    // ─────────────────────────────────────────────

    /**
     * POST /api/meetings/create
     *
     * Body:     { "roomName": "...", "private": true|false }
     * Response: Full Daily room object — { name, url, privacy, created_at, config, ... }
     *
     * The room object is returned DIRECTLY from the Daily API — clients should
     * read `data.name` and `data.url` at the root level (NOT data.data.url).
     */
    @PostMapping("/create")
    public ResponseEntity<?> createRoom(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {

        String  roomName  = (String) body.getOrDefault("roomName", null);
        boolean isPrivate = Boolean.TRUE.equals(body.get("private"));

        Map<String, Object> room = meetingService.createRoom(roomName, isPrivate);
        log.info("✅ Room created by '{}': {}", principal.getUsername(), room.get("name"));
        // Returns: { name, url, privacy, created_at, config, ... }
        return ResponseEntity.ok(room);
    }

    /**
     * POST /api/meetings/{code}/token
     *
     * Body:     { "isOwner": true|false }
     * Response: { "token": "...", "roomUrl": "...", "roomName": "...", "isOwner": true|false }
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
                principal.getUsername(),  // user_id
                principal.getUsername(),  // user_name — override via your User entity if needed
                isOwner
        );

        // Returns: { token, roomUrl, roomName, isOwner }
        return ResponseEntity.ok(Map.of(
                "token",    token,
                "roomUrl",  room.get("url"),
                "roomName", code,
                "isOwner",  isOwner
        ));
    }

    /**
     * DELETE /api/meetings/{code}
     *
     * Permanently deletes the Daily room.
     * Response: { "deleted": true, "name": "..." }
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
     *
     * Returns current participant snapshot from Daily.
     * Response: Daily presence object (passthrough).
     */
    @GetMapping("/{code}/presence")
    public ResponseEntity<?> getPresence(@PathVariable String code) {
        return ResponseEntity.ok(meetingService.getRoomPresence(code));
    }

    /**
     * GET /api/meetings?limit=20&room=xxx
     *
     * Lists meeting sessions (completed + ongoing).
     * Response: Daily meetings object (passthrough) — { total_count, data: [...] }
     */
    @GetMapping
    public ResponseEntity<?> listMeetings(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String room) {

        return ResponseEntity.ok(meetingService.listMeetings(room, limit));
    }

    /**
     * POST /api/meetings/{code}/eject
     *
     * Body:     { "participantIds": ["id1", "id2"] }
     * Response: { "ejected": N }
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
     *
     * Sends an app-message to participants via Daily.
     * Body:     { "data": { ... }, "recipient": "*" }
     * Response: { "sent": true }
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
     *
     * Response: { "recording": "started", "room": "..." }
     */
    @PostMapping("/{code}/recording/start")
    public ResponseEntity<?> startRecording(@PathVariable String code) {
        meetingService.startRecording(code);
        return ResponseEntity.ok(Map.of("recording", "started", "room", code));
    }

    /**
     * POST /api/meetings/{code}/recording/stop
     *
     * Response: { "recording": "stopped", "room": "..." }
     */
    @PostMapping("/{code}/recording/stop")
    public ResponseEntity<?> stopRecording(@PathVariable String code) {
        meetingService.stopRecording(code);
        return ResponseEntity.ok(Map.of("recording", "stopped", "room", code));
    }
}