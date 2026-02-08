package com.novaTech.Nova.Services.AI;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LLMService {

    private final WebClient webClient;
    private final OkHttpClient client = new OkHttpClient();

    private final String searchApiKey;
    private final String searchUrl;
    private final String rapidApiKey;
    private final String shazamHost;
    private final String shazamUrl;
    private final String rapidApiHost;

    public LLMService(
            @Value("${huggingface.text.api.url}") String apiUrl,
            @Value("${huggingface.api.key}") String apiKey,
            @Value("${searchapi.key}") String searchApiKey,
            @Value("${search.api.url}") String searchUrl,
            @Value("${rapidapi.key}") String rapidApiKey,
            @Value("${rapidapi.shazam.host}") String shazamHost,
            @Value("${rapidapi.shazam.url}") String shazamUrl,
            @Value("${rapidapi.weather.host}") String rapidApiHost) {
        this.searchApiKey = searchApiKey;
        this.searchUrl = searchUrl;
        this.rapidApiKey = rapidApiKey;
        this.shazamHost = shazamHost;
        this.shazamUrl = shazamUrl;
        this.rapidApiHost = rapidApiHost;
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // Hugging Face text generation (NON-STREAMING)
    public Mono<String> generateText(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Prompt must not be null or empty"));
        }

        Map<String, Object> body = Map.of(
                "model", "meta-llama/Llama-3.1-8B-Instruct",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 512,
                "temperature", 0.7,
                "stream", false
        );

        return webClient.post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    try {
                        log.info("Full API response: {}", response);

                        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                            if (message != null && message.containsKey("content")) {
                                return message.get("content").toString();
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error parsing Hugging Face response", e);
                    }
                    return "Error: No content returned from API";
                })
                .doOnSuccess(resp -> log.info("Text generation successful: {}", resp))
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException webError = (WebClientResponseException) error;
                        log.error("API Error - Status: {}, Body: {}",
                                webError.getStatusCode(),
                                webError.getResponseBodyAsString());
                    } else {
                        log.error("Error generating text", error);
                    }
                })
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5))
                        .filter(throwable -> !(throwable instanceof WebClientResponseException.Forbidden)))
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(error -> {
                    if (error instanceof WebClientResponseException.Forbidden) {
                        return Mono.just("Error: API access forbidden. Please check your API key and model access permissions.");
                    }
                    return Mono.just("Error: " + error.getMessage());
                });
    }

    // Hugging Face text generation (STREAMING)
    public Flux<String> generateTextStream(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("Prompt must not be null or empty"));
        }

        Map<String, Object> body = Map.of(
                "model", "meta-llama/Llama-3.1-8B-Instruct",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 512,
                "temperature", 0.7,
                "stream", true
        );

        return webClient.post()
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .map(chunk -> {
                    if (chunk.startsWith("data: ")) {
                        String jsonData = chunk.substring(6).trim();
                        if (jsonData.equals("[DONE]")) {
                            return "";
                        }
                        try {
                            return extractContentFromChunk(jsonData);
                        } catch (Exception e) {
                            log.error("Error parsing stream chunk", e);
                            return "";
                        }
                    }
                    return "";
                })
                .filter(s -> !s.isEmpty())
                .doOnNext(chunk -> log.debug("Stream chunk: {}", chunk))
                .doOnError(error -> log.error("Streaming error", error))
                .onErrorResume(error -> Flux.just("Error: " + error.getMessage()));
    }

    private String extractContentFromChunk(String jsonChunk) {
        try {
            if (jsonChunk.contains("\"content\":")) {
                int start = jsonChunk.indexOf("\"content\":\"") + 11;
                int end = jsonChunk.indexOf("\"", start);
                if (start > 10 && end > start) {
                    return jsonChunk.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.error("Error extracting content from chunk", e);
        }
        return "";
    }

    // Serper Google search
    public String search(String query) throws IOException {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\"q\":\"" + query + "\"}");

        Request request = new Request.Builder()
                .url(searchUrl)
                .post(body)
                .addHeader("X-API-KEY", searchApiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    // Shazam recognize from byte array (file upload)
    public String recognizeSong(byte[] fileBytes, String fileName) throws IOException {
        RequestBody fileBody = RequestBody.create(MediaType.parse("audio/mpeg"), fileBytes);

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_file", fileName, fileBody)
                .build();

        Request request = new Request.Builder()
                .url(shazamUrl)
                .post(requestBody)
                .addHeader("x-rapidapi-key", rapidApiKey)
                .addHeader("x-rapidapi-host", shazamHost)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public String recognizeSongFromUrl(String audioUrl) throws IOException {
        URL url = new URL(audioUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();

            byte[] audioBytes = buffer.toByteArray();
            String fileName = audioUrl.substring(audioUrl.lastIndexOf("/") + 1);

            return recognizeSong(audioBytes, fileName);
        }
    }

    // Weather API: Get country info
    public String getCountryInfo(String country) throws IOException {
        HttpUrl url = HttpUrl.parse("https://" + rapidApiHost + "/api/weather/country/detail")
                .newBuilder()
                .addQueryParameter("country", country)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-key", rapidApiKey)
                .addHeader("x-rapidapi-host", rapidApiHost)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    // Open Weather API: Current weather by city name
    public String getCurrentWeatherByCity(String city, String lang) throws IOException {
        HttpUrl url = HttpUrl.parse("https://open-weather13.p.rapidapi.com/city")
                .newBuilder()
                .addQueryParameter("city", city)
                .addQueryParameter("lang", lang != null ? lang : "EN")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("X-Rapidapi-Key", rapidApiKey)
                .addHeader("X-Rapidapi-Host", "open-weather13.p.rapidapi.com")
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }
}