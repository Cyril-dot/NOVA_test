package com.novaTech.Nova.Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaTech.Nova.DTO.AiModelResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class HuggingFaceAiServiceImpl implements HuggingFaceAiService {

    @Value("${huggingface.api.key}")
    private String apiKey;

    @Value("${huggingface.models.summarization}")
    private String summarizationModel;

    @Value("${huggingface.models.qa}")
    private String qaModel;

    @Value("${huggingface.models.chat}")
    private String chatModel;

    @Value("${huggingface.api.router-url:https://router.huggingface.co}")
    private String hfRouterBase;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public HuggingFaceAiServiceImpl(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${huggingface.api.key}") String apiKey,
            @Value("${huggingface.api.router-url:https://router.huggingface.co}") String hfRouterBase) {
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(hfRouterBase)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public AiModelResponse summarize(String text) throws Exception {
        log.info("Summarizing text. Original length: {}", text != null ? text.length() : 0);

        String textToSummarize = truncateText(text, 800);
        log.debug("Text length after truncation: {}", textToSummarize.length());

        // Use OpenAI-compatible format for the router
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", "Summarize the following text concisely in 2-3 sentences:\n\n" + textToSummarize);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", summarizationModel);
        payload.put("messages", new Object[]{message});
        payload.put("max_tokens", 200);
        payload.put("temperature", 0.3);

        String endpoint = "/v1/chat/completions";

        try {
            String response = makeApiCall(endpoint, payload, 90);
            log.debug("Received response from summarization model: {}", response);

            JsonNode rootNode = objectMapper.readTree(response);
            String summary;

            if (rootNode.has("choices") && rootNode.get("choices").isArray() && rootNode.get("choices").size() > 0) {
                JsonNode firstChoice = rootNode.get("choices").get(0);
                summary = firstChoice.get("message").get("content").asText();
            } else {
                log.warn("Unexpected response format");
                summary = null;
            }

            if (summary == null || summary.trim().isEmpty()) {
                log.warn("Summarization returned empty result, using extractive summary");
                summary = createExtractiveSummary(text);
            }

            log.info("Summarization completed. Summary length: {}", summary.length());

            return AiModelResponse.builder()
                    .response(summary)
                    .modelUsed(summarizationModel)
                    .build();

        } catch (Exception e) {
            log.error("Summarization failed, falling back to extractive summary: {}", e.getMessage());
            String fallbackSummary = createExtractiveSummary(text);
            return AiModelResponse.builder()
                    .response(fallbackSummary + " (Auto-generated summary - AI model unavailable)")
                    .modelUsed("Extractive Fallback")
                    .build();
        }
    }

    @Override
    public AiModelResponse answerQuestion(String question, String context) throws Exception {
        log.info("Answering question: {}", question);

        String relevantContext = extractRelevantContext(question, context, 400);
        log.debug("Relevant context length: {}", relevantContext.length());

        // Use OpenAI-compatible format
        String prompt = String.format("Context: %s\n\nQuestion: %s\n\nAnswer:", relevantContext, question);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", qaModel);
        payload.put("messages", new Object[]{message});
        payload.put("max_tokens", 150);
        payload.put("temperature", 0.1);

        String endpoint = "/v1/chat/completions";
        String response = makeApiCall(endpoint, payload, 30);

        log.debug("Received response from QA model: {}", response);

        JsonNode rootNode = objectMapper.readTree(response);

        String answer;
        if (rootNode.has("choices") && rootNode.get("choices").isArray() && rootNode.get("choices").size() > 0) {
            JsonNode firstChoice = rootNode.get("choices").get(0);
            answer = firstChoice.get("message").get("content").asText();
        } else {
            log.warn("QA model couldn't find answer in context");
            answer = "No clear answer found in the document.";
        }

        log.info("QA completed. Answer: '{}'", answer);

        return AiModelResponse.builder()
                .response(answer)
                .confidenceScore(0.8)
                .modelUsed(qaModel)
                .build();
    }

    @Override
    public AiModelResponse chat(String prompt, String context) throws Exception {
        log.info("Processing chat request");
        log.debug("Prompt length: {}", prompt != null ? prompt.length() : 0);

        String truncatedContext = truncateText(context, 1500);
        log.debug("Context length after truncation: {}", truncatedContext.length());

        String userMessage = String.format(
                "Based on the following context, please answer the question.\n\nContext:\n%s\n\nQuestion: %s",
                truncatedContext,
                prompt
        );

        // ✅ FIXED: Use OpenAI-compatible format for HuggingFace Router
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", userMessage);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", chatModel);
        payload.put("messages", new Object[]{message});
        payload.put("max_tokens", 500);
        payload.put("temperature", 0.7);
        payload.put("stream", false);

        // ✅ FIXED: Use /v1/chat/completions endpoint
        String endpoint = "/v1/chat/completions";
        String response = makeApiCall(endpoint, payload, 45);

        log.debug("Received response from chat model");

        JsonNode rootNode = objectMapper.readTree(response);

        String chatResponse;
        if (rootNode.has("choices") && rootNode.get("choices").isArray() && rootNode.get("choices").size() > 0) {
            JsonNode firstChoice = rootNode.get("choices").get(0);
            chatResponse = firstChoice.get("message").get("content").asText();
        } else {
            log.error("Unexpected chat response format: {}", response);
            chatResponse = "I'm having trouble generating a response. Please try again.";
        }

        log.info("Chat completed. Response length: {}", chatResponse.length());

        return AiModelResponse.builder()
                .response(chatResponse)
                .modelUsed(chatModel)
                .build();
    }

    @Override
    public AiModelResponse multiFeatureAnalysis(String text, String additionalPrompt) throws Exception {
        log.info("Performing multi-feature analysis");

        String truncatedText = truncateText(text, 1500);
        log.debug("Text length after truncation: {}", truncatedText.length());

        String analysisPrompt = buildMultiFeaturePrompt(truncatedText, additionalPrompt);

        // ✅ FIXED: Use OpenAI-compatible format
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", analysisPrompt);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", chatModel);
        payload.put("messages", new Object[]{message});
        payload.put("max_tokens", 800);
        payload.put("temperature", 0.7);
        payload.put("stream", false);

        String endpoint = "/v1/chat/completions";
        String response = makeApiCall(endpoint, payload, 60);

        log.debug("Received response from analysis model");

        JsonNode rootNode = objectMapper.readTree(response);

        String analysis;
        if (rootNode.has("choices") && rootNode.get("choices").isArray() && rootNode.get("choices").size() > 0) {
            JsonNode firstChoice = rootNode.get("choices").get(0);
            analysis = firstChoice.get("message").get("content").asText();
        } else {
            log.error("Unexpected analysis response format: {}", response);
            analysis = "Analysis could not be completed. Please try again.";
        }

        log.info("Multi-feature analysis completed. Response length: {}", analysis.length());

        return AiModelResponse.builder()
                .response(analysis)
                .modelUsed(chatModel)
                .build();
    }

    private String makeApiCall(String endpoint, Map<String, Object> payload, int timeoutSeconds) throws Exception {
        try {
            log.debug("Making API call to: {}{}", hfRouterBase, endpoint);
            log.debug("Payload: {}", objectMapper.writeValueAsString(payload));

            String response = webClient.post()
                    .uri(endpoint)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(5))
                            .filter(throwable ->
                                    throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException.ServiceUnavailable))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            log.debug("Response received successfully");
            return response;

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("HTTP Error {} making API call to {}: {}",
                    e.getStatusCode(), endpoint, e.getResponseBodyAsString());
            throw new Exception("AI model error: " + e.getStatusCode() + " - " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error making API call to {}: {}", endpoint, e.getMessage(), e);
            throw new Exception("Failed to communicate with AI model: " + e.getMessage(), e);
        }
    }

    private String createExtractiveSummary(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "No content to summarize.";
        }

        String[] sentences = text.split("\\. ");
        StringBuilder summary = new StringBuilder();
        int count = 0;
        for (String sentence : sentences) {
            if (sentence.length() > 20 && count < 4) {
                summary.append(sentence.trim()).append(". ");
                count++;
            }
        }

        String result = summary.toString().trim();
        if (result.isEmpty()) {
            result = text.substring(0, Math.min(200, text.length())) + "...";
        }

        return result;
    }

    private String extractRelevantContext(String question, String context, int maxWords) {
        if (context == null || context.isEmpty()) {
            return context;
        }

        String lowerQuestion = question.toLowerCase();
        String[] questionWords = lowerQuestion.split("\\s+");
        String[] sentences = context.split("[.!?]\\s+");

        Map<String, Double> sentenceScores = new HashMap<>();
        for (String sentence : sentences) {
            String lowerSentence = sentence.toLowerCase();
            double score = 0.0;

            for (String word : questionWords) {
                if (word.length() > 3 && lowerSentence.contains(word)) {
                    score += 1.0;
                }
            }

            if (score > 0) {
                sentenceScores.put(sentence, score);
            }
        }

        if (sentenceScores.isEmpty()) {
            return truncateText(context, maxWords);
        }

        StringBuilder relevantContext = new StringBuilder();
        sentenceScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(entry -> relevantContext.append(entry.getKey()).append(". "));

        String result = relevantContext.toString();

        if (result.split("\\s+").length > maxWords) {
            return truncateText(result, maxWords);
        }

        return result;
    }

    private String truncateText(String text, int maxWords) {
        if (text == null || text.isEmpty()) {
            log.debug("truncateText called with empty text");
            return text;
        }

        String[] words = text.split("\\s+");
        if (words.length <= maxWords) {
            return text;
        }

        StringBuilder truncated = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            truncated.append(words[i]).append(" ");
        }

        log.warn("Text truncated from {} words to {} words", words.length, maxWords);
        return truncated.toString().trim();
    }

    private String buildMultiFeaturePrompt(String text, String additionalPrompt) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please analyze the following document and provide:\n\n");
        prompt.append("1. **Summary**: A concise 2-3 sentence summary\n");
        prompt.append("2. **Key Points**: 3-5 main points or insights\n");
        prompt.append("3. **Main Topics**: List the primary topics discussed\n");
        prompt.append("4. **Conclusions**: Any notable conclusions or takeaways\n\n");

        if (additionalPrompt != null && !additionalPrompt.trim().isEmpty()) {
            prompt.append("**Additional Focus**: ").append(additionalPrompt).append("\n\n");
        }

        prompt.append("**Document**:\n").append(text);

        return prompt.toString();
    }
}