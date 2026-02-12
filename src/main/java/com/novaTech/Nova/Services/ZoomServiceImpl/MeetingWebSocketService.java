package com.novaTech.Nova.Services.ZoomServiceImpl;

import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.meeting.MeetingParticipant;
import com.novaTech.Nova.Entities.repo.MeetingParticipantRepository;
import com.novaTech.Nova.Entities.repo.UserRepo;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final MeetingParticipantRepository participantRepository;
    private final MeetingChatService chatService;
    private final UserRepo userRepository;

    public void handleUserJoin(Long meetingId, UUID userId, String userName) {
        List<MeetingParticipant> participants = participantRepository.findActiveParticipants(meetingId);

        User user = userRepository.findById(userId).orElse(null);
        String displayName = user != null ? getUserDisplayName(user) : userName;

        ParticipantJoinedEvent event = ParticipantJoinedEvent.builder()
                .userId(userId)
                .userName(displayName)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToMeeting(meetingId, "/participant/joined", event);
        sendToUser(userId, "/meeting/participants", buildParticipantsList(participants));

        log.info("Broadcasted user join event for user {} in meeting {}", userId, meetingId);
    }

    public void handleUserLeave(Long meetingId, UUID userId) {
        ParticipantLeftEvent event = ParticipantLeftEvent.builder()
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToMeeting(meetingId, "/participant/left", event);
        log.info("Broadcasted user leave event for user {} in meeting {}", userId, meetingId);
    }

    public void handleAudioToggle(Long meetingId, UUID userId, boolean enabled) {
        AudioStateEvent event = AudioStateEvent.builder()
                .userId(userId)
                .enabled(enabled)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToMeeting(meetingId, "/participant/audio", event);
    }

    public void handleVideoToggle(Long meetingId, UUID userId, boolean enabled) {
        VideoStateEvent event = VideoStateEvent.builder()
                .userId(userId)
                .enabled(enabled)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToMeeting(meetingId, "/participant/video", event);
    }

    public void handleScreenShareToggle(Long meetingId, UUID userId, boolean sharing) {
        ScreenShareEvent event = ScreenShareEvent.builder()
                .userId(userId)
                .sharing(sharing)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToMeeting(meetingId, "/participant/screen", event);
    }

    public void handleReaction(Long meetingId, UUID userId, String emoji) {
        ReactionEvent event = ReactionEvent.builder()
                .userId(userId)
                .emoji(emoji)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToMeeting(meetingId, "/meeting/reaction", event);
    }

    public void handleHandRaise(Long meetingId, UUID userId, boolean raised) {
        HandRaiseEvent event = HandRaiseEvent.builder()
                .userId(userId)
                .raised(raised)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToMeeting(meetingId, "/participant/hand", event);
    }

    public void handleChat(Long meetingId, UUID userId, String content,
                           Boolean isPrivate, UUID privateToUserId) {
        try {
            var savedMessage = chatService.sendMessage(meetingId, userId, content,
                    isPrivate, privateToUserId, null);

            User user = userRepository.findById(userId).orElse(null);
            String displayName = user != null ? getUserDisplayName(user) : "Unknown";

            ChatMessageEvent event = ChatMessageEvent.builder()
                    .messageId(savedMessage.getId())
                    .userId(userId)
                    .userName(displayName)
                    .content(content)
                    .isPrivate(isPrivate != null ? isPrivate : false)
                    .privateToUserId(privateToUserId)
                    .timestamp(savedMessage.getSentAt())
                    .build();

            if (isPrivate != null && isPrivate && privateToUserId != null) {
                sendToUser(privateToUserId, "/meeting/chat", event);
                sendToUser(userId, "/meeting/chat", event);
            } else {
                broadcastToMeeting(meetingId, "/meeting/chat", event);
            }
        } catch (Exception e) {
            log.error("Error handling chat message: ", e);
        }
    }

    public void handleWebRTCSignal(Long meetingId, UUID fromUserId,
                                   UUID toUserId, Map<String, Object> signalData) {
        WebRTCSignalEvent event = WebRTCSignalEvent.builder()
                .fromUserId(fromUserId)
                .signalData(signalData)
                .timestamp(LocalDateTime.now())
                .build();

        sendToUser(toUserId, "/meeting/signal", event);
    }

    private void broadcastToMeeting(Long meetingId, String destination, Object payload) {
        String topic = String.format("/topic/meeting/%d%s", meetingId, destination);
        messagingTemplate.convertAndSend(topic, payload);
    }

    private void sendToUser(UUID userId, String destination, Object payload) {
        String userDestination = String.format("/queue%s", destination);
        messagingTemplate.convertAndSendToUser(userId.toString(), userDestination, payload);
    }

    private List<ParticipantInfo> buildParticipantsList(List<MeetingParticipant> participants) {
        List<ParticipantInfo> list = new ArrayList<>();

        for (MeetingParticipant p : participants) {
            User user = userRepository.findById(p.getUserId()).orElse(null);
            String displayName = user != null ? getUserDisplayName(user) : "Unknown";

            list.add(ParticipantInfo.builder()
                    .userId(p.getUserId())
                    .userName(displayName)
                    .isMuted(p.getIsMuted() != null ? p.getIsMuted() : true)
                    .isVideoOn(p.getIsVideoOn() != null ? p.getIsVideoOn() : false)
                    .isHandRaised(p.getIsHandRaised() != null ? p.getIsHandRaised() : false)
                    .build());
        }

        return list;
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

    // Event DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantJoinedEvent {
        private UUID userId;
        private String userName;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantLeftEvent {
        private UUID userId;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioStateEvent {
        private UUID userId;
        private boolean enabled;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoStateEvent {
        private UUID userId;
        private boolean enabled;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScreenShareEvent {
        private UUID userId;
        private boolean sharing;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactionEvent {
        private UUID userId;
        private String emoji;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HandRaiseEvent {
        private UUID userId;
        private boolean raised;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageEvent {
        private Long messageId;
        private UUID userId;
        private String userName;
        private String content;
        private boolean isPrivate;
        private UUID privateToUserId;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebRTCSignalEvent {
        private UUID fromUserId;
        private Map<String, Object> signalData;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantInfo {
        private UUID userId;
        private String userName;
        private boolean isMuted;
        private boolean isVideoOn;
        private boolean isHandRaised;
    }
}