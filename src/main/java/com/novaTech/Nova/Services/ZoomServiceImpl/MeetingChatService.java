package com.novaTech.Nova.Services.ZoomServiceImpl;

import com.novaTech.Nova.Entities.meeting.*;
import com.novaTech.Nova.Entities.repo.*;
import com.novaTech.Nova.Exceptions.ResourceNotFoundException;
import com.novaTech.Nova.Exceptions.UnauthorizedException;
import com.novaTech.Nova.Services.MessagingChatService.WebSocketMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingChatService {

    private final MeetingMessageRepository messageRepository;
    private final MeetingParticipantRepository participantRepository;
    private final WebSocketMessageService webSocketMessageService;

    @Transactional
    public MeetingMessage sendMessage(Long meetingId, UUID senderId, String content,
                                      Boolean isPrivate, UUID privateToUserId, String fileUrl) {
        log.info("Sending meeting message in meeting {} from user {}", meetingId, senderId);

        // Verify participant
        participantRepository.findByMeetingIdAndUserId(meetingId, senderId)
                .orElseThrow(() -> new UnauthorizedException("Not a meeting participant"));

        MeetingMessage message = MeetingMessage.builder()
                .meetingId(meetingId)
                .senderId(senderId)
                .content(content)
                .isPrivate(isPrivate != null ? isPrivate : false)
                .privateToUserId(privateToUserId)
                .fileUrl(fileUrl)
                .build();

        MeetingMessage saved = messageRepository.save(message);

        // Send via WebSocket
        if (isPrivate != null && isPrivate && privateToUserId != null) {
            webSocketMessageService.sendPrivateMeetingMessage(privateToUserId, saved);
        } else {
            // Broadcast to all participants
            List<MeetingParticipant> participants =
                    participantRepository.findActiveParticipants(meetingId);
            participants.forEach(p -> {
                if (!p.getUserId().equals(senderId)) {
                    webSocketMessageService.sendMeetingMessage(p.getUserId(), saved);
                }
            });
        }

        return saved;
    }

    public List<MeetingMessage> getPublicMessages(Long meetingId, UUID userId) {
        // Verify participant
        participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new UnauthorizedException("Not a meeting participant"));

        return messageRepository.findPublicMessages(meetingId);
    }

    public List<MeetingMessage> getPrivateMessages(Long meetingId, UUID userId) {
        // Verify participant
        participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new UnauthorizedException("Not a meeting participant"));

        return messageRepository.findPrivateMessagesForUser(meetingId, userId);
    }

    public List<MeetingMessage> getAllMessages(Long meetingId) {
        return messageRepository.findByMeetingIdOrderBySentAtAsc(meetingId);
    }

    @Transactional
    public void deleteMessage(Long messageId, UUID userId) {
        MeetingMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        // Only sender can delete their message
        if (!message.getSenderId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own messages");
        }

        messageRepository.delete(message);
        log.info("Message {} deleted by user {}", messageId, userId);
    }
}