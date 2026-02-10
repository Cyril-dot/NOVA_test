package com.novaTech.Nova.controller;

import com.novaTech.Nova.DTO.ChatHistoryResponse;
import com.novaTech.Nova.DTO.ChatResponse;
import com.novaTech.Nova.DTO.ExternalChatRequest;
import com.novaTech.Nova.Entities.Enums.Model;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.ExternalChatService;
import com.novaTech.Nova.Services.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/external-chat")
@RequiredArgsConstructor
@Tag(name = "External Chat", description = "Chat management for external APIs (Netflix, Spotify, Weather, etc.)")
public class ExternalChatController {

    private final ExternalChatService externalChatService;
    private final UserRegistrationService userService;

    @PostConstruct
    public void printTestUrls() {
        log.info("=".repeat(80));
        log.info("🌐 EXTERNAL CHAT CONTROLLER - Ready on Port 8080");
        log.info("=".repeat(80));
        log.info("POST /api/v1/external-chat/message - Send external API message (requires JWT)");
        log.info("POST /api/v1/external-chat/message/stream - Send external API message streaming (requires JWT)");
        log.info("GET  /api/v1/external-chat/list - Get all external chats (requires JWT)");
        log.info("GET  /api/v1/external-chat/{id} - Get external chat history (requires JWT)");
        log.info("=".repeat(80) + "\n");
    }

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

    /**
     * Send an external API message (non-streaming) - BLOCKING VERSION
     */
    @PostMapping("/message")
    @Operation(summary = "Send external API chat message and get response (Netflix, Spotify, Weather, etc.)")
    public ResponseEntity<ChatResponse> sendExternalMessage(
            @RequestBody ExternalChatRequest request) {

        UserPrincipal userPrincipal = userPrincipal();
        String username = userPrincipal.getEmail();

        User user = userService.findByEmail(username);
        if (user == null){
            log.error("Invalid username: {}", username);
            throw new RuntimeException("Invalid username");
        }

        log.info("🌐 [EXTERNAL-CHAT] User: {} | Model: {} | Message: '{}'",
                user.getEmail(),
                request.getModel(),
                request.getMessage().substring(0, Math.min(30, request.getMessage().length())));

        ChatResponse chatResponse = externalChatService.processExternalMessage(request, user)
                .doOnError(err -> log.error("❌ [EXTERNAL-CHAT] Error: {}", err.getMessage()))
                .block();

        log.info("✅ [EXTERNAL-CHAT] Response: {} chars", chatResponse.getResponse().length());
        return ResponseEntity.ok(chatResponse);
    }

    /**
     * Send an external API message with STREAMING response
     */
    @PostMapping(value = "/message/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Send external API chat message and get streaming response")
    public Flux<String> sendExternalMessageStream(
            @RequestBody ExternalChatRequest request) {

        UserPrincipal userPrincipal = userPrincipal();
        String username = userPrincipal.getEmail();

        User user = userService.findByEmail(username);
        if (user == null){
            log.error("Invalid username: {}", username);
            throw new RuntimeException("Invalid username");
        }

        log.info("🌊 [EXTERNAL-CHAT-STREAM] User: {} | Model: {}", user.getEmail(), request.getModel());

        return externalChatService.processExternalMessageStream(request, user)
                .doOnComplete(() -> log.info("✅ [EXTERNAL-STREAM] Complete"))
                .doOnError(err -> log.error("❌ [EXTERNAL-STREAM] Error: {}", err.getMessage()));
    }

    @GetMapping("/list")
    @Operation(summary = "Get all external API chats for the current user")
    public ResponseEntity<List<ChatHistoryResponse>> getUserExternalChats() {

        UserPrincipal userPrincipal = userPrincipal();
        String username = userPrincipal.getEmail();

        User user = userService.findByEmail(username);
        if (user == null){
            log.error("Invalid username: {}", username);
            throw new RuntimeException("Invalid username");
        }

        List<ChatHistoryResponse> chats = externalChatService.getUserExternalChats(user);

        log.info("📋 [EXTERNAL-CHAT] Found {} external chats for {}", chats.size(), user.getEmail());
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/{chatId}")
    @Operation(summary = "Get full external chat history by chat ID")
    public ResponseEntity<ChatHistoryResponse> getExternalChatHistory(
            @PathVariable Long chatId) {

        UserPrincipal userPrincipal = userPrincipal();
        String username = userPrincipal.getEmail();

        User user = userService.findByEmail(username);
        if (user == null){
            log.error("Invalid username: {}", username);
            throw new RuntimeException("Invalid username");
        }
        ChatHistoryResponse history = externalChatService.getExternalChatHistory(chatId, user);

        return ResponseEntity.ok(history);
    }

    @PostMapping("/new")
    @Operation(summary = "Create a new external chat")
    public ResponseEntity<ChatHistoryResponse> createNewExternalChat(
            @RequestParam Model model) {

        UserPrincipal  userPrincipal = userPrincipal();
        String username = userPrincipal.getEmail();

        User user = userService.findByEmail(username);
        if (user == null){
            log.error("Invalid username: {}", username);
            throw new RuntimeException("Invalid username");
        }
        var chat = externalChatService.createNewChat(user, model);

        ChatHistoryResponse response = ChatHistoryResponse.builder()
                .chatId(chat.getId())
                .title(chat.getTitle())
                .createdAt(chat.getCreatedAt())
                .updatedAt(chat.getUpdatedAt())
                .messageCount(0)
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{chatId}")
    @Operation(summary = "Delete an external chat")
    public ResponseEntity<Void> deleteExternalChat(
            @PathVariable Long chatId) {

        UserPrincipal userPrincipal = userPrincipal();
        String username = userPrincipal.getEmail();

        User user = userService.findByEmail(username);
        if (user == null){
            log.error("Invalid username: {}", username);
            throw new RuntimeException("Invalid username");
        }

        externalChatService.deleteExternalChat(chatId, user);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{chatId}/clear")
    @Operation(summary = "Clear all messages from an external chat")
    public ResponseEntity<Void> clearExternalChat(
            @PathVariable Long chatId) {

        UserPrincipal userPrincipal = userPrincipal();
        String username = userPrincipal.getEmail();

        User user = userService.findByEmail(username);
        if (user == null){
            log.error("Invalid username: {}", username);
            throw new RuntimeException("Invalid username");
        }
        externalChatService.clearExternalChat(chatId, user);

        return ResponseEntity.noContent().build();
    }


    // ========================
    // GENERAL CHAT SEARCH (searches in title and message content)
    // ========================
    @GetMapping("/search")
    public ResponseEntity<?> generalSearchChats(@RequestParam String keyword) {
        try {
            UserPrincipal principal = userPrincipal();
            User user = userService.getUserById(principal.getUserId());

            List<ChatHistoryResponse> chats = externalChatService.generalSearchOfChats(keyword, user);

            return ResponseEntity.ok(Map.of(
                    "message", "Search completed successfully",
                    "count", chats.size(),
                    "chats", chats
            ));

        } catch (RuntimeException e) {
            log.error("Error searching chats: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ========================
    // SEARCH CHATS BY TITLE ONLY
    // ========================
    @GetMapping("/search/title")
    public ResponseEntity<?> searchChatsByTitle(@RequestParam String title) {
        try {
            UserPrincipal principal = userPrincipal();
            User user = userService.getUserById(principal.getUserId());

            List<ChatHistoryResponse> chats = externalChatService.searchChatsByTitle(title, user);

            return ResponseEntity.ok(Map.of(
                    "message", "Search completed successfully",
                    "count", chats.size(),
                    "chats", chats
            ));

        } catch (RuntimeException e) {
            log.error("Error searching chats by title: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

}