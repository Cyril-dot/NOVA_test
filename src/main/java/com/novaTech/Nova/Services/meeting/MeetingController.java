package com.novaTech.Nova.Services.meeting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingController {

    private final JitsiMeetingService meetingService;

    // ── helpers ───────────────────────────────────────────────────────────────
    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length - 1; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    // ─────────────────────────────────────────────
    // PUBLIC
    // ─────────────────────────────────────────────

    @GetMapping("/{code}")
    public ResponseEntity<?> getRoomInfo(@PathVariable String code) {
        try {
            Map<String, Object> room = meetingService.getRoom(code);
            if (room == null) return ResponseEntity.notFound().build();

            Map<String, Object> safe = new HashMap<>();
            safe.put("name",       room.get("name"));
            safe.put("url",        room.get("url"));
            safe.put("privacy",    room.get("privacy"));
            safe.put("created_at", room.get("created_at"));
            return ResponseEntity.ok(safe);
        } catch (Exception e) {
            log.error("❌ getRoomInfo error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(mapOf("error", e.getMessage()));
        }
    }

    @PostMapping("/join/guest")
    public ResponseEntity<?> joinAsGuest(@RequestBody Map<String, String> body) {
        try {
            String roomCode    = body.get("roomCode");
            String displayName = body.getOrDefault("displayName", "Guest");

            if (roomCode == null || roomCode.isBlank())
                return ResponseEntity.badRequest().body(mapOf("error", "roomCode is required"));

            // For public Jitsi — auto-create room if it doesn't exist yet
            Map<String, Object> room = meetingService.getRoom(roomCode);
            if (room == null) {
                log.info("🔧 [Jitsi] Guest join: room '{}' not found — auto-creating", roomCode);
                room = meetingService.createRoom(roomCode, false);
            }

            Map<String, Object> tokenData = meetingService.createGuestToken(roomCode, displayName);
            tokenData.put("roomCode", roomCode);
            return ResponseEntity.ok(tokenData);
        } catch (Exception e) {
            log.error("❌ joinAsGuest error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(mapOf("error", e.getMessage()));
        }
    }

    @GetMapping("/validate/{token}")
    public ResponseEntity<?> validateToken(@PathVariable String token) {
        return ResponseEntity.ok(mapOf("valid", true, "token", token));
    }

    // ─────────────────────────────────────────────
    // AUTHENTICATED
    // ─────────────────────────────────────────────

    @PostMapping("/create")
    public ResponseEntity<?> createRoom(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            String  roomName  = (String) body.getOrDefault("roomName", null);
            boolean isPrivate = Boolean.TRUE.equals(body.get("private"));

            Map<String, Object> room = meetingService.createRoom(roomName, isPrivate);
            log.info("✅ Room created by '{}': {}", principal.getUsername(), room.get("name"));
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            log.error("❌ createRoom error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(mapOf("error", e.getMessage()));
        }
    }

    @PostMapping("/{code}/token")
    public ResponseEntity<?> getMeetingToken(
            @PathVariable String code,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            boolean isModerator = body != null && Boolean.TRUE.equals(body.get("isOwner"));

            // Auto-create room if it doesn't exist (handles rejoins after server restart)
            Map<String, Object> room = meetingService.getRoom(code);
            if (room == null) {
                log.info("🔧 Room '{}' not in store — auto-creating for token request", code);
                room = meetingService.createRoom(code, false);
            }

            Map<String, Object> tokenData = meetingService.createMeetingToken(
                    code, principal.getUsername(), principal.getUsername(), isModerator);
            tokenData.put("isOwner", isModerator);
            return ResponseEntity.ok(tokenData);
        } catch (Exception e) {
            log.error("❌ getMeetingToken error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(mapOf("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<?> deleteRoom(
            @PathVariable String code,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            meetingService.deleteRoom(code);
            log.info("🗑️ Room '{}' deleted by '{}'", code, principal.getUsername());
            return ResponseEntity.ok(mapOf("deleted", true, "name", code));
        } catch (Exception e) {
            log.error("❌ deleteRoom error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(mapOf("error", e.getMessage()));
        }
    }

    @GetMapping("/{code}/presence")
    public ResponseEntity<?> getPresence(@PathVariable String code) {
        try {
            return ResponseEntity.ok(meetingService.getRoomPresence(code));
        } catch (Exception e) {
            log.error("❌ getPresence error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(mapOf("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listMeetings(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String room) {
        try {
            Map<String, Object> result = meetingService.listMeetings(room, limit);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ listMeetings error: {}", e.getMessage(), e);
            // Return empty list — never 500 on this endpoint
            Map<String, Object> empty = new HashMap<>();
            empty.put("total_count", 0);
            empty.put("data", new ArrayList<>());
            return ResponseEntity.ok(empty);
        }
    }

    @PostMapping("/{code}/eject")
    public ResponseEntity<?> ejectParticipants(
            @PathVariable String code,
            @RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<String> ids = (List<String>) body.get("participantIds");
            if (ids == null || ids.isEmpty())
                return ResponseEntity.badRequest().body(mapOf("error", "participantIds required"));
            meetingService.ejectParticipants(code, ids);
            return ResponseEntity.ok(mapOf("ejected", ids.size()));
        } catch (Exception e) {
            log.error("❌ ejectParticipants error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(mapOf("error", e.getMessage()));
        }
    }

    @PostMapping("/{code}/message")
    public ResponseEntity<?> sendMessage(
            @PathVariable String code,
            @RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            String recipient = (String) body.getOrDefault("recipient", "*");
            if (data == null)
                return ResponseEntity.badRequest().body(mapOf("error", "data is required"));
            meetingService.sendAppMessage(code, data, recipient);
            return ResponseEntity.ok(mapOf("sent", true));
        } catch (Exception e) {
            log.error("❌ sendMessage error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(mapOf("error", e.getMessage()));
        }
    }

    @PostMapping("/{code}/recording/start")
    public ResponseEntity<?> startRecording(@PathVariable String code) {
        try {
            meetingService.startRecording(code);
            return ResponseEntity.ok(mapOf("recording", "started", "room", code));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(mapOf("error", e.getMessage()));
        }
    }

    @PostMapping("/{code}/recording/stop")
    public ResponseEntity<?> stopRecording(@PathVariable String code) {
        try {
            meetingService.stopRecording(code);
            return ResponseEntity.ok(mapOf("recording", "stopped", "room", code));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(mapOf("error", e.getMessage()));
        }
    }
}