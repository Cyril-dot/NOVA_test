package com.novaTech.Nova.Services.ZoomServiceImpl;

import com.novaTech.Nova.Entities.Enums.MeetingStatus;
import com.novaTech.Nova.Entities.meeting.Meeting;
import com.novaTech.Nova.Entities.meeting.MeetingParticipant;
import com.novaTech.Nova.Entities.meeting.ScheduledMeeting;
import com.novaTech.Nova.Entities.repo.MeetingParticipantRepository;
import com.novaTech.Nova.Entities.repo.MeetingRepository;
import com.novaTech.Nova.Entities.repo.ScheduledMeetingRepository;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.UserRepo;
import com.novaTech.Nova.Exceptions.ResourceNotFoundException;
import com.novaTech.Nova.Exceptions.UnauthorizedException;
import com.novaTech.Nova.Services.MessagingChatService.WebSocketMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final ScheduledMeetingRepository scheduledMeetingRepository;
    private final WebSocketMessageService webSocketMessageService;
    private final GetStreamVideoService getStreamVideoService;
    private final UserRepo userRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private String generateCallId() {
        return "call-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private String generateMeetingUrl(String callId) {
        return String.format("%s/meeting/%s", frontendUrl, callId);
    }

    @Transactional
    public Meeting createInstantMeeting(UUID hostId, String title, Boolean waitingRoomEnabled,
                                        Boolean recordingEnabled, UUID teamId) {
        log.info("Creating instant meeting for host {}", hostId);

        String callId = generateCallId();
        String meetingLink = generateMeetingUrl(callId);

        Meeting meeting = Meeting.builder()
                .meetingLink(meetingLink)
                .hostId(hostId)
                .title(title != null ? title : "Instant Meeting")
                .description(callId)
                .waitingRoomEnabled(waitingRoomEnabled != null ? waitingRoomEnabled : false)
                .recordingEnabled(recordingEnabled != null ? recordingEnabled : false)
                .teamId(teamId)
                .startedAt(LocalDateTime.now())
                .endedAt(LocalDateTime.now().plusHours(10))
                .status(MeetingStatus.SCHEDULED)
                .build();

        Meeting saved = meetingRepository.save(meeting);
        joinMeeting(saved.getId(), hostId);

        log.info("Instant meeting created: {} with callId: {}", saved.getId(), callId);
        return saved;
    }

    @Transactional
    public ScheduledMeeting scheduleMeeting(UUID organizerId, String title, String description,
                                            LocalDateTime startTime, LocalDateTime endTime,
                                            String timezone, List<UUID> invitedUserIds,
                                            UUID teamId) {
        log.info("Scheduling meeting for organizer {}", organizerId);

        ScheduledMeeting scheduled = ScheduledMeeting.builder()
                .title(title)
                .description(description)
                .organizerId(organizerId)
                .scheduledStartTime(startTime)
                .scheduledEndTime(endTime)
                .timeZone(timezone)
                .invitedUserIds(invitedUserIds != null ? invitedUserIds : new ArrayList<>())
                .teamId(teamId)
                .build();

        ScheduledMeeting saved = scheduledMeetingRepository.save(scheduled);

        if (invitedUserIds != null) {
            invitedUserIds.forEach(userId ->
                    webSocketMessageService.sendMeetingInvitation(userId, saved)
            );
        }

        return saved;
    }

    @Transactional
    public MeetingParticipant joinMeeting(Long meetingId, UUID userId) {
        log.info("User {} joining meeting {}", userId, meetingId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        Optional<MeetingParticipant> existing =
                participantRepository.findByMeetingIdAndUserId(meetingId, userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        MeetingParticipant participant = MeetingParticipant.builder()
                .meetingId(meetingId)
                .userId(userId)
                .isInWaitingRoom(meeting.getWaitingRoomEnabled() && !meeting.getHostId().equals(userId))
                .build();

        MeetingParticipant saved = participantRepository.save(participant);

        if (saved.getIsInWaitingRoom()) {
            webSocketMessageService.sendWaitingRoomNotification(meeting.getHostId(), userId);
        } else {
            notifyAllParticipants(meetingId, "participant_joined", userId);
        }

        return saved;
    }

    public Map<String, Object> getCallCredentials(Long meetingId, UUID userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String userName = getUserDisplayName(user);
        String callId = meeting.getDescription();

        if (callId == null || !callId.startsWith("call-")) {
            callId = extractCallIdFromMeetingLink(meeting.getMeetingLink());
        }

        Map<String, String> callData = getStreamVideoService.generateCallData(userId, userName, callId);

        Map<String, Object> response = new HashMap<>();
        response.put("apiKey", callData.get("apiKey"));
        response.put("userId", callData.get("userId"));
        response.put("userName", callData.get("userName"));
        response.put("token", callData.get("userToken"));
        response.put("callId", callData.get("callId"));
        response.put("callType", callData.get("callType"));
        response.put("meetingId", meetingId);
        response.put("meetingLink", meeting.getMeetingLink());

        return response;
    }

    private String getUserDisplayName(User user) {
        if (user.getFirstName() != null && user.getLastName() != null) {
            return user.getFirstName() + " " + user.getLastName();
        } else if (user.getFirstName() != null) {
            return user.getFirstName();
        } else if (user.getUsername() != null) {
            return user.getUsername();
        } else if (user.getEmail() != null) {
            return user.getEmail().split("@")[0];
        } else {
            return "User " + user.getId().toString().substring(0, 8);
        }
    }

    private String extractCallIdFromMeetingLink(String meetingLink) {
        String[] parts = meetingLink.split("/");
        return parts[parts.length - 1];
    }

    @Transactional
    public void leaveMeeting(Long meetingId, UUID userId) {
        MeetingParticipant participant = participantRepository
                .findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        participant.setLeftAt(LocalDateTime.now());
        participantRepository.save(participant);

        notifyAllParticipants(meetingId, "participant_left", userId);
    }

    @Transactional
    public void endMeeting(Long meetingId, UUID userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!meeting.getHostId().equals(userId)) {
            throw new UnauthorizedException("Only host can end meeting");
        }

        meeting.setStatus(MeetingStatus.COMPLETED);
        meeting.setEndedAt(LocalDateTime.now());
        meetingRepository.save(meeting);

        List<MeetingParticipant> activeParticipants =
                participantRepository.findActiveParticipants(meetingId);
        activeParticipants.forEach(p -> {
            p.setLeftAt(LocalDateTime.now());
            participantRepository.save(p);
        });

        notifyAllParticipants(meetingId, "meeting_ended", userId);
    }

    @Transactional
    public void admitFromWaitingRoom(Long meetingId, UUID participantUserId, UUID hostId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!meeting.getHostId().equals(hostId)) {
            throw new UnauthorizedException("Only host can admit participants");
        }

        MeetingParticipant participant = participantRepository
                .findByMeetingIdAndUserId(meetingId, participantUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        participant.setIsInWaitingRoom(false);
        participantRepository.save(participant);

        webSocketMessageService.sendAdmissionNotification(participantUserId, meetingId);
        notifyAllParticipants(meetingId, "participant_joined", participantUserId);
    }

    public List<MeetingParticipant> getActiveParticipants(Long meetingId) {
        return participantRepository.findActiveParticipants(meetingId);
    }

    public List<MeetingParticipant> getWaitingRoomParticipants(Long meetingId) {
        return participantRepository.findByMeetingIdAndIsInWaitingRoomTrue(meetingId);
    }

    public Meeting getMeetingById(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
    }

    public Meeting getMeetingByLink(String meetingLink) {
        return meetingRepository.findByMeetingLink(meetingLink)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
    }

    public List<Meeting> getUserMeetings(UUID userId) {
        return meetingRepository.findByHostIdOrderByCreatedAtDesc(userId);
    }

    private void notifyAllParticipants(Long meetingId, String event, UUID userId) {
        List<MeetingParticipant> participants = participantRepository.findActiveParticipants(meetingId);
        participants.forEach(p -> {
            if (!p.getUserId().equals(userId)) {
                webSocketMessageService.sendMeetingEvent(p.getUserId(), meetingId, event, userId);
            }
        });
    }
}