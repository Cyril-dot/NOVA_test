package com.novaTech.Nova.Services.ZoomServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaTech.Nova.DTO.CreateMeetingDTO;
import com.novaTech.Nova.DTO.DailyRoomResponseDTO;
import com.novaTech.Nova.DTO.DailyTokenResponseDTO;
import com.novaTech.Nova.DTO.JoinMeetingDTO;
import com.novaTech.Nova.DTO.MeetingResponseDTO;
import com.novaTech.Nova.DTO.ParticipantDTO;
import com.novaTech.Nova.Entities.Enums.*;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.meeting.Meeting;
import com.novaTech.Nova.Entities.meeting.MeetingParticipant;
import com.novaTech.Nova.Entities.repo.*;
import com.novaTech.Nova.Services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * MeetingService — Daily.co Edition
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * What changed from the original:
 *
 * 1. Daily.co room management
 *    - createMeeting()  → calls POST https://api.daily.co/v1/rooms
 *    - startMeeting()   → room already exists, just flips DB status to ACTIVE
 *    - endMeeting()     → calls DELETE https://api.daily.co/v1/rooms/{name}
 *                         (kicks everyone out instantly)
 *    - ensureDailyRoom() — idempotent helper: GET first, POST if 404
 *
 * 2. Meeting token generation
 *    - createDailyToken() → POST https://api.daily.co/v1/meeting-tokens
 *      Returns a short-lived JWT the frontend passes to callFrame.join({ token })
 *      The token embeds the user's display name and owner (host) flag so Daily
 *      shows the correct name in the participant list.
 *
 * 3. Everything else (DB persistence, email, participant tracking, kick, etc.)
 *    is unchanged so the rest of the app keeps working normally.
 *
 * Required application.properties / environment variables:
 *   daily.api.key=<your Daily.co API key>
 *   daily.domain=<your Daily.co domain, e.g. noav.daily.co>
 *   daily.api.url=https://api.daily.co/v1   (default, can omit)
 *   daily.room.expiry-hours=24              (default, can omit)
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MeetingService {

    // ── Repositories & services ───────────────────────────────────────────────
    private final MeetingRepository            meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final UserRepo                     userRepo;
    private final PasswordEncoder              passwordEncoder;
    private final EmailService                 emailService;
    private final RestTemplate                 restTemplate;
    private final ObjectMapper                 objectMapper;

    // ── Daily.co config (injected from application.properties) ───────────────
    @Value("${daily.api.key}")
    private String dailyApiKey;

    @Value("${daily.domain:noav.daily.co}")
    private String dailyDomain;

    @Value("${daily.api.url:https://api.daily.co/v1}")
    private String dailyApiUrl;

    /** How many hours until a Daily room auto-expires. Default 24 h. */
    @Value("${daily.room.expiry-hours:24}")
    private int dailyRoomExpiryHours;

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final String MEETING_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int    MEETING_CODE_LENGTH     = 9; // e.g. ABC-DEF-GHI

    // =========================================================================
    //  CREATE MEETING
    //  1. Persist meeting record in DB
    //  2. Create (or reuse) a Daily.co room whose name = sanitised meetingCode
    //  3. Store the Daily room URL on the meeting entity
    // =========================================================================
    public MeetingResponseDTO createMeeting(String userEmail, CreateMeetingDTO dto) {
        log.info("Creating meeting for user: {}", userEmail);

        User host = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent duplicate active meetings for the same host
        Optional<Meeting> existingActiveMeeting = meetingRepository.findActiveHostedMeeting(host.getId());
        if (existingActiveMeeting.isPresent()) {
            throw new RuntimeException(
                    "You already have an active meeting. Please end it before creating a new one.");
        }

        String meetingCode = generateUniqueMeetingCode();

        // Hash password if required
        String hashedPassword = null;
        if (dto.getRequiresPassword() && dto.getPassword() != null) {
            hashedPassword = passwordEncoder.encode(dto.getPassword());
        }

        // ── 1. Persist to DB ──────────────────────────────────────────────────
        Meeting meeting = Meeting.builder()
                .meetingCode(meetingCode)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .host(host)
                .scheduledStartTime(dto.getScheduledStartTime() != null
                        ? dto.getScheduledStartTime() : LocalDateTime.now())
                .status(MeetingStatus.SCHEDULED)
                .maxParticipants(dto.getMaxParticipants())
                .isPublic(dto.getIsPublic())
                .requiresPassword(dto.getRequiresPassword())
                .password(hashedPassword)
                .allowGuests(dto.getAllowGuests())
                .videoEnabled(dto.getVideoEnabled())
                .audioEnabled(dto.getAudioEnabled())
                .screenShareEnabled(dto.getScreenShareEnabled())
                .chatEnabled(dto.getChatEnabled())
                .build();

        Meeting savedMeeting = meetingRepository.save(meeting);

        // ── 2. Create Daily.co room ───────────────────────────────────────────
        try {
            String dailyRoomName = toDailyRoomName(meetingCode);
            DailyRoomResponseDTO room = ensureDailyRoom(
                    dailyRoomName,
                    dto.getMaxParticipants(),
                    dto.getScreenShareEnabled()
            );
            // Store the Daily room URL so the frontend can use it directly
            savedMeeting.setDailyRoomUrl(room.getUrl());
            savedMeeting.setDailyRoomName(room.getName());
            meetingRepository.save(savedMeeting);
            log.info("Daily room created: {} → {}", room.getName(), room.getUrl());
        } catch (Exception e) {
            log.error("Failed to create Daily.co room for meeting {}: {}", meetingCode, e.getMessage());
            // Don't fail the whole request — the frontend can still create its own room
            // using the same meetingCode-derived room name as a fallback.
        }

        // ── 3. Add host as first participant (offline until they actually join) ─
        MeetingParticipant hostParticipant = MeetingParticipant.builder()
                .meeting(savedMeeting)
                .user(host)
                .isGuest(false)
                .role(ParticipantRole.HOST)
                .joinedAt(LocalDateTime.now())
                .isOnline(false)
                .build();
        participantRepository.save(hostParticipant);

        log.info("Meeting created: {} for host: {}", meetingCode, userEmail);

        // Send confirmation email (non-blocking — errors are swallowed)
        try {
            emailService.sendMeetingCreatedEmail(host.getEmail(), savedMeeting);
        } catch (Exception e) {
            log.error("Failed to send meeting creation email", e);
        }

        return convertToResponseDTO(savedMeeting);
    }

    // =========================================================================
    //  START MEETING
    //  Flips DB status to ACTIVE.  The Daily room was already created at
    //  createMeeting() time so nothing extra is needed here.
    // =========================================================================
    public MeetingResponseDTO startMeeting(String userEmail, String meetingCode) {
        log.info("Starting meeting: {} by user: {}", meetingCode, userEmail);

        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!meeting.getHost().getId().equals(user.getId())) {
            throw new RuntimeException("Only the host can start the meeting");
        }
        if (meeting.getStatus() == MeetingStatus.ACTIVE) {
            throw new RuntimeException("Meeting is already active");
        }
        if (meeting.getStatus() == MeetingStatus.ENDED
                || meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new RuntimeException("Cannot start a meeting that has ended or been cancelled");
        }

        // Ensure the Daily room still exists (it may have expired if the meeting
        // was created a long time ago and nobody joined within the expiry window).
        try {
            String dailyRoomName = toDailyRoomName(meetingCode);
            DailyRoomResponseDTO room = ensureDailyRoom(
                    dailyRoomName,
                    meeting.getMaxParticipants(),
                    meeting.getScreenShareEnabled()
            );
            meeting.setDailyRoomUrl(room.getUrl());
            meeting.setDailyRoomName(room.getName());
        } catch (Exception e) {
            log.warn("Could not ensure Daily room on start for {}: {}", meetingCode, e.getMessage());
        }

        meeting.setStatus(MeetingStatus.ACTIVE);
        meeting.setActualStartTime(LocalDateTime.now());
        Meeting saved = meetingRepository.save(meeting);

        log.info("Meeting started: {}", meetingCode);
        return convertToResponseDTO(saved);
    }

    // =========================================================================
    //  GENERATE DAILY MEETING TOKEN  (new endpoint)
    //  Returns a short-lived Daily meeting token for the requesting user.
    //  The frontend passes this to callFrame.join({ token }) so Daily knows
    //  the participant's display name and whether they are the room owner.
    // =========================================================================
    @Transactional(readOnly = true)
    public DailyTokenResponseDTO generateMeetingToken(String userEmail, String meetingCode) {
        log.info("Generating Daily token for user: {} meeting: {}", userEmail, meetingCode);

        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isOwner = meeting.getHost().getId().equals(user.getId());
        String displayName = user.getFirstName() + " " + user.getLastName();
        String dailyRoomName = meeting.getDailyRoomName() != null
                ? meeting.getDailyRoomName()
                : toDailyRoomName(meetingCode);

        String token = createDailyToken(dailyRoomName, displayName, isOwner);

        String roomUrl = meeting.getDailyRoomUrl() != null
                ? meeting.getDailyRoomUrl()
                : "https://" + dailyDomain + "/" + dailyRoomName;

        return DailyTokenResponseDTO.builder()
                .token(token)
                .roomUrl(roomUrl)
                .roomName(dailyRoomName)
                .meetingCode(meetingCode)
                .isOwner(isOwner)
                .build();
    }

    // =========================================================================
    //  GENERATE DAILY MEETING TOKEN — GUEST  (new endpoint)
    // =========================================================================
    @Transactional(readOnly = true)
    public DailyTokenResponseDTO generateGuestMeetingToken(JoinMeetingDTO dto) {
        log.info("Generating Daily token for guest: {} meeting: {}", dto.getGuestName(), dto.getMeetingCode());

        Meeting meeting = meetingRepository.findByMeetingCode(dto.getMeetingCode())
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        if (!meeting.getAllowGuests()) {
            throw new RuntimeException("This meeting does not allow guests");
        }

        String dailyRoomName = meeting.getDailyRoomName() != null
                ? meeting.getDailyRoomName()
                : toDailyRoomName(dto.getMeetingCode());

        String token = createDailyToken(dailyRoomName, dto.getGuestName(), false);

        String roomUrl = meeting.getDailyRoomUrl() != null
                ? meeting.getDailyRoomUrl()
                : "https://" + dailyDomain + "/" + dailyRoomName;

        return DailyTokenResponseDTO.builder()
                .token(token)
                .roomUrl(roomUrl)
                .roomName(dailyRoomName)
                .meetingCode(dto.getMeetingCode())
                .isOwner(false)
                .build();
    }

    // =========================================================================
    //  JOIN MEETING — Authenticated user
    // =========================================================================
    public MeetingResponseDTO joinMeetingAsUser(String userEmail, JoinMeetingDTO dto) {
        log.info("User {} joining meeting: {}", userEmail, dto.getMeetingCode());

        Meeting meeting = meetingRepository.findByMeetingCode(dto.getMeetingCode())
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateMeetingJoin(meeting, dto.getPassword());

        // If participant record already exists, just return — they're re-joining
        Optional<MeetingParticipant> existing =
                participantRepository.findActiveUserParticipant(meeting.getId(), user.getId());
        if (existing.isPresent()) {
            log.info("User {} is already a participant in meeting: {}", userEmail, dto.getMeetingCode());
            return convertToResponseDTO(meeting);
        }

        ParticipantRole role = meeting.getHost().getId().equals(user.getId())
                ? ParticipantRole.HOST
                : ParticipantRole.PARTICIPANT;

        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .isGuest(false)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .isOnline(false) // Set to true when WebSocket connects
                .build();

        participantRepository.save(participant);

        log.info("User {} joined meeting: {}", userEmail, dto.getMeetingCode());
        return convertToResponseDTO(meeting);
    }

    // =========================================================================
    //  JOIN MEETING — Guest
    // =========================================================================
    public MeetingResponseDTO joinMeetingAsGuest(JoinMeetingDTO dto) {
        log.info("Guest {} joining meeting: {}", dto.getGuestName(), dto.getMeetingCode());

        Meeting meeting = meetingRepository.findByMeetingCode(dto.getMeetingCode())
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        if (!meeting.getAllowGuests()) {
            throw new RuntimeException("This meeting does not allow guests");
        }

        validateMeetingJoin(meeting, dto.getPassword());

        if (dto.getGuestName() == null || dto.getGuestName().trim().isEmpty()) {
            throw new RuntimeException("Guest name is required");
        }

        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .guestName(dto.getGuestName())
                .guestEmail(dto.getGuestEmail())
                .isGuest(true)
                .role(ParticipantRole.PARTICIPANT)
                .joinedAt(LocalDateTime.now())
                .isOnline(false)
                .build();

        participantRepository.save(participant);

        log.info("Guest {} joined meeting: {}", dto.getGuestName(), dto.getMeetingCode());
        return convertToResponseDTO(meeting);
    }

    // =========================================================================
    //  END MEETING
    //  1. Updates DB status to ENDED
    //  2. Deletes the Daily.co room  ← this immediately disconnects all
    //     participants inside the Daily iframe on their browsers
    // =========================================================================
    public void endMeeting(String userEmail, String meetingCode) {
        log.info("Ending meeting: {} by user: {}", meetingCode, userEmail);

        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!meeting.getHost().getId().equals(user.getId())) {
            throw new RuntimeException("Only the host can end the meeting");
        }

        // ── 1. Delete Daily room (boots everyone out) ─────────────────────────
        String dailyRoomName = meeting.getDailyRoomName() != null
                ? meeting.getDailyRoomName()
                : toDailyRoomName(meetingCode);

        try {
            deleteDailyRoom(dailyRoomName);
            log.info("Daily room deleted: {}", dailyRoomName);
        } catch (Exception e) {
            // Log but don't abort — we still want the DB to be updated
            log.warn("Failed to delete Daily room {}: {}", dailyRoomName, e.getMessage());
        }

        // ── 2. Update DB ──────────────────────────────────────────────────────
        meeting.setStatus(MeetingStatus.ENDED);
        meeting.setEndTime(LocalDateTime.now());

        meeting.getParticipants().stream()
                .filter(p -> p.getLeftAt() == null)
                .forEach(MeetingParticipant::leave);

        meetingRepository.save(meeting);

        log.info("Meeting {} ended", meetingCode);

        try {
            emailService.sendMeetingSummaryEmail(user.getEmail(), meeting);
        } catch (Exception e) {
            log.error("Failed to send meeting summary email", e);
        }
    }

    // =========================================================================
    //  GET MEETING DETAILS
    // =========================================================================
    @Transactional(readOnly = true)
    public MeetingResponseDTO getMeetingDetails(String meetingCode) {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));
        return convertToResponseDTO(meeting);
    }

    // =========================================================================
    //  GET USER'S MEETINGS
    // =========================================================================
    @Transactional(readOnly = true)
    public List<MeetingResponseDTO> getUserMeetings(String userEmail, MeetingStatus status) {
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Meeting> meetings = (status != null)
                ? meetingRepository.findByHostIdAndStatus(user.getId(), status)
                : meetingRepository.findByHostId(user.getId());

        return meetings.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    // =========================================================================
    //  UPDATE PARTICIPANT STATUS
    // =========================================================================
    public void updateParticipantStatus(String sessionId,
                                        Boolean videoEnabled,
                                        Boolean audioEnabled,
                                        Boolean screenSharing) {
        MeetingParticipant participant = participantRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        if (videoEnabled  != null) participant.setVideoEnabled(videoEnabled);
        if (audioEnabled  != null) participant.setAudioEnabled(audioEnabled);
        if (screenSharing != null) participant.setScreenSharing(screenSharing);

        participantRepository.save(participant);
        log.info("Updated media status for participant: {}", participant.getDisplayName());
    }

    // =========================================================================
    //  KICK PARTICIPANT
    // =========================================================================
    public void kickParticipant(String moderatorEmail, UUID participantId) {
        User moderator = userRepo.findByEmail(moderatorEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MeetingParticipant target = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        Meeting meeting = target.getMeeting();

        MeetingParticipant moderatorParticipant = participantRepository
                .findActiveUserParticipant(meeting.getId(), moderator.getId())
                .orElseThrow(() -> new RuntimeException("You are not in this meeting"));

        if (!moderatorParticipant.isModerator()) {
            throw new RuntimeException("Only moderators can kick participants");
        }
        if (target.getId().equals(moderatorParticipant.getId())) {
            throw new RuntimeException("You cannot kick yourself");
        }
        if (target.isHost()) {
            throw new RuntimeException("You cannot kick the host");
        }

        target.leave();
        participantRepository.save(target);

        log.info("Participant {} kicked by {}", target.getDisplayName(), moderatorEmail);
    }

    // =========================================================================
    //  WEBSOCKET DISCONNECT LISTENER
    //  Still needed for presence tracking even though Daily handles video.
    // =========================================================================
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (sessionId == null) {
            log.warn("Disconnect event with null sessionId — skipping.");
            return;
        }

        log.info("WebSocket disconnected: {}", sessionId);

        try {
            MeetingParticipant participant = participantRepository.findBySessionId(sessionId)
                    .orElse(null);

            if (participant == null) return;

            participant.leave();
            participantRepository.save(participant);

            // Auto-end if no participants remain
            Meeting meeting = participant.getMeeting();
            long activeCount = meeting.getParticipants().stream()
                    .filter(p -> p.getLeftAt() == null)
                    .count();

            if (activeCount == 0 && meeting.getStatus() == MeetingStatus.ACTIVE) {
                endMeeting(meeting.getHost().getEmail(), meeting.getMeetingCode());
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket disconnect for session {}: {}", sessionId, e.getMessage());
        }
    }

    // =========================================================================
    //  DAILY.CO REST HELPERS
    // =========================================================================

    /**
     * GET the room first (idempotent). If it doesn't exist, POST to create it.
     * Returns the room object from Daily.
     */
    @SuppressWarnings("unchecked")
    private DailyRoomResponseDTO ensureDailyRoom(String roomName,
                                                  int maxParticipants,
                                                  boolean screenShareEnabled) {
        HttpHeaders headers = dailyHeaders();

        // Try to fetch existing room
        try {
            ResponseEntity<Map> getResponse = restTemplate.exchange(
                    dailyApiUrl + "/rooms/" + roomName,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (getResponse.getStatusCode() == HttpStatus.OK && getResponse.getBody() != null) {
                return mapToDailyRoom(getResponse.getBody());
            }
        } catch (Exception e) {
            log.debug("Room {} not found, creating: {}", roomName, e.getMessage());
        }

        // Create new room
        return createDailyRoom(roomName, maxParticipants, screenShareEnabled);
    }

    /**
     * POST /rooms — creates a new Daily.co room.
     */
    @SuppressWarnings("unchecked")
    private DailyRoomResponseDTO createDailyRoom(String roomName,
                                                  int maxParticipants,
                                                  boolean screenShareEnabled) {
        HttpHeaders headers = dailyHeaders();

        long expiryEpoch = (System.currentTimeMillis() / 1000L)
                + (dailyRoomExpiryHours * 3600L);

        Map<String, Object> properties = new HashMap<>();
        properties.put("enable_screenshare",  screenShareEnabled);
        properties.put("enable_chat",         false); // Chat handled by sendAppMessage in frontend
        properties.put("enable_knocking",     false);
        properties.put("start_video_off",     false);
        properties.put("start_audio_off",     false);
        properties.put("max_participants",    maxParticipants > 0 ? maxParticipants : 50);
        properties.put("exp",                 expiryEpoch);

        Map<String, Object> body = new HashMap<>();
        body.put("name",       roomName);
        body.put("privacy",    "public");
        body.put("properties", properties);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    dailyApiUrl + "/rooms",
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Daily room created: {}", roomName);
                return mapToDailyRoom(response.getBody());
            }

            throw new RuntimeException("Daily API returned: " + response.getStatusCode());

        } catch (Exception e) {
            // 409 = room already exists — fetch it
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                log.info("Room {} already exists, fetching…", roomName);
                ResponseEntity<Map> retry = restTemplate.exchange(
                        dailyApiUrl + "/rooms/" + roomName,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Map.class
                );
                if (retry.getBody() != null) {
                    return mapToDailyRoom(retry.getBody());
                }
            }
            throw new RuntimeException("Failed to create Daily room: " + e.getMessage(), e);
        }
    }

    /**
     * DELETE /rooms/{name} — deletes the Daily room and boots everyone out.
     */
    private void deleteDailyRoom(String roomName) {
        try {
            restTemplate.exchange(
                    dailyApiUrl + "/rooms/" + roomName,
                    HttpMethod.DELETE,
                    new HttpEntity<>(dailyHeaders()),
                    Void.class
            );
            log.info("Deleted Daily room: {}", roomName);
        } catch (Exception e) {
            log.warn("Could not delete Daily room {}: {}", roomName, e.getMessage());
        }
    }

    /**
     * POST /meeting-tokens — issues a signed JWT for a participant.
     * The frontend passes this to callFrame.join({ token }) so Daily can
     * display the correct display name and grant owner permissions if isOwner=true.
     */
    @SuppressWarnings("unchecked")
    private String createDailyToken(String roomName, String displayName, boolean isOwner) {
        HttpHeaders headers = dailyHeaders();

        Map<String, Object> tokenProperties = new HashMap<>();
        tokenProperties.put("room_name",          roomName);
        tokenProperties.put("user_name",           displayName);
        tokenProperties.put("is_owner",            isOwner);
        tokenProperties.put("enable_screenshare",  true);
        // Token expires in 2 hours
        tokenProperties.put("exp",
                (System.currentTimeMillis() / 1000L) + 7200L);

        Map<String, Object> body = Map.of("properties", tokenProperties);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    dailyApiUrl + "/meeting-tokens",
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().containsKey("token")) {
                return (String) response.getBody().get("token");
            }

            log.warn("Daily token response missing 'token' field: {}", response.getBody());
            return null;

        } catch (Exception e) {
            log.error("Failed to create Daily meeting token: {}", e.getMessage());
            return null; // Frontend will join without a token (still works, just without owner perms)
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Build auth headers for all Daily REST calls. */
    private HttpHeaders dailyHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(dailyApiKey);
        return headers;
    }

    /**
     * Convert a meeting code like "ABC-DEF-GHI" into a valid Daily room name
     * (lowercase alphanumeric + hyphens only, no leading/trailing hyphens).
     */
    private String toDailyRoomName(String meetingCode) {
        return meetingCode
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    @SuppressWarnings("unchecked")
    private DailyRoomResponseDTO mapToDailyRoom(Map<String, Object> body) {
        String name = (String) body.get("name");
        String url  = (String) body.getOrDefault("url",
                "https://" + dailyDomain + "/" + name);
        return DailyRoomResponseDTO.builder()
                .name(name)
                .url(url)
                .build();
    }

    // =========================================================================
    //  MEETING CODE GENERATION
    // =========================================================================

    private String generateUniqueMeetingCode() {
        String code;
        do {
            code = generateMeetingCode();
        } while (meetingRepository.findByMeetingCode(code).isPresent());
        return code;
    }

    private String generateMeetingCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code  = new StringBuilder();
        for (int i = 0; i < MEETING_CODE_LENGTH; i++) {
            if (i > 0 && i % 3 == 0) code.append('-');
            code.append(MEETING_CODE_CHARACTERS
                    .charAt(random.nextInt(MEETING_CODE_CHARACTERS.length())));
        }
        return code.toString();
    }

    // =========================================================================
    //  DTO CONVERTERS
    // =========================================================================

    private MeetingResponseDTO convertToResponseDTO(Meeting meeting) {
        List<ParticipantDTO> participants = meeting.getParticipants().stream()
                .map(this::convertToParticipantDTO)
                .collect(Collectors.toList());

        return MeetingResponseDTO.builder()
                .id(meeting.getId())
                .meetingCode(meeting.getMeetingCode())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .hostName(meeting.getHost().getFirstName() + " " + meeting.getHost().getLastName())
                .hostId(meeting.getHost().getId())
                .scheduledStartTime(meeting.getScheduledStartTime())
                .actualStartTime(meeting.getActualStartTime())
                .endTime(meeting.getEndTime())
                .status(meeting.getStatus())
                .maxParticipants(meeting.getMaxParticipants())
                .currentParticipants(meeting.getCurrentParticipantCount())
                .isPublic(meeting.getIsPublic())
                .requiresPassword(meeting.getRequiresPassword())
                .allowGuests(meeting.getAllowGuests())
                .videoEnabled(meeting.getVideoEnabled())
                .audioEnabled(meeting.getAudioEnabled())
                .screenShareEnabled(meeting.getScreenShareEnabled())
                .chatEnabled(meeting.getChatEnabled())
                // ── New Daily.co fields ──────────────────────────────────────
                .dailyRoomUrl(meeting.getDailyRoomUrl())
                .dailyRoomName(meeting.getDailyRoomName())
                // ─────────────────────────────────────────────────────────────
                .participants(participants)
                .createdAt(meeting.getCreatedAt())
                .build();
    }

    private ParticipantDTO convertToParticipantDTO(MeetingParticipant p) {
        return ParticipantDTO.builder()
                .id(p.getId())
                .displayName(p.getDisplayName())
                .email(p.getIsGuest()
                        ? p.getGuestEmail()
                        : (p.getUser() != null ? p.getUser().getEmail() : null))
                .isGuest(p.getIsGuest())
                .role(p.getRole().name())
                .videoEnabled(p.getVideoEnabled())
                .audioEnabled(p.getAudioEnabled())
                .screenSharing(p.getScreenSharing())
                .isOnline(p.getIsOnline())
                .joinedAt(p.getJoinedAt())
                .build();
    }

    // =========================================================================
    //  VALIDATE MEETING CAN BE JOINED
    // =========================================================================
    private void validateMeetingJoin(Meeting meeting, String providedPassword) {
        if (meeting.getStatus() == MeetingStatus.ENDED) {
            throw new RuntimeException("This meeting has ended");
        }
        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new RuntimeException("This meeting has been cancelled");
        }
        if (meeting.isFull()) {
            throw new RuntimeException("This meeting is full");
        }
        if (meeting.getRequiresPassword()) {
            if (providedPassword == null || providedPassword.isEmpty()) {
                throw new RuntimeException("This meeting requires a password");
            }
            if (!passwordEncoder.matches(providedPassword, meeting.getPassword())) {
                throw new RuntimeException("Incorrect meeting password");
            }
        }
    }
}