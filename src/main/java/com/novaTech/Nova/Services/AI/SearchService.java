package com.novaTech.Nova.Services.AI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@Service
public class SearchService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String searchApiKey;

    public SearchService(
            ObjectMapper objectMapper,
            @Value("${searchapi.key}") String searchApiKey,
            @Value("${search.api.url}") String searchUrl) {
        this.objectMapper = objectMapper;
        this.searchApiKey = searchApiKey;
        this.webClient = WebClient.builder()
                .baseUrl(searchUrl)
                .defaultHeader("X-API-KEY", searchApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String generateSearch(String query) {
        try {
            Map<String, Object> requestBody = Map.of("q", query);

            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return formatSearchResults(response);

        } catch (WebClientResponseException e) {
            log.error("Search API Error: {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return "Search API Error: " + e.getRawStatusCode() + " - " + e.getResponseBodyAsString();
        } catch (Exception e) {
            log.error("Unexpected Error during search: {}", e.getMessage());
            return "Unexpected Error: " + e.getMessage();
        }
    }

    public Flux<String> generateSearchStream(String query) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "q", query,
                    "stream", true
            );

            return webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .filter(line -> line.startsWith("data: "))
                    .map(line -> line.substring(6))
                    .filter(json -> !json.equals("[DONE]"))
                    .mapNotNull(json -> {
                        try {
                            JsonNode node = objectMapper.readTree(json);

                            if (node.has("result")) {
                                return formatSearchChunk(node.get("result"));
                            } else if (node.has("content")) {
                                return node.get("content").asText();
                            }

                            return null;
                        } catch (Exception e) {
                            log.warn("Failed to parse streaming search chunk: {}", e.getMessage());
                            return null;
                        }
                    });

        } catch (WebClientResponseException e) {
            log.error("Search API Error: {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return Flux.just("Search API Error: " + e.getRawStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected Error during search stream: {}", e.getMessage());
            return Flux.just("Unexpected Error: " + e.getMessage());
        }
    }

    private String formatSearchResults(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            StringBuilder formatted = new StringBuilder();

            formatted.append("🔍 Search Results:\n\n");

            if (root.has("results")) {
                JsonNode results = root.get("results");
                int count = 1;

                for (JsonNode result : results) {
                    String title = result.has("title") ? result.get("title").asText() : "No title";
                    String snippet = result.has("snippet") ? result.get("snippet").asText() : "";
                    String url = result.has("url") ? result.get("url").asText() : "";

                    formatted.append(count).append(". **").append(title).append("**\n");
                    if (!snippet.isEmpty()) {
                        formatted.append("   ").append(snippet).append("\n");
                    }
                    if (!url.isEmpty()) {
                        formatted.append("   🔗 ").append(url).append("\n");
                    }
                    formatted.append("\n");

                    count++;
                }

                return formatted.toString();
            }

            return jsonResponse;

        } catch (Exception e) {
            log.error("Error formatting search results: {}", e.getMessage());
            return jsonResponse;
        }
    }

    private String formatSearchChunk(JsonNode resultNode) {
        try {
            if (resultNode.has("title") && resultNode.has("snippet")) {
                String title = resultNode.get("title").asText();
                String snippet = resultNode.get("snippet").asText();

                return "**" + title + "**\n" + snippet + "\n\n";
            } else if (resultNode.isTextual()) {
                return resultNode.asText();
            }

            return "";

        } catch (Exception e) {
            log.warn("Error formatting search chunk: {}", e.getMessage());
            return "";
        }
    }
}