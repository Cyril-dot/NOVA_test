package com.novaTech.Nova.Services.AI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.Map;

@Transactional
@Slf4j
@Service
public class CerebrasService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public CerebrasService(
            ObjectMapper objectMapper,
            @Value("${cerebras.api.key}") String apiKey,
            @Value("${cerebras.base.url}") String baseUrl,
            @Value("${cerebras.model}") String model) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String sendChat(String userMessage) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "stream", false,
                    "messages", new Map[]{Map.of("role", "user", "content", userMessage)},
                    "temperature", 0,
                    "max_tokens", -1,
                    "seed", 0,
                    "top_p", 1
            );

            return webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Cerebras API Error: {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return "Cerebras API Error: " + e.getRawStatusCode() + " - " + e.getResponseBodyAsString();
        } catch (Exception e) {
            log.error("Unexpected Error: {}", e.getMessage());
            return "Unexpected Error: " + e.getMessage();
        }
    }

    public String generateText(String userMessage) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", new Map[]{Map.of("role", "user", "content", userMessage)},
                    "temperature", 0,
                    "max_tokens", 512,
                    "top_p", 1,
                    "stream", false
            );

            Map response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            var choices = (java.util.List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                var message = (Map<String, Object>) choices.get(0).get("message");
                return message != null ? (String) message.get("content") : "";
            }
            return "";

        } catch (WebClientResponseException e) {
            log.error("Cerebras API Error: {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return "Cerebras API Error: " + e.getRawStatusCode() + " - " + e.getResponseBodyAsString();
        } catch (Exception e) {
            log.error("Unexpected Error: {}", e.getMessage());
            return "Unexpected Error: " + e.getMessage();
        }
    }

    public Flux<String> generateTextStream(String userMessage) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", new Map[]{Map.of("role", "user", "content", userMessage)},
                    "temperature", 0,
                    "max_tokens", 512,
                    "top_p", 1,
                    "stream", true
            );

            return webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .filter(line -> line.startsWith("data: "))
                    .map(line -> line.substring(6))
                    .filter(json -> !json.equals("[DONE]"))
                    .mapNotNull(json -> {
                        try {
                            JsonNode node = objectMapper.readTree(json);
                            JsonNode delta = node.path("choices").get(0).path("delta");
                            if (delta.has("content")) {
                                return delta.get("content").asText();
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse streaming chunk: {}", e.getMessage());
                        }
                        return null;
                    });

        } catch (WebClientResponseException e) {
            log.error("Cerebras API Error: {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return Flux.just("Cerebras API Error: " + e.getRawStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected Error: {}", e.getMessage());
            return Flux.just("Unexpected Error: " + e.getMessage());
        }
    }
}