package com.novaTech.Nova.controller;

import com.novaTech.Nova.DTO.SendMessageRequest;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.chats.Friendship;
import com.novaTech.Nova.Entities.chats.PrivateMessage;
import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.MessagingChatService.FriendshipService;
import com.novaTech.Nova.Services.MessagingChatService.PrivateChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class PrivateChatController {

    private final PrivateChatService chatService;
    private final FriendshipService friendshipService;

    private UserPrincipal userPrincipal(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("No authentication found in SecurityContext");
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            log.error("Invalid principal type: {}", principal != null ? principal.getClass().getName() : "null");
            throw new RuntimeException("Invalid authentication principal");
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;
        log.debug("Successfully retrieved UserPrincipal for user: {} (ID: {})",
                userPrincipal.getEmail(), userPrincipal.getUserId());

        return userPrincipal;
    }

    // ===== MESSAGING - NEW USERNAME/EMAIL ENDPOINTS =====

    /**
     * Send message using username or email
     * POST /api/chat/messages/to/{usernameOrEmail}
     */
    @PostMapping("/messages/to/{usernameOrEmail}")
    public ResponseEntity<PrivateMessage> sendMessageByIdentifier(
            @PathVariable String usernameOrEmail,
            @RequestBody SendMessageRequest request) {

        UUID senderId = userPrincipal().getUserId();

        PrivateMessage message = chatService.sendMessageByIdentifier(
                senderId, usernameOrEmail, request.getContent(),
                request.getReplyToMessageId(), request.getFileUrl(), request.getFileType()
        );
        return ResponseEntity.ok(message);
    }

    /**
     * Get conversation with user by username or email
     * GET /api/chat/conversations/with/{usernameOrEmail}
     */
    @GetMapping("/conversations/with/{usernameOrEmail}")
    public ResponseEntity<List<PrivateMessage>> getConversationByIdentifier(
            @PathVariable String usernameOrEmail) {

        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(chatService.getConversationByIdentifier(userId, usernameOrEmail));
    }

    /**
     * Search users by username or email
     * GET /api/chat/users/search?q=searchTerm
     */
    @GetMapping("/users/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String q) {
        return ResponseEntity.ok(chatService.searchUsers(q));
    }

    /**
     * Get user details by username or email
     * GET /api/chat/users/{usernameOrEmail}
     */
    @GetMapping("/users/{usernameOrEmail}")
    public ResponseEntity<User> getUserByIdentifier(@PathVariable String usernameOrEmail) {
        return ResponseEntity.ok(chatService.getUserByIdentifier(usernameOrEmail));
    }

    /**
     * Search messages in conversation by username/email
     * GET /api/chat/search/with/{usernameOrEmail}?q=query
     */
    @GetMapping("/search/with/{usernameOrEmail}")
    public ResponseEntity<List<PrivateMessage>> searchMessagesByIdentifier(
            @PathVariable String usernameOrEmail,
            @RequestParam String q) {
        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(chatService.searchMessagesByIdentifier(userId, usernameOrEmail, q));
    }

    // ===== MESSAGING - ORIGINAL UUID ENDPOINTS (BACKWARD COMPATIBLE) =====

    /**
     * Send message using UUID (original method)
     * POST /api/chat/messages?receiverId=uuid
     */
    @PostMapping("/messages")
    public ResponseEntity<PrivateMessage> sendMessage(
            @RequestBody SendMessageRequest request,
            @RequestParam UUID receiverId) {

        UUID senderId = userPrincipal().getUserId();

        PrivateMessage message = chatService.sendMessage(
                senderId, receiverId, request.getContent(),
                request.getReplyToMessageId(), request.getFileUrl(), request.getFileType()
        );
        return ResponseEntity.ok(message);
    }

    /**
     * Get conversation by UUID (original method)
     * GET /api/chat/conversations/{friendId}
     */
    @GetMapping("/conversations/{friendId}")
    public ResponseEntity<List<PrivateMessage>> getConversation(
            @PathVariable UUID friendId) {

        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(chatService.getConversation(userId, friendId));
    }

    @PutMapping("/messages/{messageId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long messageId) {
        UUID userId = userPrincipal().getUserId();
        chatService.markAsRead(messageId, userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/messages/{messageId}")
    public ResponseEntity<PrivateMessage> editMessage(
            @PathVariable Long messageId,
            @RequestBody String newContent) {
        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(chatService.editMessage(messageId, userId, newContent));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        UUID userId = userPrincipal().getUserId();
        chatService.deleteMessage(messageId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Search messages by UUID (original method)
     * GET /api/chat/search?friendId=uuid&query=text
     */
    @GetMapping("/search")
    public ResponseEntity<List<PrivateMessage>> searchMessages(
            @RequestParam UUID friendId,
            @RequestParam String query) {
        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(chatService.searchMessages(userId, friendId, query));
    }

    // ===== FRIEND MANAGEMENT - NEW USERNAME/EMAIL ENDPOINTS =====

    /**
     * Send friend request by username or email
     * POST /api/chat/friends/request/to/{usernameOrEmail}
     */
    @PostMapping("/friends/request/to/{usernameOrEmail}")
    public ResponseEntity<Friendship> sendFriendRequestByIdentifier(
            @PathVariable String usernameOrEmail) {
        UUID requesterId = userPrincipal().getUserId();
        return ResponseEntity.ok(friendshipService.sendFriendRequestByIdentifier(requesterId, usernameOrEmail));
    }

    /**
     * Remove friend by username or email
     * DELETE /api/chat/friends/{usernameOrEmail}
     */
    @DeleteMapping("/friends/{usernameOrEmail}")
    public ResponseEntity<Void> removeFriendByIdentifier(@PathVariable String usernameOrEmail) {
        UUID userId = userPrincipal().getUserId();
        friendshipService.removeFriendByIdentifier(userId, usernameOrEmail);
        return ResponseEntity.ok().build();
    }

    /**
     * Check if users are friends
     * GET /api/chat/friends/check/{usernameOrEmail}
     */
    @GetMapping("/friends/check/{usernameOrEmail}")
    public ResponseEntity<Boolean> checkFriendship(@PathVariable String usernameOrEmail) {
        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(friendshipService.areFriendsByIdentifier(userId, usernameOrEmail));
    }

    /**
     * Get friends with details (username, email)
     * GET /api/chat/friends/details
     */
    @GetMapping("/friends/details")
    public ResponseEntity<List<FriendshipService.FriendshipDTO>> getFriendsWithDetails() {
        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(friendshipService.getFriendsWithDetails(userId));
    }

    /**
     * Get pending requests with requester details
     * GET /api/chat/friends/pending/details
     */
    @GetMapping("/friends/pending/details")
    public ResponseEntity<List<FriendshipService.FriendRequestDTO>> getPendingRequestsWithDetails() {
        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(friendshipService.getPendingRequestsWithDetails(userId));
    }

    /**
     * Get sent requests with addressee details
     * GET /api/chat/friends/sent/details
     */
    @GetMapping("/friends/sent/details")
    public ResponseEntity<List<FriendshipService.FriendRequestDTO>> getSentRequestsWithDetails() {
        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(friendshipService.getSentRequestsWithDetails(userId));
    }

    // ===== FRIEND MANAGEMENT - ORIGINAL UUID ENDPOINTS (BACKWARD COMPATIBLE) =====

    /**
     * Send friend request by UUID (original method)
     * POST /api/chat/friends/request?addresseeId=uuid
     */
    @PostMapping("/friends/request")
    public ResponseEntity<Friendship> sendFriendRequest(@RequestParam UUID addresseeId) {
        UUID requesterId = userPrincipal().getUserId();
        return ResponseEntity.ok(friendshipService.sendFriendRequest(requesterId, addresseeId));
    }

    @PutMapping("/friends/{friendshipId}/accept")
    public ResponseEntity<Friendship> acceptFriendRequest(@PathVariable Long friendshipId) {
        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(friendshipService.acceptFriendRequest(friendshipId, userId));
    }

    @PutMapping("/friends/{friendshipId}/reject")
    public ResponseEntity<Void> rejectFriendRequest(@PathVariable Long friendshipId) {
        UUID userId = userPrincipal().getUserId();
        friendshipService.rejectFriendRequest(friendshipId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/friends")
    public ResponseEntity<List<Friendship>> getFriends() {
        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(friendshipService.getFriends(userId));
    }

    @GetMapping("/friends/pending")
    public ResponseEntity<List<Friendship>> getPendingRequests() {
        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(friendshipService.getPendingRequests(userId));
    }
}