package com.novaTech.Nova.Services.meeting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class JitsiMeetingService {

    private final JitsiMeetingConfig config;

    private final Map<String, Map<String, Object>> roomStore = new ConcurrentHashMap<>();

    // ─── ROOM MANAGEMENT ──────────────────────────────────────────────────────

    public Map<String, Object> createRoom(String roomName, boolean isPrivate) {
        String sanitised = sanitiseRoomName(roomName);
        String roomUrl   = config.buildRoomUrl(sanitised);

        Map<String, Object> room = new HashMap<>();
        room.put("name",       sanitised);
        room.put("url",        roomUrl);
        room.put("privacy",    isPrivate ? "private" : "public");
        room.put("created_at", Instant.now().toString());
        room.put("domain",     config.getDomain());
        room.put("active",     true);

        roomStore.put(sanitised, room);
        log.info("✅ [Jitsi] Room registered: {} | url: {}", sanitised, roomUrl);
        return new HashMap<>(room);
    }

    public Map<String, Object> getRoom(String roomName) {
        if (roomName == null || roomName.isBlank()) return null;
        Map<String, Object> room = roomStore.get(roomName);
        if (room == null) {
            log.warn("⚠️ [Jitsi] Room not found: {}", roomName);
        }
        return room != null ? new HashMap<>(room) : null;
    }

    public boolean deleteRoom(String roomName) {
        Map<String, Object> removed = roomStore.remove(roomName);
        if (removed != null) {
            log.info("🗑️ [Jitsi] Room deleted: {}", roomName);
            return true;
        }
        log.warn("⚠️ [Jitsi] Delete: room not found: {}", roomName);
        // Return true anyway so frontend doesn't get an error for already-deleted rooms
        return true;
    }

    public Map<String, Object> listRooms(int limit, String startingAfter) {
        try {
            // Copy to a new mutable ArrayList — avoids subList serialization issues
            List<Map<String, Object>> rooms = new ArrayList<>();
            for (Map<String, Object> r : roomStore.values()) {
                rooms.add(new HashMap<>(r));
            }

            // Sort by created_at descending
            rooms.sort((a, b) -> {
                String ta = String.valueOf(a.getOrDefault("created_at", ""));
                String tb = String.valueOf(b.getOrDefault("created_at", ""));
                return tb.compareTo(ta);
            });

            // Pagination
            int fromIndex = 0;
            if (startingAfter != null && !startingAfter.isBlank()) {
                for (int i = 0; i < rooms.size(); i++) {
                    if (startingAfter.equals(rooms.get(i).get("name"))) {
                        fromIndex = i + 1;
                        break;
                    }
                }
            }

            int safeLimit  = Math.min(Math.max(limit, 1), 100);
            int toIndex    = Math.min(fromIndex + safeLimit, rooms.size());
            fromIndex      = Math.min(fromIndex, rooms.size());

            // Copy page into a fresh ArrayList — NOT subList
            List<Map<String, Object>> page = new ArrayList<>(rooms.subList(fromIndex, toIndex));

            Map<String, Object> result = new HashMap<>();
            result.put("total_count", rooms.size());
            result.put("data", page);
            return result;

        } catch (Exception e) {
            log.error("❌ [Jitsi] listRooms error: {}", e.getMessage(), e);
            Map<String, Object> empty = new HashMap<>();
            empty.put("total_count", 0);
            empty.put("data", new ArrayList<>());
            return empty;
        }
    }

    // ─── TOKEN GENERATION ─────────────────────────────────────────────────────

    public Map<String, Object> createMeetingToken(
            String roomName, String userId, String userName, boolean isModerator) {
        try {
            Map<String, Object> room = getRoom(roomName);
            String roomUrl = room != null
                    ? (String) room.get("url")
                    : config.buildRoomUrl(roomName);

            Map<String, Object> result = new HashMap<>();
            result.put("roomUrl",     roomUrl);
            result.put("roomName",    roomName);
            result.put("isModerator", isModerator);
            result.put("isOwner",     isModerator);
            result.put("userName",    userName);

            if (config.isJwtEnabled()) {
                String token = generateSimpleToken(roomName, userId, userName, isModerator);
                result.put("token", token);
            } else {
                result.put("token", null);
            }

            log.info("🎟️ [Jitsi] Token created for '{}' in room '{}'", userName, roomName);
            return result;

        } catch (Exception e) {
            log.error("❌ [Jitsi] createMeetingToken error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create meeting token: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> createGuestToken(String roomName, String displayName) {
        String guestId = "guest-" + UUID.randomUUID().toString().substring(0, 8);
        String name    = (displayName != null && !displayName.isBlank()) ? displayName : "Guest";
        return createMeetingToken(roomName, guestId, name, false);
    }

    private String generateSimpleToken(String room, String userId, String userName, boolean isMod) {
        try {
            String payload = String.format(
                    "{\"room\":\"%s\",\"userId\":\"%s\",\"userName\":\"%s\",\"moderator\":%b,\"exp\":%d}",
                    room, userId, userName, isMod, Instant.now().getEpochSecond() + 7200);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payload.getBytes());
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    // ─── PRESENCE ─────────────────────────────────────────────────────────────

    public Map<String, Object> getRoomPresence(String roomName) {
        try {
            Map<String, Object> room = getRoom(roomName);
            Map<String, Object> result = new HashMap<>();
            result.put("room",   roomName);
            result.put("active", room != null);
            result.put("url",    room != null ? room.get("url") : null);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("room",   roomName);
            result.put("active", false);
            result.put("url",    null);
            return result;
        }
    }

    public Map<String, Object> listMeetings(String roomName, int limit) {
        try {
            if (roomName != null && !roomName.isBlank()) {
                Map<String, Object> room = getRoom(roomName);
                List<Map<String, Object>> list = new ArrayList<>();
                if (room != null) list.add(new HashMap<>(room));
                Map<String, Object> r = new HashMap<>();
                r.put("total_count", list.size());
                r.put("data", list);
                return r;
            }
            return listRooms(limit, null);
        } catch (Exception e) {
            log.error("❌ [Jitsi] listMeetings error: {}", e.getMessage(), e);
            Map<String, Object> empty = new HashMap<>();
            empty.put("total_count", 0);
            empty.put("data", new ArrayList<>());
            return empty;
        }
    }

    // ─── UTILITIES ────────────────────────────────────────────────────────────

    private String sanitiseRoomName(String name) {
        if (name == null || name.isBlank()) {
            return "room-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    public void startRecording(String roomName) {
        log.info("🔴 [Jitsi] Recording start requested for '{}' (requires JaaS)", roomName);
    }

    public void stopRecording(String roomName) {
        log.info("⏹️ [Jitsi] Recording stop requested for '{}' (requires JaaS)", roomName);
    }

    public void ejectParticipants(String roomName, List<String> ids) {
        log.info("🚫 [Jitsi] Eject {} participant(s) from '{}'", ids.size(), roomName);
    }

    public void sendAppMessage(String roomName, Map<String, Object> data, String recipient) {
        log.info("📨 [Jitsi] Message for room '{}'", roomName);
    }
}