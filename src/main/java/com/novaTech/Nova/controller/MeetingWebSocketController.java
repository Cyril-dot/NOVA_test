package com.novaTech.Nova.controller;

import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.ZoomServiceImpl.MeetingWebSocketService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MeetingWebSocketController {

    private final MeetingWebSocketService meetingWebSocketService;

    @MessageMapping("/meeting/{meetingId}/join")
    public void joinMeeting(
            @DestinationVariable Long meetingId,
            @Payload JoinMeetingMessage message,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor) {

        UUID userId = getUserIdFromPrincipal(principal);
        log.info("User {} joining meeting {} via WebSocket", userId, meetingId);

        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("meetingId", meetingId);
            headerAccessor.getSessionAttributes().put("userId", userId);
        }

        meetingWebSocketService.handleUserJoin(meetingId, userId, message.getUserName());
    }

    @MessageMapping("/meeting/{meetingId}/leave")
    public void leaveMeeting(@DestinationVariable Long meetingId, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        log.info("User {} leaving meeting {}", userId, meetingId);
        meetingWebSocketService.handleUserLeave(meetingId, userId);
    }

    @MessageMapping("/meeting/{meetingId}/audio")
    public void toggleAudio(@DestinationVariable Long meetingId, @Payload AudioStateMessage message, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        meetingWebSocketService.handleAudioToggle(meetingId, userId, message.isEnabled());
    }

    @MessageMapping("/meeting/{meetingId}/video")
    public void toggleVideo(@DestinationVariable Long meetingId, @Payload VideoStateMessage message, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        meetingWebSocketService.handleVideoToggle(meetingId, userId, message.isEnabled());
    }

    @MessageMapping("/meeting/{meetingId}/screen")
    public void toggleScreenShare(@DestinationVariable Long meetingId, @Payload ScreenShareMessage message, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        meetingWebSocketService.handleScreenShareToggle(meetingId, userId, message.isSharing());
    }

    @MessageMapping("/meeting/{meetingId}/reaction")
    public void sendReaction(@DestinationVariable Long meetingId, @Payload ReactionMessage message, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        meetingWebSocketService.handleReaction(meetingId, userId, message.getEmoji());
    }

    @MessageMapping("/meeting/{meetingId}/hand")
    public void toggleHand(@DestinationVariable Long meetingId, @Payload HandRaiseMessage message, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        meetingWebSocketService.handleHandRaise(meetingId, userId, message.isRaised());
    }

    @MessageMapping("/meeting/{meetingId}/chat")
    public void sendChat(@DestinationVariable Long meetingId, @Payload ChatMessage message, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        meetingWebSocketService.handleChat(meetingId, userId, message.getContent(),
                message.getIsPrivate(), message.getPrivateToUserId());
    }

    @MessageMapping("/meeting/{meetingId}/signal")
    public void handleSignal(@DestinationVariable Long meetingId, @Payload WebRTCSignalMessage message, Principal principal) {
        UUID userId = getUserIdFromPrincipal(principal);
        meetingWebSocketService.handleWebRTCSignal(meetingId, userId,
                message.getTargetUserId(), message.getSignalData());
    }

    private UUID getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("User not authenticated");
        }

        // Check if it's a UserPrincipal (from REST API authentication)
        if (principal instanceof Authentication) {
            Authentication auth = (Authentication) principal;
            if (auth.getPrincipal() instanceof UserPrincipal) {
                return ((UserPrincipal) auth.getPrincipal()).getUserId();
            }
        }

        // For WebSocket authentication, the principal name is the userId string
        try {
            // Try to parse as Long first (userId from token), then convert to UUID
            String principalName = principal.getName();

            // If it's a valid UUID string, parse it directly
            if (principalName.contains("-")) {
                return UUID.fromString(principalName);
            }

            // Otherwise, it might be a Long userId - you'll need to look it up
            // For now, throw an exception for debugging
            throw new RuntimeException("Invalid principal format: " + principalName +
                    ". Expected UUID format. Please ensure User.id is UUID type.");

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to parse userId from principal: " + principal.getName(), e);
        }
    }

    // Message DTOs
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinMeetingMessage {
        private String userName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioStateMessage {
        private boolean enabled;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoStateMessage {
        private boolean enabled;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScreenShareMessage {
        private boolean sharing;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactionMessage {
        private String emoji;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HandRaiseMessage {
        private boolean raised;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private String content;
        private Boolean isPrivate;
        private UUID privateToUserId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebRTCSignalMessage {
        private UUID targetUserId;
        private Map<String, Object> signalData;
    }
}