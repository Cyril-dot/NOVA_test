package com.novaTech.Nova.Services;

import com.novaTech.Nova.DTO.ChatHistoryResponse;
import com.novaTech.Nova.DTO.ChatResponse;
import com.novaTech.Nova.DTO.ExternalChatRequest;
import com.novaTech.Nova.DTO.MessageResponse;
import com.novaTech.Nova.Entities.AI.Chat;
import com.novaTech.Nova.Entities.AI.Message;
import com.novaTech.Nova.Entities.AI.Repo.ChatRepository;
import com.novaTech.Nova.Entities.AI.Repo.MessageRepository;
import com.novaTech.Nova.Entities.Enums.Model;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Services.AI.ExternalApiService;
import com.novaTech.Nova.Services.AI.UpdatedExternalApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExternalChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UpdatedExternalApiService externalApiService;

    /**
     * Process external API chat message (non-streaming)
     * FOR MULTI-PARAM EXTERNAL APIs: NETFLIX, SPOTIFY, WORKOUT, NUTRITION, EXERCISE, WEATHER, etc.
     */
    @Transactional
    public Mono<ChatResponse> processExternalMessage(ExternalChatRequest request, User user) {
        log.info("Processing EXTERNAL message for user: {} with model: {}", user.getId(), request.getModel());

        // 1. Get or create chat
        Chat chat = getOrCreateChat(request.getChatId(), user, request.getModel());

        // 2. Save user message
        Message userMessage = saveMessage(chat, request.getMessage(), Message.MessageRole.USER);

        // 3. Get external API response based on model
        return Mono.fromSupplier(() -> {
            String apiResponse = callExternalApi(request);
            return buildChatResponseSync(chat, apiResponse, request.getMessage());
        });
    }

    /**
     * Process external API chat message with STREAMING
     * FOR MULTI-PARAM EXTERNAL APIs: NETFLIX, SPOTIFY, WORKOUT, NUTRITION, EXERCISE, WEATHER, etc.
     */
    @Transactional
    public Flux<String> processExternalMessageStream(ExternalChatRequest request, User user) {
        log.info("Processing EXTERNAL STREAMING message for user: {} with model: {}", user.getId(), request.getModel());

        // 1. Get or create chat
        Chat chat = getOrCreateChat(request.getChatId(), user, request.getModel());

        // 2. Save user message
        Message userMessage = saveMessage(chat, request.getMessage(), Message.MessageRole.USER);

        // 3. Stream external API response
        StringBuilder fullResponse = new StringBuilder();

        Flux<String> stream = callExternalApiStream(request);

        return stream
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

    // ==================== EXTERNAL API ROUTING ====================

    private String callExternalApi(ExternalChatRequest request) {
        Map<String, Object> params = request.getParams();

        return switch (request.getModel()) {
            case NETFLIX -> externalApiService.generateNetflixSearch(
                    request.getMessage(),
                    getIntParam(params, "offset", 0),
                    getIntParam(params, "limitTitles", 20),
                    getIntParam(params, "limitSuggestions", 20),
                    getStringParam(params, "lang", "en")
            );

            case SPOTIFY -> externalApiService.generateSpotifySearch(
                    request.getMessage(),
                    getIntParam(params, "offset", 0),
                    getIntParam(params, "limit", 10),
                    getIntParam(params, "numberOfTopResults", 5)
            );

            case WORKOUT -> externalApiService.generateWorkoutPlan(
                    getStringParam(params, "goal", "weight_loss"),
                    getStringParam(params, "fitnessLevel", "beginner"),
                    getArrayParam(params, "preferences"),
                    getArrayParam(params, "healthConditions"),
                    getIntParam(params, "daysPerWeek", 3),
                    getIntParam(params, "sessionDuration", 60),
                    getIntParam(params, "planDurationWeeks", 4),
                    getStringParam(params, "lang", "en")
            );

            case NUTRITION -> externalApiService.generateNutritionAdvice(
                    getStringParam(params, "goal", "weight_loss"),
                    getArrayParam(params, "dietaryRestrictions"),
                    getDoubleParam(params, "currentWeight", 70.0),
                    getDoubleParam(params, "targetWeight", 65.0),
                    getStringParam(params, "dailyActivityLevel", "moderate"),
                    getStringParam(params, "lang", "en")
            );

            case EXERCISE -> externalApiService.generateExerciseDetails(
                    request.getMessage(),
                    getStringParam(params, "lang", "en")
            );

            case SONG_RECOGNITION -> externalApiService.generateSongRecognition(request.getMessage());

            case COUNTRY_WEATHER -> externalApiService.generateCountryWeatherInfo(request.getMessage());

            case CITY_WEATHER -> externalApiService.generateCurrentWeatherByCity(
                    request.getMessage(),
                    getStringParam(params, "lang", "EN")
            );

            default -> "Unsupported external API model: " + request.getModel();
        };
    }

    private Flux<String> callExternalApiStream(ExternalChatRequest request) {
        Map<String, Object> params = request.getParams();

        return switch (request.getModel()) {
            case NETFLIX -> externalApiService.generateNetflixSearchStream(
                    request.getMessage(),
                    getIntParam(params, "offset", 0),
                    getIntParam(params, "limitTitles", 20),
                    getIntParam(params, "limitSuggestions", 20),
                    getStringParam(params, "lang", "en")
            );

            case SPOTIFY -> externalApiService.generateSpotifySearchStream(
                    request.getMessage(),
                    getIntParam(params, "offset", 0),
                    getIntParam(params, "limit", 10),
                    getIntParam(params, "numberOfTopResults", 5)
            );

            case WORKOUT -> externalApiService.generateWorkoutPlanStream(
                    getStringParam(params, "goal", "weight_loss"),
                    getStringParam(params, "fitnessLevel", "beginner"),
                    getArrayParam(params, "preferences"),
                    getArrayParam(params, "healthConditions"),
                    getIntParam(params, "daysPerWeek", 3),
                    getIntParam(params, "sessionDuration", 60),
                    getIntParam(params, "planDurationWeeks", 4),
                    getStringParam(params, "lang", "en")
            );

            case NUTRITION -> externalApiService.generateNutritionAdviceStream(
                    getStringParam(params, "goal", "weight_loss"),
                    getArrayParam(params, "dietaryRestrictions"),
                    getDoubleParam(params, "currentWeight", 70.0),
                    getDoubleParam(params, "targetWeight", 65.0),
                    getStringParam(params, "dailyActivityLevel", "moderate"),
                    getStringParam(params, "lang", "en")
            );

            case EXERCISE -> externalApiService.generateExerciseDetailsStream(
                    request.getMessage(),
                    getStringParam(params, "lang", "en")
            );

            case SONG_RECOGNITION -> externalApiService.generateSongRecognitionStream(request.getMessage());

            case COUNTRY_WEATHER -> externalApiService.generateCountryWeatherInfoStream(request.getMessage());

            case CITY_WEATHER -> externalApiService.generateCurrentWeatherByCityStream(
                    request.getMessage(),
                    getStringParam(params, "lang", "EN")
            );

            default -> Flux.just("Unsupported external API model: " + request.getModel());
        };
    }

    // ==================== PARAMETER HELPERS ====================

    private String getStringParam(Map<String, Object> params, String key, String defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        return params.get(key).toString();
    }

    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof String) return Integer.parseInt((String) value);
        return defaultValue;
    }

    private double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        if (value instanceof Double) return (Double) value;
        if (value instanceof String) return Double.parseDouble((String) value);
        return defaultValue;
    }

    private String[] getArrayParam(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key)) return new String[]{};
        Object value = params.get(key);
        if (value instanceof String[]) return (String[]) value;
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream().map(Object::toString).toArray(String[]::new);
        }
        return new String[]{};
    }

    // ==================== CHAT MANAGEMENT (SAME AS ChatService) ====================

    private ChatResponse buildChatResponseSync(Chat chat, String apiResponse, String firstMessage) {
        Message assistantMessage = saveMessage(chat, apiResponse, Message.MessageRole.ASSISTANT);

        if (chat.getMessages().size() == 2) {
            updateChatTitle(chat, firstMessage);
        }

        return ChatResponse.builder()
                .chatId(chat.getId())
                .title(chat.getTitle())
                .response(apiResponse)
                .messageId(assistantMessage.getId())
                .timestamp(assistantMessage.getCreatedAt())
                .messageCount(chat.getMessages().size())
                .build();
    }

    private Chat getOrCreateChat(Long chatId, User user, Model model) {
        if (chatId != null) {
            return chatRepository.findByIdAndUserAndIsActiveTrue(chatId, user)
                    .orElseThrow(() -> new RuntimeException("Chat not found or not accessible"));
        }

        return chatRepository.findFirstByUserAndIsActiveTrueOrderByUpdatedAtDesc(user)
                .orElseGet(() -> createNewChat(user, model));
    }

    @Transactional
    public Chat createNewChat(User user, Model model) {
        log.info("Creating new EXTERNAL chat for user: {}", user.getId());

        Chat chat = Chat.builder()
                .user(user)
                .title("New Chat")
                .isActive(true)
                .model(model)
                .build();

        return chatRepository.save(chat);
    }

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

    private void updateChatTitle(Chat chat, String firstMessage) {
        String title = firstMessage.length() > 50
                ? firstMessage.substring(0, 47) + "..."
                : firstMessage;
        chat.setTitle(title);
        chatRepository.save(chat);
    }

    public List<ChatHistoryResponse> getUserExternalChats(User user) {
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

    public ChatHistoryResponse getExternalChatHistory(Long chatId, User user) {
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

    @Transactional
    public void deleteExternalChat(Long chatId, User user) {
        Chat chat = chatRepository.findByIdAndUserAndIsActiveTrue(chatId, user)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        chat.setActive(false);
        chatRepository.save(chat);
        log.info("Deleted external chat {} for user {}", chatId, user.getId());
    }

    @Transactional
    public void clearExternalChat(Long chatId, User user) {
        Chat chat = chatRepository.findByIdAndUserAndIsActiveTrue(chatId, user)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        messageRepository.deleteAll(chat.getMessages());
        chat.getMessages().clear();
        chat.setTitle("New Chat");
        chatRepository.save(chat);
        log.info("Cleared all messages from external chat {}", chatId);
    }
}