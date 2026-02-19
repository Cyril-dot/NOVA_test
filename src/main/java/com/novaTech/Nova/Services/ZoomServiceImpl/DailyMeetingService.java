package com.novaTech.Nova.Services.ZoomServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service that wraps the Daily.co REST API.
 *
 * Replaces the custom WebRTC signaling handler — Daily.co's cloud
 * infrastructure now handles all SFU / peer signaling. This service only
 * manages room lifecycle and participant tokens.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DailyMeetingService {

    private final DailyMeetingConfig config;

    @Qualifier("dailyRestTemplate")
    private final RestTemplate dailyRestTemplate;

    // ─────────────────────────────────────────────
    // ROOM MANAGEMENT
    // ─────────────────────────────────────────────

    /**
     * Create a new Daily room.
     *
     * @param roomName  Desired room name (alphanumeric, dash, underscore; max 128 chars).
     *                  Pass null for an auto-generated name.
     * @param isPrivate Whether the room requires a meeting token to join.
     * @return The full Daily room object as a Map.
     */
    public Map<String, Object> createRoom(String roomName, boolean isPrivate) {
        Map<String, Object> body = new HashMap<>();

        if (roomName != null && !roomName.isBlank()) {
            body.put("name", roomName);
        }
        body.put("privacy", isPrivate ? "private" : "public");

        Map<String, Object> properties = new HashMap<>();
        long expiresAt = Instant.now().getEpochSecond() + config.getRoomExpirySeconds();
        properties.put("exp", expiresAt);
        properties.put("eject_at_room_exp", true);   // clean up when room expires
        properties.put("enable_chat", true);
        properties.put("start_video_off", false);
        properties.put("start_audio_off", false);
        body.put("properties", properties);

        try {
            ResponseEntity<Map> response = dailyRestTemplate.postForEntity(
                    config.getApiBaseUrl() + "/rooms", body, Map.class);

            log.info("✅ [Daily] Room created: {}", response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("❌ [Daily] Failed to create room: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to create Daily room: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve an existing room by name.
     */
    public Map<String, Object> getRoom(String roomName) {
        try {
            ResponseEntity<Map> response = dailyRestTemplate.getForEntity(
                    config.getApiBaseUrl() + "/rooms/" + roomName, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("⚠️ [Daily] Room not found: {}", roomName);
            return null;
        } catch (HttpClientErrorException e) {
            log.error("❌ [Daily] Error fetching room {}: {}", roomName, e.getMessage());
            throw new RuntimeException("Failed to get Daily room: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a room by name.
     */
    public boolean deleteRoom(String roomName) {
        try {
            dailyRestTemplate.delete(config.getApiBaseUrl() + "/rooms/" + roomName);
            log.info("🗑️ [Daily] Room deleted: {}", roomName);
            return true;
        } catch (HttpClientErrorException e) {
            log.error("❌ [Daily] Failed to delete room {}: {}", roomName, e.getMessage());
            return false;
        }
    }

    /**
     * List rooms (up to 100). Use starting_after for pagination.
     */
    public Map<String, Object> listRooms(int limit, String startingAfter) {
        String url = config.getApiBaseUrl() + "/rooms?limit=" + limit;
        if (startingAfter != null && !startingAfter.isBlank()) {
            url += "&starting_after=" + startingAfter;
        }
        ResponseEntity<Map> response = dailyRestTemplate.getForEntity(url, Map.class);
        return response.getBody();
    }

    // ─────────────────────────────────────────────
    // MEETING TOKENS
    // ─────────────────────────────────────────────

    /**
     * Create a meeting token for a specific participant.
     *
     * @param roomName   The room this token is valid for.
     * @param userId     Your internal user identifier (max 36 chars).
     * @param userName   Display name shown in the call UI.
     * @param isOwner    true  → participant has host/admin privileges.
     * @param expiryOverrideSecs  Optional expiry in seconds from now;
     *                            pass 0 to use the default from config.
     */
    public String createMeetingToken(
            String roomName,
            String userId,
            String userName,
            boolean isOwner,
            long expiryOverrideSecs) {

        long exp = Instant.now().getEpochSecond()
                + (expiryOverrideSecs > 0 ? expiryOverrideSecs : config.getTokenExpirySeconds());

        Map<String, Object> properties = new HashMap<>();
        properties.put("room_name", roomName);
        properties.put("exp", exp);
        properties.put("is_owner", isOwner);
        properties.put("eject_at_token_exp", true);

        if (userId != null && !userId.isBlank()) {
            // user_id max 36 chars — truncate or hash if needed
            properties.put("user_id", userId.length() > 36 ? userId.substring(0, 36) : userId);
        }
        if (userName != null && !userName.isBlank()) {
            properties.put("user_name", userName);
        }

        // Guests start with audio/video muted so they don't surprise anyone
        if (!isOwner) {
            properties.put("start_video_off", false);
            properties.put("start_audio_off", false);
        }

        Map<String, Object> body = Map.of("properties", properties);

        try {
            ResponseEntity<Map> response = dailyRestTemplate.postForEntity(
                    config.getApiBaseUrl() + "/meeting-tokens", body, Map.class);

            String token = (String) response.getBody().get("token");
            log.info("🎟️ [Daily] Token created for user '{}' in room '{}'", userName, roomName);
            return token;
        } catch (HttpClientErrorException e) {
            log.error("❌ [Daily] Failed to create token: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to create Daily meeting token: " + e.getMessage(), e);
        }
    }

    /** Convenience overload using config default expiry. */
    public String createMeetingToken(String roomName, String userId, String userName, boolean isOwner) {
        return createMeetingToken(roomName, userId, userName, isOwner, 0);
    }

    /**
     * Create a short-lived guest token (no userId, no owner privileges).
     */
    public String createGuestToken(String roomName, String guestDisplayName) {
        return createMeetingToken(
                roomName,
                "guest-" + UUID.randomUUID().toString().substring(0, 8),
                guestDisplayName != null ? guestDisplayName : "Guest",
                false,
                1800  // 30-minute guest token
        );
    }

    /**
     * Validate an existing meeting token.
     * Returns the token's properties if valid, null if expired / invalid.
     */
    public Map<String, Object> validateToken(String token) {
        try {
            ResponseEntity<Map> response = dailyRestTemplate.getForEntity(
                    config.getApiBaseUrl() + "/meeting-tokens/" + token, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.warn("⚠️ [Daily] Token validation failed: {}", e.getStatusCode());
            return null;
        }
    }

    // ─────────────────────────────────────────────
    // PARTICIPANT / PRESENCE
    // ─────────────────────────────────────────────

    /**
     * Get current presence snapshot for a room (who is live right now).
     */
    public Map<String, Object> getRoomPresence(String roomName) {
        try {
            ResponseEntity<Map> response = dailyRestTemplate.getForEntity(
                    config.getApiBaseUrl() + "/rooms/" + roomName + "/presence", Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("❌ [Daily] Presence fetch failed for {}: {}", roomName, e.getMessage());
            throw new RuntimeException("Failed to get room presence: " + e.getMessage(), e);
        }
    }

    /**
     * Eject one or more participants from an active meeting by participant id.
     */
    public void ejectParticipants(String roomName, java.util.List<String> participantIds) {
        Map<String, Object> body = Map.of("ids", participantIds);
        try {
            dailyRestTemplate.postForEntity(
                    config.getApiBaseUrl() + "/rooms/" + roomName + "/eject", body, Map.class);
            log.info("🚫 [Daily] Ejected {} participant(s) from '{}'", participantIds.size(), roomName);
        } catch (HttpClientErrorException e) {
            log.error("❌ [Daily] Eject failed: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to eject participants: " + e.getMessage(), e);
        }
    }

    /**
     * Send an in-call app-message to all participants (or a specific one).
     *
     * @param roomName  Target room.
     * @param data      JSON-serialisable payload (≤ 4 KB).
     * @param recipient Session id of target participant, or "*" for broadcast.
     */
    public void sendAppMessage(String roomName, Map<String, Object> data, String recipient) {
        Map<String, Object> body = new HashMap<>();
        body.put("data", data);
        body.put("recipient", recipient != null ? recipient : "*");

        try {
            dailyRestTemplate.postForEntity(
                    config.getApiBaseUrl() + "/rooms/" + roomName + "/send-app-message",
                    body, Map.class);
            log.debug("📨 [Daily] App-message sent to '{}' in '{}'", recipient, roomName);
        } catch (HttpClientErrorException e) {
            log.error("❌ [Daily] send-app-message failed: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to send app message: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    // MEETINGS (HISTORY)
    // ─────────────────────────────────────────────

    /**
     * Fetch completed/ongoing meeting sessions for a room.
     *
     * @param roomName Optional filter by room name.
     * @param limit    Max results (≤ 100).
     */
    public Map<String, Object> listMeetings(String roomName, int limit) {
        String url = config.getApiBaseUrl() + "/meetings?limit=" + Math.min(limit, 100);
        if (roomName != null && !roomName.isBlank()) {
            url += "&room=" + roomName;
        }
        try {
            ResponseEntity<Map> response = dailyRestTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("❌ [Daily] listMeetings failed: {}", e.getMessage());
            throw new RuntimeException("Failed to list meetings: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    // RECORDINGS
    // ─────────────────────────────────────────────

    /** Start a cloud recording with the default grid layout. */
    public void startRecording(String roomName) {
        Map<String, Object> body = Map.of(
                "layout", Map.of("preset", "default")
        );
        try {
            dailyRestTemplate.postForEntity(
                    config.getApiBaseUrl() + "/rooms/" + roomName + "/recordings/start",
                    body, Map.class);
            log.info("🔴 [Daily] Recording started in '{}'", roomName);
        } catch (HttpClientErrorException e) {
            log.error("❌ [Daily] Start recording failed: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to start recording: " + e.getMessage(), e);
        }
    }

    /** Stop an active cloud recording. */
    public void stopRecording(String roomName) {
        try {
            dailyRestTemplate.postForEntity(
                    config.getApiBaseUrl() + "/rooms/" + roomName + "/recordings/stop",
                    Map.of("type", "cloud"), Map.class);
            log.info("⏹️ [Daily] Recording stopped in '{}'", roomName);
        } catch (HttpClientErrorException e) {
            log.error("❌ [Daily] Stop recording failed: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to stop recording: " + e.getMessage(), e);
        }
    }
}