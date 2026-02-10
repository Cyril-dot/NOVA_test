package com.novaTech.Nova.controller;

import com.novaTech.Nova.DTO.TeamMessageRequest;
import com.novaTech.Nova.Entities.chats.TeamMessage;
import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.MessagingChatService.TeamChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams/{teamId}/chat")
@RequiredArgsConstructor
@Slf4j
public class TeamChatController {

    private final TeamChatService chatService;

    private UserPrincipal userPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("No authentication found in SecurityContext");
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            log.error("Invalid principal type: {}",
                    principal != null ? principal.getClass().getName() : "null");
            throw new RuntimeException("Invalid authentication principal");
        }

        return (UserPrincipal) principal;
    }

    // -------------------------
    // Messaging
    // -------------------------

    @PostMapping("/messages")
    public ResponseEntity<TeamMessage> sendMessage(
            @PathVariable UUID teamId,
            @RequestBody TeamMessageRequest request) {

        UUID senderId = userPrincipal().getUserId();

        TeamMessage message = chatService.sendMessage(
                teamId,
                senderId,
                request.getContent(),
                request.getMentionedUserIds(),
                request.getMentionTeam(),
                request.getMentionAdmins(),
                request.getReplyToMessageId(),
                request.getFileUrl()
        );

        return ResponseEntity.ok(message);
    }

    @GetMapping("/messages")
    public ResponseEntity<List<TeamMessage>> getMessages(
            @PathVariable UUID teamId) {

        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(chatService.getTeamMessages(teamId, userId));
    }

    @GetMapping("/mentions")
    public ResponseEntity<List<TeamMessage>> getMentions(
            @PathVariable UUID teamId) {

        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(chatService.getUserMentions(teamId, userId));
    }

    // -------------------------
    // Threads
    // -------------------------

    @GetMapping("/messages/{messageId}/thread")
    public ResponseEntity<List<TeamMessage>> getThread(
            @PathVariable Long messageId) {
        return ResponseEntity.ok(chatService.getThreadReplies(messageId));
    }

    // -------------------------
    // Message Editing
    // -------------------------

    @PatchMapping("/messages/{messageId}")
    public ResponseEntity<TeamMessage> editMessage(
            @PathVariable Long messageId,
            @RequestBody String newContent) {

        UUID userId = userPrincipal().getUserId();
        return ResponseEntity.ok(chatService.editMessage(messageId, userId, newContent));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId) {

        UUID userId = userPrincipal().getUserId();
        chatService.deleteMessage(messageId, userId);
        return ResponseEntity.ok().build();
    }
}
