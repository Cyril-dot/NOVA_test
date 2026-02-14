package com.novaTech.Nova.Services.ZoomServiceImpl;

import com.novaTech.Nova.DTO.CreateMeetingDTO;
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
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String MEETING_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int MEETING_CODE_LENGTH = 9; // Format: XXX-XXX-XXX

    // ========================
    // CREATE MEETING
    // ========================
    public MeetingResponseDTO createMeeting(String userEmail, CreateMeetingDTO dto) {
        log.info("Creating meeting for user: {}", userEmail);

        User host = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user already has an active meeting
        Optional<Meeting> existingActiveMeeting = meetingRepository.findActiveHostedMeeting(host.getId());
        if (existingActiveMeeting.isPresent()) {
            throw new RuntimeException("You already have an active meeting. Please end it before creating a new one.");
        }

        // Generate unique meeting code
        String meetingCode = generateUniqueMeetingCode();

        // Hash password if provided
        String hashedPassword = null;
        if (dto.getRequiresPassword() && dto.getPassword() != null) {
            hashedPassword = passwordEncoder.encode(dto.getPassword());
        }

        Meeting meeting = Meeting.builder()
                .meetingCode(meetingCode)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .host(host)
                .scheduledStartTime(dto.getScheduledStartTime() != null ?
                        dto.getScheduledStartTime() : LocalDateTime.now())
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

        // Create host as first participant
        MeetingParticipant hostParticipant = MeetingParticipant.builder()
                .meeting(savedMeeting)
                .user(host)
                .isGuest(false)
                .role(ParticipantRole.HOST)
                .joinedAt(LocalDateTime.now())
                .isOnline(false) // Will be set when they actually join via WebSocket
                .build();

        participantRepository.save(hostParticipant);

        log.info("Meeting created successfully: {} for host: {}", meetingCode, userEmail);

        // Send meeting creation email
        try {
            emailService.sendMeetingCreatedEmail(host.getEmail(), savedMeeting);
        } catch (Exception e) {
            log.error("Failed to send meeting creation email", e);
        }

        return convertToResponseDTO(savedMeeting);
    }

    // ========================
    // START MEETING
    // ========================
    public MeetingResponseDTO startMeeting(String userEmail, String meetingCode) {
        log.info("Starting meeting: {} by user: {}", meetingCode, userEmail);

        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is the host
        if (!meeting.getHost().getId().equals(user.getId())) {
            throw new RuntimeException("Only the host can start the meeting");
        }

        if (meeting.getStatus() == MeetingStatus.ACTIVE) {
            throw new RuntimeException("Meeting is already active");
        }

        if (meeting.getStatus() == MeetingStatus.ENDED || meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new RuntimeException("Cannot start a meeting that has ended or been cancelled");
        }

        meeting.setStatus(MeetingStatus.ACTIVE);
        meeting.setActualStartTime(LocalDateTime.now());
        Meeting savedMeeting = meetingRepository.save(meeting);

        log.info("Meeting started successfully: {}", meetingCode);

        return convertToResponseDTO(savedMeeting);
    }

    // ========================
    // JOIN MEETING (For Registered Users)
    // ========================
    public MeetingResponseDTO joinMeetingAsUser(String userEmail, JoinMeetingDTO dto) {
        log.info("User {} attempting to join meeting: {}", userEmail, dto.getMeetingCode());

        Meeting meeting = meetingRepository.findByMeetingCode(dto.getMeetingCode())
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate meeting can be joined
        validateMeetingJoin(meeting, dto.getPassword());

        // Check if user is already a participant
        Optional<MeetingParticipant> existingParticipant =
                participantRepository.findActiveUserParticipant(meeting.getId(), user.getId());

        if (existingParticipant.isPresent()) {
            log.info("User {} is already in meeting: {}", userEmail, dto.getMeetingCode());
            return convertToResponseDTO(meeting);
        }

        // Determine role
        ParticipantRole role = meeting.getHost().getId().equals(user.getId()) ?
                ParticipantRole.HOST : ParticipantRole.PARTICIPANT;

        // Create participant record
        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .isGuest(false)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .isOnline(false) // Will be set when WebSocket connects
                .build();

        participantRepository.save(participant);

        log.info("User {} joined meeting: {} successfully", userEmail, dto.getMeetingCode());

        return convertToResponseDTO(meeting);
    }

    // ========================
    // JOIN MEETING (For Guests)
    // ========================
    public MeetingResponseDTO joinMeetingAsGuest(JoinMeetingDTO dto) {
        log.info("Guest {} attempting to join meeting: {}", dto.getGuestName(), dto.getMeetingCode());

        Meeting meeting = meetingRepository.findByMeetingCode(dto.getMeetingCode())
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        // Check if guests are allowed
        if (!meeting.getAllowGuests()) {
            throw new RuntimeException("This meeting does not allow guests");
        }

        // Validate meeting can be joined
        validateMeetingJoin(meeting, dto.getPassword());

        // Validate guest info
        if (dto.getGuestName() == null || dto.getGuestName().trim().isEmpty()) {
            throw new RuntimeException("Guest name is required");
        }

        // Create guest participant record
        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .guestName(dto.getGuestName())
                .guestEmail(dto.getGuestEmail())
                .isGuest(true)
                .role(ParticipantRole.PARTICIPANT)
                .joinedAt(LocalDateTime.now())
                .isOnline(false) // Will be set when WebSocket connects
                .build();

        participantRepository.save(participant);

        log.info("Guest {} joined meeting: {} successfully", dto.getGuestName(), dto.getMeetingCode());

        return convertToResponseDTO(meeting);
    }

    // ========================
    // LEAVE MEETING
    // Triggered automatically when a WebSocket session disconnects.
    // The sessionId is stored on MeetingParticipant when they connect via WebSocket.
    // ========================
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (sessionId == null) {
            log.warn("Received disconnect event with null sessionId, skipping.");
            return;
        }

        log.info("WebSocket disconnected — sessionId: {}", sessionId);
        leaveMeeting(sessionId);
    }

    private void leaveMeeting(String sessionId) {
        log.info("Participant leaving meeting with sessionId: {}", sessionId);

        MeetingParticipant participant = participantRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        participant.leave();
        participantRepository.save(participant);

        log.info("Participant {} left meeting successfully", participant.getDisplayName());

        // If meeting has no more active participants, end it
        Meeting meeting = participant.getMeeting();
        long activeCount = meeting.getParticipants().stream()
                .filter(p -> p.getLeftAt() == null)
                .count();

        if (activeCount == 0 && meeting.getStatus() == MeetingStatus.ACTIVE) {
            endMeeting(meeting.getHost().getEmail(), meeting.getMeetingCode());
        }
    }

    // ========================
    // END MEETING
    // ========================
    public void endMeeting(String userEmail, String meetingCode) {
        log.info("Ending meeting: {} by user: {}", meetingCode, userEmail);

        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only host can end meeting
        if (!meeting.getHost().getId().equals(user.getId())) {
            throw new RuntimeException("Only the host can end the meeting");
        }

        meeting.setStatus(MeetingStatus.ENDED);
        meeting.setEndTime(LocalDateTime.now());

        // Mark all active participants as left
        meeting.getParticipants().stream()
                .filter(p -> p.getLeftAt() == null)
                .forEach(MeetingParticipant::leave);

        meetingRepository.save(meeting);

        log.info("Meeting {} ended successfully", meetingCode);

        // Send meeting summary email
        try {
            emailService.sendMeetingSummaryEmail(user.getEmail(), meeting);
        } catch (Exception e) {
            log.error("Failed to send meeting summary email", e);
        }
    }

    // ========================
    // GET MEETING DETAILS
    // ========================
    @Transactional(readOnly = true)
    public MeetingResponseDTO getMeetingDetails(String meetingCode) {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        return convertToResponseDTO(meeting);
    }

    // ========================
    // GET USER'S MEETINGS
    // ========================
    @Transactional(readOnly = true)
    public List<MeetingResponseDTO> getUserMeetings(String userEmail, MeetingStatus status) {
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Meeting> meetings;
        if (status != null) {
            meetings = meetingRepository.findByHostIdAndStatus(user.getId(), status);
        } else {
            meetings = meetingRepository.findByHostId(user.getId());
        }

        return meetings.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    // ========================
    // UPDATE PARTICIPANT STATUS
    // ========================
    public void updateParticipantStatus(String sessionId, Boolean videoEnabled, Boolean audioEnabled, Boolean screenSharing) {
        MeetingParticipant participant = participantRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        if (videoEnabled != null) participant.setVideoEnabled(videoEnabled);
        if (audioEnabled != null) participant.setAudioEnabled(audioEnabled);
        if (screenSharing != null) participant.setScreenSharing(screenSharing);

        participantRepository.save(participant);

        log.info("Updated status for participant: {}", participant.getDisplayName());
    }

    // ========================
    // KICK PARTICIPANT (Moderator only)
    // ========================
    public void kickParticipant(String moderatorEmail, UUID participantId) {
        User moderator = userRepo.findByEmail(moderatorEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MeetingParticipant participantToKick = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        Meeting meeting = participantToKick.getMeeting();

        // Verify moderator has permission
        MeetingParticipant moderatorParticipant = participantRepository
                .findActiveUserParticipant(meeting.getId(), moderator.getId())
                .orElseThrow(() -> new RuntimeException("You are not in this meeting"));

        if (!moderatorParticipant.isModerator()) {
            throw new RuntimeException("Only moderators can kick participants");
        }

        // Can't kick yourself or the host
        if (participantToKick.getId().equals(moderatorParticipant.getId())) {
            throw new RuntimeException("You cannot kick yourself");
        }

        if (participantToKick.isHost()) {
            throw new RuntimeException("You cannot kick the host");
        }

        participantToKick.leave();
        participantRepository.save(participantToKick);

        log.info("Participant {} was kicked from meeting by {}",
                participantToKick.getDisplayName(), moderatorEmail);
    }

    // ========================
    // HELPER METHODS
    // ========================
    private void validateMeetingJoin(Meeting meeting, String providedPassword) {
        if (meeting.getStatus() == MeetingStatus.ENDED) {
            throw new RuntimeException("This meeting has ended");
        }

        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new RuntimeException("This meeting has been cancelled");
        }

        // Check if meeting is full
        if (meeting.isFull()) {
            throw new RuntimeException("This meeting is full");
        }

        // Verify password if required
        if (meeting.getRequiresPassword()) {
            if (providedPassword == null || providedPassword.isEmpty()) {
                throw new RuntimeException("This meeting requires a password");
            }

            if (!passwordEncoder.matches(providedPassword, meeting.getPassword())) {
                throw new RuntimeException("Incorrect meeting password");
            }
        }
    }

    private String generateUniqueMeetingCode() {
        String code;
        do {
            code = generateMeetingCode();
        } while (meetingRepository.findByMeetingCode(code).isPresent());

        return code;
    }

    private String generateMeetingCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < MEETING_CODE_LENGTH; i++) {
            if (i > 0 && i % 3 == 0) {
                code.append('-');
            }
            int index = random.nextInt(MEETING_CODE_CHARACTERS.length());
            code.append(MEETING_CODE_CHARACTERS.charAt(index));
        }

        return code.toString();
    }

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
                .participants(participants)
                .createdAt(meeting.getCreatedAt())
                .build();
    }

    private ParticipantDTO convertToParticipantDTO(MeetingParticipant participant) {
        return ParticipantDTO.builder()
                .id(participant.getId())
                .displayName(participant.getDisplayName())
                // ✅ FIX: Add null check for user
                .email(participant.getIsGuest()
                        ? participant.getGuestEmail()
                        : (participant.getUser() != null ? participant.getUser().getEmail() : null))
                .isGuest(participant.getIsGuest())
                .role(participant.getRole().name())
                .videoEnabled(participant.getVideoEnabled())
                .audioEnabled(participant.getAudioEnabled())
                .screenSharing(participant.getScreenSharing())
                .isOnline(participant.getIsOnline())
                .joinedAt(participant.getJoinedAt())
                .build();
    }
}