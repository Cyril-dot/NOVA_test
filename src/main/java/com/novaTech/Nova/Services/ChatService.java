package com.novaTech.Nova.Services;

import com.novaTech.Nova.DTO.ChatHistoryResponse;
import com.novaTech.Nova.DTO.ChatRequest;
import com.novaTech.Nova.DTO.ChatResponse;
import com.novaTech.Nova.DTO.MessageResponse;
import com.novaTech.Nova.Entities.AI.Chat;
import com.novaTech.Nova.Entities.AI.Message;
import com.novaTech.Nova.Entities.AI.Repo.ChatRepository;
import com.novaTech.Nova.Entities.AI.Repo.MessageRepository;
import com.novaTech.Nova.Entities.Enums.Model;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Services.AI.CerebrasService;
import com.novaTech.Nova.Services.AI.LLMService;
import com.novaTech.Nova.Services.AI.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final LLMService llmService;
    private final CerebrasService cerebrasService;
    private final SearchService searchService;

    /**
     * Process a chat message - creates chat if needed, saves messages, gets AI response
     */
    @Transactional
    public Mono<ChatResponse> processMessage(ChatRequest request, User user) {
        log.info("Processing message for user: {}", user.getId());

        // 1. Get or create chat
        Chat chat = getOrCreateChat(request.getChatId(), user, request.getModel());

        // 2. Save user message
        Message userMessage = saveMessage(chat, request.getMessage(), Message.MessageRole.USER);

        // 3. Build context from chat history
        String prompt = buildPromptWithContext(chat);

        // 4. Get AI response based on selected model
        if (request.getModel() == Model.LLM) {
            return llmService.generateText(prompt)
                    .flatMap(aiResponse -> {
                        // 5. Save AI response
                        Message assistantMessage = saveMessage(chat, aiResponse, Message.MessageRole.ASSISTANT);

                        // 6. Update chat title if it's the first message
                        if (chat.getMessages().size() == 2) {
                            updateChatTitle(chat, request.getMessage());
                        }

                        // 7. Build response
                        return Mono.just(ChatResponse.builder()
                                .chatId(chat.getId())
                                .title(chat.getTitle())
                                .response(aiResponse)
                                .messageId(assistantMessage.getId())
                                .timestamp(assistantMessage.getCreatedAt())
                                .messageCount(chat.getMessages().size())
                                .build());
                    });

        } else if (request.getModel() == Model.CEREBRAS) {
            return Mono.fromSupplier(() -> {
                // Generate AI response using CerebrasService
                String aiResponse = cerebrasService.generateText(prompt);

                // Save AI response
                Message assistantMessage = saveMessage(chat, aiResponse, Message.MessageRole.ASSISTANT);

                // Update chat title if it's the first message
                if (chat.getMessages().size() == 2) {
                    updateChatTitle(chat, request.getMessage());
                }

                // Build and return ChatResponse
                return ChatResponse.builder()
                        .chatId(chat.getId())
                        .title(chat.getTitle())
                        .response(aiResponse)
                        .messageId(assistantMessage.getId())
                        .timestamp(assistantMessage.getCreatedAt())
                        .messageCount(chat.getMessages().size())
                        .build();
            });

        } else if (request.getModel() == Model.SEARCH) {
            // NEW: Handle SEARCH model - calls generateSearch()
            return Mono.fromSupplier(() -> {
                // Generate search results using SearchService
                String searchResults = searchService.generateSearch(request.getMessage());

                // Save search results as assistant message
                Message assistantMessage = saveMessage(chat, searchResults, Message.MessageRole.ASSISTANT);

                // Update chat title if it's the first message
                if (chat.getMessages().size() == 2) {
                    updateChatTitle(chat, request.getMessage());
                }

                // Build and return ChatResponse
                return ChatResponse.builder()
                        .chatId(chat.getId())
                        .title(chat.getTitle())
                        .response(searchResults)
                        .messageId(assistantMessage.getId())
                        .timestamp(assistantMessage.getCreatedAt())
                        .messageCount(chat.getMessages().size())
                        .build();
            });

        } else {
            // Default to LLM
            return llmService.generateText(prompt)
                    .flatMap(aiResponse -> {
                        Message assistantMessage = saveMessage(chat, aiResponse, Message.MessageRole.ASSISTANT);

                        if (chat.getMessages().size() == 2) {
                            updateChatTitle(chat, request.getMessage());
                        }

                        return Mono.just(ChatResponse.builder()
                                .chatId(chat.getId())
                                .title(chat.getTitle())
                                .response(aiResponse)
                                .messageId(assistantMessage.getId())
                                .timestamp(assistantMessage.getCreatedAt())
                                .messageCount(chat.getMessages().size())
                                .build());
                    });
        }
    }

    /**
     * Process a chat message with STREAMING
     */
    @Transactional
    public Flux<String> processMessageStream(ChatRequest request, User user) {
        log.info("Processing STREAMING message for user: {}", user.getId());

        // 1. Get or create chat
        Chat chat = getOrCreateChat(request.getChatId(), user, request.getModel());

        // 2. Save user message
        Message userMessage = saveMessage(chat, request.getMessage(), Message.MessageRole.USER);

        // 3. Build context
        String prompt = buildPromptWithContext(chat);

        // 4. Stream AI response and collect it
        StringBuilder fullResponse = new StringBuilder();

        if (request.getModel() == Model.LLM) {
            return llmService.generateTextStream(prompt)
                    .doOnNext(chunk -> {
                        fullResponse.append(chunk);
                        log.debug("Streaming chunk: {}", chunk);
                    })
                    .doOnComplete(() -> {
                        // Save complete assistant message after streaming is done
                        saveMessage(chat, fullResponse.toString(), Message.MessageRole.ASSISTANT);

                        // Update title if first message
                        if (chat.getMessages().size() == 2) {
                            updateChatTitle(chat, request.getMessage());
                        }

                        log.info("Streaming complete. Total response length: {}", fullResponse.length());
                    });

        } else if (request.getModel() == Model.CEREBRAS) {
            StringBuilder fullResponse1 = new StringBuilder();

            return cerebrasService.generateTextStream(prompt)
                    .doOnNext(chunk -> {
                        fullResponse1.append(chunk);
                        log.debug("Streaming chunk: {}", chunk);
                    })
                    .doOnComplete(() -> {
                        // Save complete assistant message after streaming is done
                        saveMessage(chat, fullResponse1.toString(), Message.MessageRole.ASSISTANT);

                        // Update chat title if first message
                        if (chat.getMessages().size() == 2) {
                            updateChatTitle(chat, request.getMessage());
                        }

                        log.info("Streaming complete. Total response length: {}", fullResponse1.length());
                    });

        } else if (request.getModel() == Model.SEARCH) {
            // NEW: Handle SEARCH model streaming - calls generateSearchStream()
            StringBuilder searchResponse = new StringBuilder();

            return searchService.generateSearchStream(request.getMessage())
                    .doOnNext(chunk -> {
                        searchResponse.append(chunk);
                        log.debug("Streaming search chunk: {}", chunk);
                    })
                    .doOnComplete(() -> {
                        // Save complete search results after streaming is done
                        saveMessage(chat, searchResponse.toString(), Message.MessageRole.ASSISTANT);

                        // Update chat title if first message
                        if (chat.getMessages().size() == 2) {
                            updateChatTitle(chat, request.getMessage());
                        }

                        log.info("Search streaming complete. Total response length: {}", searchResponse.length());
                    });

        } else {
            // Default to LLM
            return llmService.generateTextStream(prompt)
                    .doOnNext(chunk -> {
                        fullResponse.append(chunk);
                        log.debug("Streaming chunk: {}", chunk);
                    })
                    .doOnComplete(() -> {
                        saveMessage(chat, fullResponse.toString(), Message.MessageRole.ASSISTANT);

                        if (chat.getMessages().size() == 2) {
                            updateChatTitle(chat, request.getMessage());
                        }

                        log.info("Streaming complete. Total response length: {}", fullResponse.length());
                    });
        }
    }

    /**
     * Get or create a chat for the user
     */
    private Chat getOrCreateChat(Long chatId, User user, Model model) {
        if (chatId != null) {
            // Try to find existing chat
            return chatRepository.findByIdAndUserAndIsActiveTrue(chatId, user)
                    .orElseThrow(() -> new RuntimeException("Chat not found or not accessible"));
        }

        // Try to get most recent active chat
        return chatRepository.findFirstByUserAndIsActiveTrueOrderByUpdatedAtDesc(user)
                .orElseGet(() -> createNewChat(user, model));
    }

    /**
     * Create a new chat for the user
     */
    @Transactional
    public Chat createNewChat(User user, Model model) {
        log.info("Creating new chat for user: {}", user.getId());

        Chat chat = Chat.builder()
                .user(user)
                .title("New Chat")
                .isActive(true)
                .model(model)
                .build();

        return chatRepository.save(chat);
    }

    /**
     * Save a message to the database
     */
    private Message saveMessage(Chat chat, String content, Message.MessageRole role) {
        Message message = Message.builder()
                .chat(chat)
                .content(content)
                .role(role)
                .build();

        Message saved = messageRepository.save(message);
        chatRepository.save(chat);

        log.info("Saved {} message to chat {}", role, chat.getId());
        return saved;
    }

    /**
     * Build prompt with conversation context (last 10 messages)
     */
    private String buildPromptWithContext(Chat chat) {
        List<Message> recentMessages = messageRepository
                .findTop10ByChatOrderByCreatedAtDesc(chat);

        if (recentMessages.isEmpty()) {
            return "";
        }

        // Reverse to get chronological order
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            Message msg = recentMessages.get(i);
            contextBuilder.append(msg.getRole().name())
                    .append(": ")
                    .append(msg.getContent())
                    .append("\n");
        }

        return contextBuilder.toString();
    }

    /**
     * Update chat title based on first message
     */
    private void updateChatTitle(Chat chat, String firstMessage) {
        String title = firstMessage.length() > 50
                ? firstMessage.substring(0, 47) + "..."
                : firstMessage;
        chat.setTitle(title);
        chatRepository.save(chat);
    }

    /**
     * Get all chats for a user
     */
    public List<ChatHistoryResponse> getUserChats(User user) {
        List<Chat> chats = chatRepository.findByUserAndIsActiveTrueOrderByUpdatedAtDesc(user);

        return chats.stream()
                .map(chat -> ChatHistoryResponse.builder()
                        .chatId(chat.getId())
                        .title(chat.getTitle())
                        .createdAt(chat.getCreatedAt())
                        .updatedAt(chat.getUpdatedAt())
                        .messageCount(chat.getMessages().size())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get specific chat with full message history
     */
    public ChatHistoryResponse getChatHistory(Long chatId, User user) {
        Chat chat = chatRepository.findByIdAndUserWithMessages(chatId, user)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        List<MessageResponse> messages = chat.getMessages().stream()
                .map(MessageResponse::fromEntity)
                .collect(Collectors.toList());

        return ChatHistoryResponse.builder()
                .chatId(chat.getId())
                .title(chat.getTitle())
                .createdAt(chat.getCreatedAt())
                .updatedAt(chat.getUpdatedAt())
                .messageCount(messages.size())
                .messages(messages)
                .build();
    }

    /**
     * Delete a chat (soft delete)
     */
    @Transactional
    public void deleteChat(Long chatId, User user) {
        Chat chat = chatRepository.findByIdAndUserAndIsActiveTrue(chatId, user)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        chat.setActive(false);
        chatRepository.save(chat);
        log.info("Deleted chat {} for user {}", chatId, user.getId());
    }

    /**
     * Clear all messages from a chat
     */
    @Transactional
    public void clearChat(Long chatId, User user) {
        Chat chat = chatRepository.findByIdAndUserAndIsActiveTrue(chatId, user)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        messageRepository.deleteAll(chat.getMessages());
        chat.getMessages().clear();
        chat.setTitle("New Chat");
        chatRepository.save(chat);
        log.info("Cleared all messages from chat {}", chatId);
    }

    /**
     * Get all chat history for a user with message details
     */
    public List<ChatHistoryResponse> getUserChatHistory(User user) {
        log.info("Fetching chat history for user: {}", user.getId());

        List<Chat> chats = chatRepository.findByUserAndIsActiveTrueOrderByUpdatedAtDesc(user);

        if (chats.isEmpty()) {
            log.info("No chat history found for user: {}", user.getId());
            return List.of();
        }

        return chats.stream()
                .map(chat -> {
                    List<MessageResponse> messages = chat.getMessages().stream()
                            .map(MessageResponse::fromEntity)
                            .collect(Collectors.toList());

                    return ChatHistoryResponse.builder()
                            .chatId(chat.getId())
                            .title(chat.getTitle())
                            .createdAt(chat.getCreatedAt())
                            .updatedAt(chat.getUpdatedAt())
                            .messageCount(messages.size())
                            .messages(messages)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Delete all chat history for a user (soft delete all chats)
     */
    @Transactional
    public void clearAllUserChatHistory(User user) {
        log.info("Clearing all chat history for user: {}", user.getId());

        List<Chat> userChats = chatRepository.findByUserAndIsActiveTrueOrderByUpdatedAtDesc(user);

        if (userChats.isEmpty()) {
            log.info("No active chats found for user: {}", user.getId());
            return;
        }

        // Soft delete all user's chats
        userChats.forEach(chat -> {
            chat.setActive(false);
            log.debug("Deactivating chat: {}", chat.getId());
        });

        chatRepository.saveAll(userChats);
        log.info("Successfully cleared {} chats for user: {}", userChats.size(), user.getId());
    }
}