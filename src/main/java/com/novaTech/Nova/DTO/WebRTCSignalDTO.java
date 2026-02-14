package com.novaTech.Nova.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * WebRTC Signaling Messages
 * These are exchanged between peers via WebSocket to establish P2P connection
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebRTCSignalDTO {
    private SignalType type;
    private String meetingCode;
    private String fromPeerId;
    private String toPeerId; // null means broadcast to all
    private Object data; // Can be SDP offer/answer or ICE candidate
    
    public enum SignalType {
        // Connection lifecycle
        JOIN,           // User joins meeting
        LEAVE,          // User leaves meeting
        
        // WebRTC signaling
        OFFER,          // SDP offer from initiator
        ANSWER,         // SDP answer from receiver
        ICE_CANDIDATE,  // ICE candidate exchange
        
        // Media controls
        TOGGLE_VIDEO,   // Toggle video on/off
        TOGGLE_AUDIO,   // Toggle audio on/off
        SCREEN_SHARE_START, // Start screen sharing
        SCREEN_SHARE_STOP,  // Stop screen sharing
        
        // Meeting controls
        PARTICIPANT_LIST, // Request/send participant list
        KICK_PARTICIPANT, // Remove participant (moderator only)
        
        // Chat
        CHAT_MESSAGE,   // Send chat message
        
        // Error
        ERROR           // Signal error
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SDPData {
    private String type; // "offer" or "answer"
    private String sdp;  // Session Description Protocol
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ICECandidateData {
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ChatMessageData {
    private String message;
    private String senderName;
    private Long timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class MediaToggleData {
    private String peerId;
    private Boolean enabled;
    private String mediaType; // "video" or "audio"
}