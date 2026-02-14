package com.novaTech.Nova.Services.ZoomServiceImpl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaTech.Nova.DTO.WebRTCSignalDTO;
import com.novaTech.Nova.Entities.meeting.MeetingParticipant;
import com.novaTech.Nova.Entities.repo.MeetingParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC Signaling Server
 * Handles WebSocket connections for WebRTC peer-to-peer signaling
 * 
 * This does NOT handle actual video/audio data - that flows directly between peers.
 * This server only helps peers:
 * 1. Discover each other
 * 2. Exchange connection metadata (SDP offers/answers)
 * 3. Exchange ICE candidates
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebRTCSignalingHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final MeetingParticipantRepository participantRepository;

    // Store active WebSocket sessions by meeting code
    // meetingCode -> Set of WebSocket sessions
    private final Map<String, Set<WebSocketSession>> meetingSessions = new ConcurrentHashMap<>();
    
    // Store session metadata
    // sessionId -> {peerId, meetingCode, participantId}
    private final Map<String, SessionMetadata> sessionMetadata = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("✅ WebSocket connection established: {}", session.getId());
        
        // Send welcome message
        WebRTCSignalDTO welcome = WebRTCSignalDTO.builder()
                .type(WebRTCSignalDTO.SignalType.JOIN)
                .data(Map.of("message", "Connected to signaling server", "sessionId", session.getId()))
                .build();
        
        sendToSession(session, welcome);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            WebRTCSignalDTO signal = objectMapper.readValue(message.getPayload(), WebRTCSignalDTO.class);
            log.info("📨 Received signal: {} from session: {}", signal.getType(), session.getId());

            switch (signal.getType()) {
                case JOIN:
                    handleJoin(session, signal);
                    break;
                    
                case LEAVE:
                    handleLeave(session, signal);
                    break;
                    
                case OFFER:
                case ANSWER:
                case ICE_CANDIDATE:
                    handleWebRTCSignal(session, signal);
                    break;
                    
                case TOGGLE_VIDEO:
                case TOGGLE_AUDIO:
                case SCREEN_SHARE_START:
                case SCREEN_SHARE_STOP:
                    handleMediaControl(session, signal);
                    break;
                    
                case CHAT_MESSAGE:
                    handleChatMessage(session, signal);
                    break;
                    
                case PARTICIPANT_LIST:
                    handleParticipantListRequest(session, signal);
                    break;
                    
                case KICK_PARTICIPANT:
                    handleKickParticipant(session, signal);
                    break;
                    
                default:
                    log.warn("Unknown signal type: {}", signal.getType());
            }
            
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendError(session, "Failed to process message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("❌ WebSocket connection closed: {} - Status: {}", session.getId(), status);
        
        SessionMetadata metadata = sessionMetadata.get(session.getId());
        if (metadata != null) {
            // Remove from meeting sessions
            Set<WebSocketSession> sessions = meetingSessions.get(metadata.meetingCode);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    meetingSessions.remove(metadata.meetingCode);
                }
            }
            
            // Update participant status in database
            updateParticipantOffline(session.getId());
            
            // Notify other participants
            broadcastToMeeting(metadata.meetingCode, WebRTCSignalDTO.builder()
                    .type(WebRTCSignalDTO.SignalType.LEAVE)
                    .fromPeerId(metadata.peerId)
                    .meetingCode(metadata.meetingCode)
                    .data(Map.of("peerId", metadata.peerId))
                    .build(), session.getId());
            
            sessionMetadata.remove(session.getId());
        }
    }

    // ========================
    // SIGNAL HANDLERS
    // ========================
    
    private void handleJoin(WebSocketSession session, WebRTCSignalDTO signal) throws Exception {
        String meetingCode = signal.getMeetingCode();
        String peerId = signal.getFromPeerId();
        
        if (meetingCode == null || peerId == null) {
            sendError(session, "Missing meetingCode or peerId");
            return;
        }
        
        log.info("👤 Peer {} joining meeting: {}", peerId, meetingCode);
        
        // Store session metadata
        SessionMetadata metadata = new SessionMetadata(session.getId(), peerId, meetingCode, null);
        sessionMetadata.put(session.getId(), metadata);
        
        // Add session to meeting group
        meetingSessions.computeIfAbsent(meetingCode, k -> ConcurrentHashMap.newKeySet())
                .add(session);
        
        // Update participant in database
        updateParticipantOnline(session.getId(), peerId);
        
        // Get list of existing peers in the meeting
        List<String> existingPeers = getExistingPeersInMeeting(meetingCode, session.getId());
        
        // Send existing peers to new joiner
        WebRTCSignalDTO peerListSignal = WebRTCSignalDTO.builder()
                .type(WebRTCSignalDTO.SignalType.PARTICIPANT_LIST)
                .meetingCode(meetingCode)
                .data(Map.of("peers", existingPeers))
                .build();
        
        sendToSession(session, peerListSignal);
        
        // Notify existing participants about new peer
        broadcastToMeeting(meetingCode, WebRTCSignalDTO.builder()
                .type(WebRTCSignalDTO.SignalType.JOIN)
                .fromPeerId(peerId)
                .meetingCode(meetingCode)
                .data(Map.of("peerId", peerId))
                .build(), session.getId());
        
        log.info("✅ Peer {} joined meeting {} successfully. Existing peers: {}", 
                peerId, meetingCode, existingPeers.size());
    }
    
    private void handleLeave(WebSocketSession session, WebRTCSignalDTO signal) throws Exception {
        SessionMetadata metadata = sessionMetadata.get(session.getId());
        if (metadata == null) {
            return;
        }
        
        log.info("👋 Peer {} leaving meeting: {}", metadata.peerId, metadata.meetingCode);
        
        // Notify other participants
        broadcastToMeeting(metadata.meetingCode, WebRTCSignalDTO.builder()
                .type(WebRTCSignalDTO.SignalType.LEAVE)
                .fromPeerId(metadata.peerId)
                .meetingCode(metadata.meetingCode)
                .data(Map.of("peerId", metadata.peerId))
                .build(), session.getId());
        
        // Clean up
        Set<WebSocketSession> sessions = meetingSessions.get(metadata.meetingCode);
        if (sessions != null) {
            sessions.remove(session);
        }
        
        updateParticipantOffline(session.getId());
        sessionMetadata.remove(session.getId());
    }
    
    private void handleWebRTCSignal(WebSocketSession session, WebRTCSignalDTO signal) throws Exception {
        // Forward SDP offer/answer or ICE candidate to target peer
        String targetPeerId = signal.getToPeerId();
        
        if (targetPeerId == null) {
            sendError(session, "Missing target peer ID");
            return;
        }
        
        SessionMetadata senderMetadata = sessionMetadata.get(session.getId());
        if (senderMetadata == null) {
            sendError(session, "You must join a meeting first");
            return;
        }
        
        signal.setFromPeerId(senderMetadata.peerId);
        signal.setMeetingCode(senderMetadata.meetingCode);
        
        log.info("🔀 Forwarding {} from {} to {}", signal.getType(), 
                senderMetadata.peerId, targetPeerId);
        
        // Find target session and forward
        boolean sent = sendToSpecificPeer(senderMetadata.meetingCode, targetPeerId, signal);
        
        if (!sent) {
            sendError(session, "Target peer not found: " + targetPeerId);
        }
    }
    
    private void handleMediaControl(WebSocketSession session, WebRTCSignalDTO signal) throws Exception {
        SessionMetadata metadata = sessionMetadata.get(session.getId());
        if (metadata == null) {
            sendError(session, "You must join a meeting first");
            return;
        }
        
        signal.setFromPeerId(metadata.peerId);
        signal.setMeetingCode(metadata.meetingCode);
        
        // Broadcast media control to all participants
        broadcastToMeeting(metadata.meetingCode, signal, session.getId());
        
        log.info("📹 Media control {} from peer {}", signal.getType(), metadata.peerId);
    }
    
    private void handleChatMessage(WebSocketSession session, WebRTCSignalDTO signal) throws Exception {
        SessionMetadata metadata = sessionMetadata.get(session.getId());
        if (metadata == null) {
            sendError(session, "You must join a meeting first");
            return;
        }
        
        signal.setFromPeerId(metadata.peerId);
        signal.setMeetingCode(metadata.meetingCode);
        
        // Broadcast chat message to all participants
        broadcastToMeeting(metadata.meetingCode, signal, null); // Include sender
        
        log.info("💬 Chat message from peer {}", metadata.peerId);
    }
    
    private void handleParticipantListRequest(WebSocketSession session, WebRTCSignalDTO signal) throws Exception {
        SessionMetadata metadata = sessionMetadata.get(session.getId());
        if (metadata == null) {
            sendError(session, "You must join a meeting first");
            return;
        }
        
        List<String> peers = getExistingPeersInMeeting(metadata.meetingCode, session.getId());
        
        WebRTCSignalDTO response = WebRTCSignalDTO.builder()
                .type(WebRTCSignalDTO.SignalType.PARTICIPANT_LIST)
                .meetingCode(metadata.meetingCode)
                .data(Map.of("peers", peers))
                .build();
        
        sendToSession(session, response);
    }
    
    private void handleKickParticipant(WebSocketSession session, WebRTCSignalDTO signal) throws Exception {
        // This would require moderator verification
        // Simplified version - just disconnect the target peer
        
        SessionMetadata metadata = sessionMetadata.get(session.getId());
        if (metadata == null) {
            sendError(session, "You must join a meeting first");
            return;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) signal.getData();
        String targetPeerId = (String) data.get("targetPeerId");
        
        if (targetPeerId == null) {
            sendError(session, "Missing target peer ID");
            return;
        }
        
        // Find and close target session
        WebSocketSession targetSession = findSessionByPeerId(metadata.meetingCode, targetPeerId);
        if (targetSession != null) {
            targetSession.close(CloseStatus.POLICY_VIOLATION);
            log.info("🚫 Peer {} kicked from meeting by {}", targetPeerId, metadata.peerId);
        }
    }

    // ========================
    // HELPER METHODS
    // ========================
    
    private void broadcastToMeeting(String meetingCode, WebRTCSignalDTO signal, String excludeSessionId) {
        Set<WebSocketSession> sessions = meetingSessions.get(meetingCode);
        if (sessions == null) {
            return;
        }
        
        sessions.forEach(session -> {
            if (excludeSessionId == null || !session.getId().equals(excludeSessionId)) {
                try {
                    sendToSession(session, signal);
                } catch (Exception e) {
                    log.error("Error broadcasting to session: {}", session.getId(), e);
                }
            }
        });
    }
    
    private boolean sendToSpecificPeer(String meetingCode, String targetPeerId, WebRTCSignalDTO signal) {
        WebSocketSession targetSession = findSessionByPeerId(meetingCode, targetPeerId);
        if (targetSession != null) {
            try {
                sendToSession(targetSession, signal);
                return true;
            } catch (Exception e) {
                log.error("Error sending to peer: {}", targetPeerId, e);
            }
        }
        return false;
    }
    
    private WebSocketSession findSessionByPeerId(String meetingCode, String peerId) {
        Set<WebSocketSession> sessions = meetingSessions.get(meetingCode);
        if (sessions == null) {
            return null;
        }
        
        return sessions.stream()
                .filter(s -> {
                    SessionMetadata meta = sessionMetadata.get(s.getId());
                    return meta != null && meta.peerId.equals(peerId);
                })
                .findFirst()
                .orElse(null);
    }
    
    private List<String> getExistingPeersInMeeting(String meetingCode, String excludeSessionId) {
        Set<WebSocketSession> sessions = meetingSessions.get(meetingCode);
        if (sessions == null) {
            return Collections.emptyList();
        }
        
        return sessions.stream()
                .filter(s -> !s.getId().equals(excludeSessionId))
                .map(s -> sessionMetadata.get(s.getId()))
                .filter(Objects::nonNull)
                .map(meta -> meta.peerId)
                .toList();
    }
    
    private void sendToSession(WebSocketSession session, WebRTCSignalDTO signal) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(signal);
            session.sendMessage(new TextMessage(json));
        }
    }
    
    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            WebRTCSignalDTO error = WebRTCSignalDTO.builder()
                    .type(WebRTCSignalDTO.SignalType.ERROR)
                    .data(Map.of("error", errorMessage))
                    .build();
            sendToSession(session, error);
        } catch (IOException e) {
            log.error("Error sending error message", e);
        }
    }
    
    private void updateParticipantOnline(String sessionId, String peerId) {
        try {
            Optional<MeetingParticipant> participant = participantRepository.findBySessionId(sessionId);
            if (participant.isEmpty()) {
                // Try to find by peerId if sessionId not set yet
                participant = participantRepository.findByPeerId(peerId);
            }
            
            if (participant.isPresent()) {
                MeetingParticipant p = participant.get();
                p.setSessionId(sessionId);
                p.setPeerId(peerId);
                p.setIsOnline(true);
                participantRepository.save(p);
                log.info("✅ Participant {} marked as online", p.getDisplayName());
            }
        } catch (Exception e) {
            log.error("Error updating participant online status", e);
        }
    }
    
    private void updateParticipantOffline(String sessionId) {
        try {
            Optional<MeetingParticipant> participant = participantRepository.findBySessionId(sessionId);
            if (participant.isPresent()) {
                MeetingParticipant p = participant.get();
                p.setIsOnline(false);
                participantRepository.save(p);
                log.info("👋 Participant {} marked as offline", p.getDisplayName());
            }
        } catch (Exception e) {
            log.error("Error updating participant offline status", e);
        }
    }

    // ========================
    // SESSION METADATA
    // ========================
    
    private static class SessionMetadata {
        String sessionId;
        String peerId;
        String meetingCode;
        UUID participantId;
        
        SessionMetadata(String sessionId, String peerId, String meetingCode, UUID participantId) {
            this.sessionId = sessionId;
            this.peerId = peerId;
            this.meetingCode = meetingCode;
            this.participantId = participantId;
        }
    }
}