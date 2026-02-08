package com.novaTech.Nova.controller;

import com.novaTech.Nova.DTO.ChatHistoryResponse;
import com.novaTech.Nova.DTO.ChatRequest;
import com.novaTech.Nova.DTO.ChatResponse;
import com.novaTech.Nova.Entities.Enums.Model;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Services.ChatService;
import com.novaTech.Nova.Services.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat management endpoints")
public class ChatController {

    private final ChatService chatService;
    private final UserRegistrationService userService;

    @PostConstruct
    public void printTestUrls() {
        log.info("=".repeat(80));
        log.info("💬 CHAT CONTROLLER - Ready on Port 8080");
        log.info("=".repeat(80));
        log.info("POST /api/v1/chat/message - Send message (requires JWT)");
        log.info("POST /api/v1/chat/message/stream - Send message streaming (requires JWT)");
        log.info("GET  /api/v1/chat/list - Get all chats (requires JWT)");
        log.info("GET  /api/v1/chat/{id} - Get chat history (requires JWT)");
        log.info("=".repeat(80) + "\n");
    }

    private User extractUser(String authHeader) {
        return userService.getUserFromToken(authHeader);
    }

    /**
     * Send a message (non-streaming) - BLOCKING VERSION
     */
    @PostMapping("/message")
    @Operation(summary = "Send a chat message and get AI response")
    public ResponseEntity<ChatResponse> sendMessage(
            @RequestBody ChatRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

        User user = extractUser(authHeader);

        log.info("💬 [CHAT] User: {} | Message: '{}'",
                user.getEmail(),
                request.getMessage().substring(0, Math.min(30, request.getMessage().length())));

        ChatResponse chatResponse = chatService.processMessage(request, user)
                .doOnError(err -> log.error("❌ [CHAT] Error: {}", err.getMessage()))
                .block();  // Convert Mono to blocking

        log.info("✅ [CHAT] Response: {} chars", chatResponse.getResponse().length());
        return ResponseEntity.ok(chatResponse);
    }

    /**
     * Send a message with STREAMING response
     */
    @PostMapping(value = "/message/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Send a chat message and get streaming AI response")
    public Flux<String> sendMessageStream(
            @RequestBody ChatRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

        User user = extractUser(authHeader);

        log.info("🌊 [CHAT-STREAM] User: {} | Model: {}", user.getEmail(), request.getModel());

        return chatService.processMessageStream(request, user)
                .doOnComplete(() -> log.info("✅ [STREAM] Complete"))
                .doOnError(err -> log.error("❌ [STREAM] Error: {}", err.getMessage()));
    }

    @GetMapping("/list")
    @Operation(summary = "Get all chats for the current user")
    public ResponseEntity<List<ChatHistoryResponse>> getUserChats(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

        User user = extractUser(authHeader);
        List<ChatHistoryResponse> chats = chatService.getUserChats(user);

        log.info("📋 [CHAT] Found {} chats for {}", chats.size(), user.getEmail());
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/{chatId}")
    @Operation(summary = "Get full chat history by chat ID")
    public ResponseEntity<ChatHistoryResponse> getChatHistory(
            @PathVariable Long chatId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

        User user = extractUser(authHeader);
        ChatHistoryResponse history = chatService.getChatHistory(chatId, user);

        return ResponseEntity.ok(history);
    }

    @PostMapping("/new")
    @Operation(summary = "Create a new chat")
    public ResponseEntity<ChatHistoryResponse> createNewChat(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestParam Model model) {

        User user = extractUser(authHeader);
        var chat = chatService.createNewChat(user, model);

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
    @Operation(summary = "Delete a chat")
    public ResponseEntity<Void> deleteChat(
            @PathVariable Long chatId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

        User user = extractUser(authHeader);
        chatService.deleteChat(chatId, user);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{chatId}/clear")
    @Operation(summary = "Clear all messages from a chat")
    public ResponseEntity<Void> clearChat(
            @PathVariable Long chatId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

        User user = extractUser(authHeader);
        chatService.clearChat(chatId, user);

        return ResponseEntity.noContent().build();
    }
}