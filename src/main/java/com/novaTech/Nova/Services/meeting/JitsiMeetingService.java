package com.novaTech.Nova.Services.meeting;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
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
        room.put("appId",      config.getAppId());
        room.put("active",     true);

        roomStore.put(sanitised, room);
        log.info("✅ [JaaS] Room registered: {} | url: {}", sanitised, roomUrl);
        return new HashMap<>(room);
    }

    public Map<String, Object> getRoom(String roomName) {
        if (roomName == null || roomName.isBlank()) return null;
        Map<String, Object> room = roomStore.get(roomName);
        if (room == null) {
            log.warn("⚠️ [JaaS] Room not found: {}", roomName);
        }
        return room != null ? new HashMap<>(room) : null;
    }

    public boolean deleteRoom(String roomName) {
        Map<String, Object> removed = roomStore.remove(roomName);
        if (removed != null) {
            log.info("🗑️ [JaaS] Room deleted: {}", roomName);
        } else {
            log.warn("⚠️ [JaaS] Delete: room not found (already deleted?): {}", roomName);
        }
        // Always return true — frontend shouldn't error on double-delete
        return true;
    }

    public Map<String, Object> listRooms(int limit, String startingAfter) {
        try {
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

            int safeLimit = Math.min(Math.max(limit, 1), 100);
            int toIndex   = Math.min(fromIndex + safeLimit, rooms.size());
            fromIndex     = Math.min(fromIndex, rooms.size());

            List<Map<String, Object>> page = new ArrayList<>(rooms.subList(fromIndex, toIndex));

            Map<String, Object> result = new HashMap<>();
            result.put("total_count", rooms.size());
            result.put("data", page);
            return result;

        } catch (Exception e) {
            log.error("❌ [JaaS] listRooms error: {}", e.getMessage(), e);
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
            // Auto-create room in store if missing
            Map<String, Object> room = getRoom(roomName);
            if (room == null) {
                room = createRoom(roomName, false);
            }

            String roomUrl = (String) room.get("url");

            Map<String, Object> result = new HashMap<>();
            result.put("roomUrl",     roomUrl);
            result.put("roomName",    roomName);
            result.put("domain",      config.getDomain());
            result.put("appId",       config.getAppId());
            result.put("isModerator", isModerator);
            result.put("isOwner",     isModerator);
            result.put("userName",    userName);

            String token = generateJaaSToken(roomName, userId, userName, isModerator);
            result.put("token", token);

            log.info("🎟️ [JaaS] Token created for '{}' in room '{}' | moderator={}", userName, roomName, isModerator);
            return result;

        } catch (Exception e) {
            log.error("❌ [JaaS] createMeetingToken error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create JaaS meeting token: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> createGuestToken(String roomName, String displayName) {
        String guestId = "guest-" + UUID.randomUUID().toString().substring(0, 8);
        String name    = (displayName != null && !displayName.isBlank()) ? displayName : "Guest";
        return createMeetingToken(roomName, guestId, name, false);
    }

    /**
     * Generates a proper RS256-signed JaaS JWT.
     *
     * JWT structure required by 8x8 JaaS:
     *   Header : { alg: RS256, kid: <apiKeyId>, typ: JWT }
     *   Payload: { aud, iss, sub, room, exp, nbf, context: { user, features, room } }
     */
    private String generateJaaSToken(String room, String userId, String userName, boolean isModerator) {
        try {
            PrivateKey privateKey = loadPrivateKey();

            long nowSec = Instant.now().getEpochSecond();
            long expSec = nowSec + config.getRoomExpirySeconds();

            // ── context.user ─────────────────────────────────────────────────
            // IMPORTANT: moderator must be the STRING "true"/"false", not boolean
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id",       userId);
            user.put("name",     userName);
            user.put("email",    userName + "@nova.meet");
            user.put("avatar",   "");
            user.put("moderator", isModerator ? "true" : "false");
            user.put("hidden-from-recorder", false);

            // ── context.features ─────────────────────────────────────────────
            Map<String, Object> features = new LinkedHashMap<>();
            features.put("recording",        isModerator); // only moderators can record
            features.put("livestreaming",    false);
            features.put("transcription",    false);
            features.put("outbound-call",    false);
            features.put("sip-outbound-call",false);
            features.put("sip-inbound-call", false);

            // ── context.room ─────────────────────────────────────────────────
            Map<String, Object> roomConfig = new LinkedHashMap<>();
            roomConfig.put("regex", false);

            // ── context ──────────────────────────────────────────────────────
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("user",     user);
            context.put("features", features);
            context.put("room",     roomConfig);

            // ── Build and sign JWT ────────────────────────────────────────────
            return Jwts.builder()
                    // Header
                    .setHeaderParam("kid", config.getApiKeyId())
                    .setHeaderParam("typ", "JWT")
                    // Standard claims
                    .claim("aud", "jitsi")
                    .claim("iss", "chat")
                    .claim("sub", config.getAppId())          // your AppID
                    .claim("room", "*")                        // wildcard — works for all rooms
                    // Timing
                    .setIssuedAt(  Date.from(Instant.ofEpochSecond(nowSec)))
                    .setNotBefore( Date.from(Instant.ofEpochSecond(nowSec)))
                    .setExpiration(Date.from(Instant.ofEpochSecond(expSec)))
                    // Context (user info + permissions)
                    .claim("context", context)
                    // Sign with RS256
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

        } catch (Exception e) {
            log.error("❌ [JaaS] JWT generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("JWT generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the RS256 private key from config.
     *
     * Accepts both:
     *   - PKCS#8 PEM  (-----BEGIN PRIVATE KEY-----)
     *   - Raw base64  (no header/footer — what JaaS dashboard gives you)
     */
    private PrivateKey loadPrivateKey() throws Exception {
        String raw = config.getPrivateKey();

        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException(
                "jitsi.private.key is not configured. " +
                "Set it in application.properties or as an environment variable JITSI_PRIVATE_KEY."
            );
        }

        // Strip PEM headers/footers and all whitespace/newlines
        String base64 = raw
                .replace("-----BEGIN PRIVATE KEY-----",     "")
                .replace("-----END PRIVATE KEY-----",       "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----",   "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
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
            log.error("❌ [JaaS] listMeetings error: {}", e.getMessage(), e);
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
        log.info("🔴 [JaaS] Recording start requested for '{}' — requires JaaS REST API integration", roomName);
        // TODO: call JaaS recording REST API if needed
    }

    public void stopRecording(String roomName) {
        log.info("⏹️ [JaaS] Recording stop requested for '{}' — requires JaaS REST API integration", roomName);
        // TODO: call JaaS recording REST API if needed
    }

    public void ejectParticipants(String roomName, List<String> ids) {
        log.info("🚫 [JaaS] Eject {} participant(s) from '{}'", ids.size(), roomName);
    }

    public void sendAppMessage(String roomName, Map<String, Object> data, String recipient) {
        log.info("📨 [JaaS] Message for room '{}'", roomName);
    }
}