package com.novaTech.Nova.Services.MessagingChatService;

import com.novaTech.Nova.Entities.TeamMember;
import com.novaTech.Nova.Entities.chats.TeamChatRoom;
import com.novaTech.Nova.Entities.chats.TeamMessage;
import com.novaTech.Nova.Entities.repo.TeamChatRoomRepository;
import com.novaTech.Nova.Entities.repo.TeamMemberRepository;
import com.novaTech.Nova.Entities.repo.TeamMessageRepository;
import com.novaTech.Nova.Exceptions.ResourceNotFoundException;
import com.novaTech.Nova.Exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamChatService {
    
    private final TeamMessageRepository messageRepository;
    private final TeamChatRoomRepository chatRoomRepository;
    private final TeamMemberRepository memberRepository;
    private final WebSocketMessageService webSocketMessageService;
    
    @Transactional
    public TeamMessage sendMessage(UUID teamId, UUID senderId, String content,
                                   List<Long> mentionedUserIds, Boolean mentionTeam,
                                   Boolean mentionAdmins, Long replyToMessageId, String fileUrl) {
        log.info("Sending team message to team {} from user {}", teamId, senderId);
        
        // Verify team membership
        if (!isMember(teamId, senderId)) {
            throw new UnauthorizedException("Not a team member");
        }
        
        // Get or create chat room
        TeamChatRoom chatRoom = getOrCreateTeamChatRoom(teamId);
        
        // Create message
        TeamMessage message = TeamMessage.builder()
                .teamId(teamId)
                .chatRoomId(chatRoom.getId())
                .senderId(senderId)
                .content(content)
                .mentionedUserIds(mentionedUserIds)
                .mentionTeam(mentionTeam)
                .mentionAdmins(mentionAdmins)
                .replyToMessageId(replyToMessageId)
                .fileUrl(fileUrl)
                .build();
        
        TeamMessage saved = messageRepository.save(message);
        
        // Update chat room timestamp
        chatRoom.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);
        
        // Broadcast to all team members via WebSocket
        List<TeamMember> members = memberRepository.findByTeamIdAndIsActiveTrue(teamId);
        members.forEach(member -> {
            if (!member.getUser().getId().equals(senderId)) {
                webSocketMessageService.sendTeamMessage(member.getUser().getId(), saved);
            }
        });
        
        log.info("Team message sent successfully: {}", saved.getId());
        return saved;
    }
    
    public List<TeamMessage> getTeamMessages(UUID teamId, UUID userId) {
        if (!isMember(teamId, userId)) {
            throw new UnauthorizedException("Not a team member");
        }
        
        return messageRepository.findByTeamIdAndIsDeletedFalseOrderBySentAtAsc(teamId);
    }
    
    public List<TeamMessage> getUserMentions(UUID teamId, UUID userId) {
        return messageRepository.findMentionsForUser(teamId, userId);
    }
    
    public List<TeamMessage> getThreadReplies(Long messageId) {
        return messageRepository.findThreadReplies(messageId);
    }
    
    @Transactional
    public TeamMessage editMessage(Long messageId, UUID userId, String newContent) {
        TeamMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        
        if (!message.getSenderId().equals(userId)) {
            throw new UnauthorizedException("Not authorized");
        }
        
        message.setContent(newContent);
        message.setEditedAt(LocalDateTime.now());
        TeamMessage updated = messageRepository.save(message);
        
        // Broadcast update to team
        broadcastToTeam(message.getTeamId(), updated, "update");
        
        return updated;
    }
    
    @Transactional
    public void deleteMessage(Long messageId, UUID userId) {
        TeamMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        
        if (!message.getSenderId().equals(userId)) {
            throw new UnauthorizedException("Not authorized");
        }
        
        message.setIsDeleted(true);
        messageRepository.save(message);
        
        // Broadcast deletion to team
        broadcastToTeam(message.getTeamId(), message, "delete");
    }
    
    private boolean isMember(UUID teamId, UUID userId) {
        return memberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(teamId, userId);
    }
    
    private TeamChatRoom getOrCreateTeamChatRoom(UUID teamId) {
        return chatRoomRepository.findByTeamIdAndIsActiveTrue(teamId)
                .orElseGet(() -> {
                    TeamChatRoom chatRoom = TeamChatRoom.builder()
                            .teamId(teamId)
                            .build();
                    return chatRoomRepository.save(chatRoom);
                });
    }
    
    private void broadcastToTeam(UUID teamId, TeamMessage message, String action) {
        List<TeamMember> members = memberRepository.findByTeamIdAndIsActiveTrue(teamId);
        members.forEach(member -> {
            webSocketMessageService.sendTeamMessageUpdate(member.getUser().getId(), message, action);
        });
    }
}