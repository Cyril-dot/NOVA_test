package com.novaTech.Nova.Services.MessagingChatService;

import com.novaTech.Nova.Entities.Enums.FriendshipStatus;
import com.novaTech.Nova.Entities.Enums.MessageStatus;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.chats.ChatRoom;
import com.novaTech.Nova.Entities.chats.PrivateMessage;
import com.novaTech.Nova.Entities.repo.ChatRoomRepository;
import com.novaTech.Nova.Entities.repo.FriendshipRepository;
import com.novaTech.Nova.Entities.repo.PrivateMessageRepository;
import com.novaTech.Nova.Entities.repo.UserRepo;
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
public class PrivateChatService {

    private final PrivateMessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepo userRepository;
    private final WebSocketMessageService webSocketMessageService;

    /**
     * Find user by username or email
     */
    private User findUserByUsernameOrEmail(String identifier) {
        return userRepository.findByUsername(identifier)
                .orElseGet(() -> userRepository.findByEmail(identifier)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User not found with username or email: " + identifier)));
    }

    /**
     * Send message using username or email
     */
    @Transactional
    public PrivateMessage sendMessageByIdentifier(UUID senderId, String receiverIdentifier,
                                                  String content, Long replyToMessageId,
                                                  String fileUrl, String fileType) {
        log.info("Sending message from {} to {}", senderId, receiverIdentifier);

        // Find receiver by username or email
        User receiver = findUserByUsernameOrEmail(receiverIdentifier);
        UUID receiverId = receiver.getId();

        return sendMessage(senderId, receiverId, content, replyToMessageId, fileUrl, fileType);
    }

    /**
     * Original send message method (can still be used internally)
     */
    @Transactional
    public PrivateMessage sendMessage(UUID senderId, UUID receiverId, String content,
                                      Long replyToMessageId, String fileUrl, String fileType) {
        log.info("Sending message from {} to {}", senderId, receiverId);

        // Verify friendship
        if (!areFriends(senderId, receiverId)) {
            throw new UnauthorizedException("Users are not friends");
        }

        // Get or create chat room
        ChatRoom chatRoom = getOrCreateChatRoom(senderId, receiverId);

        // Create message
        PrivateMessage message = PrivateMessage.builder()
                .chatRoomId(chatRoom.getId())
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .replyToMessageId(replyToMessageId)
                .fileUrl(fileUrl)
                .fileType(fileType)
                .build();

        PrivateMessage saved = messageRepository.save(message);

        // Update chat room timestamp
        chatRoom.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        // Send via WebSocket
        webSocketMessageService.sendPrivateMessage(receiverId, saved);

        log.info("Message sent successfully: {}", saved.getId());
        return saved;
    }

    /**
     * Get conversation by username or email
     */
    public List<PrivateMessage> getConversationByIdentifier(UUID userId, String friendIdentifier) {
        User friend = findUserByUsernameOrEmail(friendIdentifier);
        return getConversation(userId, friend.getId());
    }

    /**
     * Original get conversation method
     */
    public List<PrivateMessage> getConversation(UUID user1, UUID user2) {
        if (!areFriends(user1, user2)) {
            throw new UnauthorizedException("Users are not friends");
        }

        ChatRoom chatRoom = getOrCreateChatRoom(user1, user2);
        return messageRepository.findByChatRoomIdAndIsDeletedFalseOrderBySentAtAsc(chatRoom.getId());
    }

    /**
     * Search users by username or email (partial match)
     */
    public List<User> searchUsers(String searchTerm) {
        log.info("Searching users with term: {}", searchTerm);
        return userRepository.searchByUsernameOrEmail(searchTerm);
    }

    /**
     * Get user details by identifier
     */
    public User getUserByIdentifier(String identifier) {
        return findUserByUsernameOrEmail(identifier);
    }

    @Transactional
    public void markAsRead(Long messageId, UUID userId) {
        PrivateMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getReceiverId().equals(userId)) {
            throw new UnauthorizedException("Not authorized");
        }

        message.setStatus(MessageStatus.READ);
        message.setReadAt(LocalDateTime.now());
        messageRepository.save(message);

        // Notify sender via WebSocket
        webSocketMessageService.sendReadReceipt(message.getSenderId(), messageId);
    }

    @Transactional
    public PrivateMessage editMessage(Long messageId, UUID userId, String newContent) {
        PrivateMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new UnauthorizedException("Not authorized");
        }

        message.setContent(newContent);
        message.setEditedAt(LocalDateTime.now());
        PrivateMessage updated = messageRepository.save(message);

        // Notify receiver via WebSocket
        webSocketMessageService.sendMessageUpdate(message.getReceiverId(), updated);

        return updated;
    }

    @Transactional
    public void deleteMessage(Long messageId, UUID userId) {
        PrivateMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new UnauthorizedException("Not authorized");
        }

        message.setIsDeleted(true);
        messageRepository.save(message);

        // Notify receiver via WebSocket
        webSocketMessageService.sendMessageDeletion(message.getReceiverId(), messageId);
    }

    /**
     * Search messages in conversation by identifier
     */
    public List<PrivateMessage> searchMessagesByIdentifier(UUID userId, String friendIdentifier, String searchTerm) {
        User friend = findUserByUsernameOrEmail(friendIdentifier);
        return searchMessages(userId, friend.getId(), searchTerm);
    }

    /**
     * Original search messages method
     */
    public List<PrivateMessage> searchMessages(UUID user1, UUID user2, String searchTerm) {
        ChatRoom chatRoom = getOrCreateChatRoom(user1, user2);
        return messageRepository.searchInChatRoom(chatRoom.getId(), searchTerm);
    }

    private boolean areFriends(UUID user1, UUID user2) {
        return friendshipRepository.findByUsers(user1, user2)
                .map(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElse(false);
    }

    private ChatRoom getOrCreateChatRoom(UUID user1, UUID user2) {
        // Ensure consistent order of user IDs
        UUID userId1 = user1.compareTo(user2) < 0 ? user1 : user2;
        UUID userId2 = user1.compareTo(user2) < 0 ? user2 : user1;

        // Try to find an existing chat room
        return chatRoomRepository.findByUsers(userId1, userId2)
                .orElseGet(() -> {
                    // Create new chat room if not found
                    ChatRoom chatRoom = ChatRoom.builder()
                            .user1Id(userId1)
                            .user2Id(userId2)
                            .build();
                    return chatRoomRepository.save(chatRoom);
                });
    }
}