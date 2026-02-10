package com.novaTech.Nova.Services.MessagingChatService;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    
    // Private Chat Messages
    public void sendPrivateMessage(UUID userId, Object message) {
        String destination = "/user/" + userId + "/queue/messages";
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Sent private message to user {}", userId);
    }
    
    public void sendReadReceipt(UUID userId, Long messageId) {
        Map<String, Object> payload = Map.of(
            "type", "read_receipt",
            "messageId", messageId
        );
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/notifications", payload);
    }
    
    public void sendMessageUpdate(UUID userId, Object message) {
        Map<String, Object> payload = Map.of(
            "type", "message_update",
            "message", message
        );
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/messages", payload);
    }
    
    public void sendMessageDeletion(UUID userId, Long messageId) {
        Map<String, Object> payload = Map.of(
            "type", "message_delete",
            "messageId", messageId
        );
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/messages", payload);
    }
    
    // Typing Indicators
    public void sendTypingIndicator(UUID userId, Long typingUserId, boolean isTyping) {
        Map<String, Object> payload = Map.of(
            "type", "typing",
            "userId", typingUserId,
            "isTyping", isTyping
        );
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/presence", payload);
    }
    
    // Friend Requests
    public void sendFriendRequest(UUID userId, Object friendship) {
        Map<String, Object> payload = Map.of(
            "type", "friend_request",
            "friendship", friendship
        );
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/notifications", payload);
    }
    
    public void sendFriendAccepted(UUID userId, Object friendship) {
        Map<String, Object> payload = Map.of(
            "type", "friend_accepted",
            "friendship", friendship
        );
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/notifications", payload);
    }
    
    // Team Messages
    public void sendTeamMessage(UUID userId, Object message) {
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/team-messages", message);
    }
    
    public void sendTeamMessageUpdate(UUID userId, Object message, String action) {
        Map<String, Object> payload = Map.of(
            "type", "team_message_" + action,
            "message", message
        );
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/team-messages", payload);
    }
    
    // Meeting Events
    public void sendMeetingInvitation(UUID userId, Object meeting) {
        Map<String, Object> payload = Map.of(
            "type", "meeting_invitation",
            "meeting", meeting
        );
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/notifications", payload);
    }
    
    public void sendWaitingRoomNotification(UUID hostId, UUID waitingUserId) {
        Map<String, Object> payload = Map.of(
            "type", "waiting_room",
            "userId", waitingUserId
        );
        messagingTemplate.convertAndSend("/user/" + hostId + "/queue/meeting-events", payload);
    }
    
    public void sendAdmissionNotification(UUID userId, Long meetingId) {
        Map<String, Object> payload = Map.of(
            "type", "admitted",
            "meetingId", meetingId
        );
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/meeting-events", payload);
    }
    
    public void sendMeetingEvent(UUID userId, Long meetingId, String event, UUID eventUserId) {
        Map<String, Object> payload = Map.of(
            "type", event,
            "meetingId", meetingId,
            "userId", eventUserId
        );
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/meeting-events", payload);
    }
    
    public void sendMeetingMessage(UUID userId, Object message) {
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/meeting-chat", message);
    }
    
    public void sendPrivateMeetingMessage(UUID userId, Object message) {
        Map<String, Object> payload = Map.of(
            "type", "private_message",
            "message", message
        );
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/meeting-chat", payload);
    }
    
    // Presence Updates
    public void sendPresenceUpdate(UUID userId, String status) {
        Map<String, Object> payload = Map.of(
            "type", "presence_update",
            "userId", userId,
            "status", status
        );
        messagingTemplate.convertAndSend("/topic/presence", payload);
    }
}