package com.novaTech.Nova.Services.AI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UpdatedExternalApiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Injected configuration values
    private final String rapidApiKey;
    private final String netflixHost;
    private final String spotifyHost;
    private final String workoutHost;
    private final String shazamHost;
    private final String weatherApiHost;
    private final String openWeatherHost;

    public UpdatedExternalApiService(
            ObjectMapper objectMapper,
            @Value("${rapidapi.key}") String rapidApiKey,
            @Value("${rapidapi.netflix.host}") String netflixHost,
            @Value("${rapidapi.spotify.host}") String spotifyHost,
            @Value("${rapidapi.workout.host}") String workoutHost,
            @Value("${rapidapi.shazam.host}") String shazamHost,
            @Value("${rapidapi.weather.host}") String weatherApiHost,
            @Value("${rapidapi.open-weather.host}") String openWeatherHost) {
        this.objectMapper = objectMapper;
        this.rapidApiKey = rapidApiKey;
        this.netflixHost = netflixHost;
        this.spotifyHost = spotifyHost;
        this.workoutHost = workoutHost;
        this.shazamHost = shazamHost;
        this.weatherApiHost = weatherApiHost;
        this.openWeatherHost = openWeatherHost;
        this.webClient = WebClient.builder().build();
    }

    // ==================== NETFLIX API ====================

    /**
     * Search Netflix (non-streaming)
     */
    public String generateNetflixSearch(String query, int offset, int limitTitles, int limitSuggestions, String lang) {
        try {
            String url = "https://" + netflixHost + "/search/" +
                    "?query=" + query +
                    "&offset=" + offset +
                    "&limit_titles=" + limitTitles +
                    "&limit_suggestions=" + limitSuggestions +
                    "&lang=" + lang;

            String response = webClient.get()
                    .uri(url)
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", netflixHost)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return formatNetflixResults(response);

        } catch (WebClientResponseException e) {
            log.error("Netflix API Error: {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return "Netflix API Error: " + e.getRawStatusCode();
        } catch (Exception e) {
            log.error("Unexpected Error: {}", e.getMessage());
            return "Unexpected Error: " + e.getMessage();
        }
    }

    /**
     * Search Netflix (streaming)
     */
    public Flux<String> generateNetflixSearchStream(String query, int offset, int limitTitles, int limitSuggestions, String lang) {
        try {
            String url = "https://" + netflixHost + "/search/" +
                    "?query=" + query +
                    "&offset=" + offset +
                    "&limit_titles=" + limitTitles +
                    "&limit_suggestions=" + limitSuggestions +
                    "&lang=" + lang +
                    "&stream=true";

            return webClient.get()
                    .uri(url)
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", netflixHost)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .filter(line -> line.startsWith("data: "))
                    .map(line -> line.substring(6))
                    .filter(json -> !json.equals("[DONE]"))
                    .mapNotNull(json -> formatNetflixChunk(json));

        } catch (Exception e) {
            log.error("Netflix Stream Error: {}", e.getMessage());
            return Flux.just("Error: " + e.getMessage());
        }
    }

    private String formatNetflixResults(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            StringBuilder formatted = new StringBuilder("🎬 Netflix Results:\n\n");

            if (root.has("results")) {
                JsonNode results = root.get("results");
                int count = 1;
                for (JsonNode item : results) {
                    String title = item.has("title") ? item.get("title").asText() : "Unknown";
                    String type = item.has("type") ? item.get("type").asText() : "";
                    String year = item.has("year") ? item.get("year").asText() : "";

                    formatted.append(count).append(". **").append(title).append("**");
                    if (!type.isEmpty()) formatted.append(" (").append(type).append(")");
                    if (!year.isEmpty()) formatted.append(" - ").append(year);
                    formatted.append("\n");
                    count++;
                }
            }
            return formatted.toString();
        } catch (Exception e) {
            return jsonResponse;
        }
    }

    private String formatNetflixChunk(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("title")) {
                return "**" + node.get("title").asText() + "**\n";
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    // ==================== SPOTIFY API ====================

    /**
     * Search Spotify (non-streaming)
     */
    public String generateSpotifySearch(String query, int offset, int limit, int numberOfTopResults) {
        try {
            String url = "https://" + spotifyHost + "/search/" +
                    "?q=" + query +
                    "&type=multi" +
                    "&offset=" + offset +
                    "&limit=" + limit +
                    "&numberOfTopResults=" + numberOfTopResults;

            String response = webClient.get()
                    .uri(url)
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", spotifyHost)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return formatSpotifyResults(response);

        } catch (WebClientResponseException e) {
            log.error("Spotify API Error: {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return "Spotify API Error: " + e.getRawStatusCode();
        } catch (Exception e) {
            log.error("Unexpected Error: {}", e.getMessage());
            return "Unexpected Error: " + e.getMessage();
        }
    }

    /**
     * Search Spotify (streaming)
     */
    public Flux<String> generateSpotifySearchStream(String query, int offset, int limit, int numberOfTopResults) {
        try {
            String url = "https://" + spotifyHost + "/search/" +
                    "?q=" + query +
                    "&type=multi" +
                    "&offset=" + offset +
                    "&limit=" + limit +
                    "&numberOfTopResults=" + numberOfTopResults +
                    "&stream=true";

            return webClient.get()
                    .uri(url)
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", spotifyHost)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .filter(line -> line.startsWith("data: "))
                    .map(line -> line.substring(6))
                    .filter(json -> !json.equals("[DONE]"))
                    .mapNotNull(json -> formatSpotifyChunk(json));

        } catch (Exception e) {
            log.error("Spotify Stream Error: {}", e.getMessage());
            return Flux.just("Error: " + e.getMessage());
        }
    }

    private String formatSpotifyResults(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            StringBuilder formatted = new StringBuilder("🎵 Spotify Results:\n\n");

            if (root.has("tracks") && root.get("tracks").has("items")) {
                JsonNode tracks = root.get("tracks").get("items");
                int count = 1;
                for (JsonNode track : tracks) {
                    String name = track.has("name") ? track.get("name").asText() : "Unknown";
                    String artist = "";
                    if (track.has("artists") && track.get("artists").isArray() && track.get("artists").size() > 0) {
                        artist = track.get("artists").get(0).get("name").asText();
                    }

                    formatted.append(count).append(". **").append(name).append("**");
                    if (!artist.isEmpty()) formatted.append(" by ").append(artist);
                    formatted.append("\n");
                    count++;
                }
            }
            return formatted.toString();
        } catch (Exception e) {
            return jsonResponse;
        }
    }

    private String formatSpotifyChunk(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("name")) {
                return "**" + node.get("name").asText() + "**\n";
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    // ==================== WORKOUT PLANNER API ====================

    /**
     * Generate Workout Plan (non-streaming)
     */
    public String generateWorkoutPlan(String goal, String fitnessLevel, String[] preferences,
                                      String[] healthConditions, int daysPerWeek, int sessionDuration,
                                      int planDurationWeeks, String lang) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("goal", goal);
            requestBody.put("fitness_level", fitnessLevel);
            requestBody.put("preferences", preferences);
            requestBody.put("health_conditions", healthConditions);
            requestBody.put("schedule", Map.of(
                    "days_per_week", daysPerWeek,
                    "session_duration", sessionDuration
            ));
            requestBody.put("plan_duration_weeks", planDurationWeeks);
            requestBody.put("lang", lang);

            String response = webClient.post()
                    .uri("https://" + workoutHost + "/generateWorkoutPlan")
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", workoutHost)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return formatWorkoutPlan(response);

        } catch (WebClientResponseException e) {
            log.error("Workout API Error: {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return "Workout API Error: " + e.getRawStatusCode();
        } catch (Exception e) {
            log.error("Unexpected Error: {}", e.getMessage());
            return "Unexpected Error: " + e.getMessage();
        }
    }

    /**
     * Generate Workout Plan (streaming)
     */
    public Flux<String> generateWorkoutPlanStream(String goal, String fitnessLevel, String[] preferences,
                                                  String[] healthConditions, int daysPerWeek, int sessionDuration,
                                                  int planDurationWeeks, String lang) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("goal", goal);
            requestBody.put("fitness_level", fitnessLevel);
            requestBody.put("preferences", preferences);
            requestBody.put("health_conditions", healthConditions);
            requestBody.put("schedule", Map.of(
                    "days_per_week", daysPerWeek,
                    "session_duration", sessionDuration
            ));
            requestBody.put("plan_duration_weeks", planDurationWeeks);
            requestBody.put("lang", lang);
            requestBody.put("stream", true);

            return webClient.post()
                    .uri("https://" + workoutHost + "/generateWorkoutPlan")
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", workoutHost)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .filter(line -> line.startsWith("data: "))
                    .map(line -> line.substring(6))
                    .filter(json -> !json.equals("[DONE]"))
                    .mapNotNull(json -> formatWorkoutChunk(json));

        } catch (Exception e) {
            log.error("Workout Stream Error: {}", e.getMessage());
            return Flux.just("Error: " + e.getMessage());
        }
    }

    private String formatWorkoutPlan(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            StringBuilder formatted = new StringBuilder("💪 Workout Plan:\n\n");

            if (root.has("plan")) {
                formatted.append(root.get("plan").asText());
            } else {
                formatted.append(jsonResponse);
            }
            return formatted.toString();
        } catch (Exception e) {
            return jsonResponse;
        }
    }

    private String formatWorkoutChunk(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("content")) {
                return node.get("content").asText();
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Nutrition Advice (non-streaming)
     */
    public String generateNutritionAdvice(String goal, String[] dietaryRestrictions, double currentWeight,
                                          double targetWeight, String dailyActivityLevel, String lang) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("goal", goal);
            requestBody.put("dietary_restrictions", dietaryRestrictions);
            requestBody.put("current_weight", currentWeight);
            requestBody.put("target_weight", targetWeight);
            requestBody.put("daily_activity_level", dailyActivityLevel);
            requestBody.put("lang", lang);

            String response = webClient.post()
                    .uri("https://" + workoutHost + "/nutritionAdvice")
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", workoutHost)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return formatNutritionAdvice(response);

        } catch (WebClientResponseException e) {
            log.error("Nutrition API Error: {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return "Nutrition API Error: " + e.getRawStatusCode();
        } catch (Exception e) {
            log.error("Unexpected Error: {}", e.getMessage());
            return "Unexpected Error: " + e.getMessage();
        }
    }

    /**
     * Nutrition Advice (streaming)
     */
    public Flux<String> generateNutritionAdviceStream(String goal, String[] dietaryRestrictions, double currentWeight,
                                                      double targetWeight, String dailyActivityLevel, String lang) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("goal", goal);
            requestBody.put("dietary_restrictions", dietaryRestrictions);
            requestBody.put("current_weight", currentWeight);
            requestBody.put("target_weight", targetWeight);
            requestBody.put("daily_activity_level", dailyActivityLevel);
            requestBody.put("lang", lang);
            requestBody.put("stream", true);

            return webClient.post()
                    .uri("https://" + workoutHost + "/nutritionAdvice")
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", workoutHost)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .filter(line -> line.startsWith("data: "))
                    .map(line -> line.substring(6))
                    .filter(json -> !json.equals("[DONE]"))
                    .mapNotNull(json -> formatNutritionChunk(json));

        } catch (Exception e) {
            log.error("Nutrition Stream Error: {}", e.getMessage());
            return Flux.just("Error: " + e.getMessage());
        }
    }

    private String formatNutritionAdvice(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            StringBuilder formatted = new StringBuilder("🥗 Nutrition Advice:\n\n");

            if (root.has("advice")) {
                formatted.append(root.get("advice").asText());
            } else {
                formatted.append(jsonResponse);
            }
            return formatted.toString();
        } catch (Exception e) {
            return jsonResponse;
        }
    }

    private String formatNutritionChunk(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("content")) {
                return node.get("content").asText();
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Exercise Details (non-streaming)
     */
    public String generateExerciseDetails(String exerciseName, String lang) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "exercise_name", exerciseName,
                    "lang", lang
            );

            String response = webClient.post()
                    .uri("https://" + workoutHost + "/exerciseDetails")
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", workoutHost)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return formatExerciseDetails(response);

        } catch (WebClientResponseException e) {
            log.error("Exercise API Error: {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return "Exercise API Error: " + e.getRawStatusCode();
        } catch (Exception e) {
            log.error("Unexpected Error: {}", e.getMessage());
            return "Unexpected Error: " + e.getMessage();
        }
    }

    /**
     * Exercise Details (streaming)
     */
    public Flux<String> generateExerciseDetailsStream(String exerciseName, String lang) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "exercise_name", exerciseName,
                    "lang", lang,
                    "stream", true
            );

            return webClient.post()
                    .uri("https://" + workoutHost + "/exerciseDetails")
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", workoutHost)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .filter(line -> line.startsWith("data: "))
                    .map(line -> line.substring(6))
                    .filter(json -> !json.equals("[DONE]"))
                    .mapNotNull(json -> formatExerciseChunk(json));

        } catch (Exception e) {
            log.error("Exercise Stream Error: {}", e.getMessage());
            return Flux.just("Error: " + e.getMessage());
        }
    }

    private String formatExerciseDetails(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            StringBuilder formatted = new StringBuilder("🏋️ Exercise Details:\n\n");

            if (root.has("details")) {
                formatted.append(root.get("details").asText());
            } else {
                formatted.append(jsonResponse);
            }
            return formatted.toString();
        } catch (Exception e) {
            return jsonResponse;
        }
    }

    private String formatExerciseChunk(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("content")) {
                return node.get("content").asText();
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    // ==================== SHAZAM API (Song Recognition) ====================

    /**
     * Recognize Song from URL (non-streaming)
     */
    public String generateSongRecognition(String audioUrl) {
        try {
            byte[] audioBytes = downloadAudioFromUrl(audioUrl);
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

            Map<String, Object> requestBody = Map.of("audio", base64Audio);

            String response = webClient.post()
                    .uri("https://" + shazamHost + "/songs/detect")
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", shazamHost)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return formatSongRecognition(response);

        } catch (Exception e) {
            log.error("Shazam API Error: {}", e.getMessage());
            return "Song Recognition Error: " + e.getMessage();
        }
    }

    /**
     * Recognize Song from URL (streaming)
     */
    public Flux<String> generateSongRecognitionStream(String audioUrl) {
        try {
            byte[] audioBytes = downloadAudioFromUrl(audioUrl);
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

            Map<String, Object> requestBody = Map.of(
                    "audio", base64Audio,
                    "stream", true
            );

            return webClient.post()
                    .uri("https://" + shazamHost + "/songs/detect")
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", shazamHost)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .filter(line -> line.startsWith("data: "))
                    .map(line -> line.substring(6))
                    .filter(json -> !json.equals("[DONE]"))
                    .mapNotNull(json -> formatSongChunk(json));

        } catch (Exception e) {
            log.error("Shazam Stream Error: {}", e.getMessage());
            return Flux.just("Error: " + e.getMessage());
        }
    }

    private byte[] downloadAudioFromUrl(String audioUrl) throws Exception {
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
            return buffer.toByteArray();
        }
    }

    private String formatSongRecognition(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            StringBuilder formatted = new StringBuilder("🎵 Song Recognition:\n\n");

            if (root.has("track")) {
                JsonNode track = root.get("track");
                String title = track.has("title") ? track.get("title").asText() : "Unknown";
                String artist = track.has("subtitle") ? track.get("subtitle").asText() : "Unknown";

                formatted.append("**").append(title).append("**\n");
                formatted.append("Artist: ").append(artist).append("\n");
            } else {
                formatted.append(jsonResponse);
            }
            return formatted.toString();
        } catch (Exception e) {
            return jsonResponse;
        }
    }

    private String formatSongChunk(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("title")) {
                return "**" + node.get("title").asText() + "**\n";
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    // ==================== WEATHER APIs ====================

    /**
     * Get Country Weather Info (non-streaming)
     */
    public String generateCountryWeatherInfo(String country) {
        try {
            String url = "https://" + weatherApiHost + "/api/weather/country/detail?country=" + country;

            String response = webClient.get()
                    .uri(url)
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", weatherApiHost)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return formatCountryWeather(response);

        } catch (WebClientResponseException e) {
            log.error("Weather API Error: {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return "Weather API Error: " + e.getRawStatusCode();
        } catch (Exception e) {
            log.error("Unexpected Error: {}", e.getMessage());
            return "Unexpected Error: " + e.getMessage();
        }
    }

    /**
     * Get Country Weather Info (streaming)
     */
    public Flux<String> generateCountryWeatherInfoStream(String country) {
        try {
            String url = "https://" + weatherApiHost + "/api/weather/country/detail?country=" + country + "&stream=true";

            return webClient.get()
                    .uri(url)
                    .header("x-rapidapi-key", rapidApiKey)
                    .header("x-rapidapi-host", weatherApiHost)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .filter(line -> line.startsWith("data: "))
                    .map(line -> line.substring(6))
                    .filter(json -> !json.equals("[DONE]"))
                    .mapNotNull(json -> formatWeatherChunk(json));

        } catch (Exception e) {
            log.error("Weather Stream Error: {}", e.getMessage());
            return Flux.just("Error: " + e.getMessage());
        }
    }

    private String formatCountryWeather(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            StringBuilder formatted = new StringBuilder("🌤️ Country Weather Info:\n\n");
            formatted.append(jsonResponse);
            return formatted.toString();
        } catch (Exception e) {
            return jsonResponse;
        }
    }

    private String formatWeatherChunk(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("content")) {
                return node.get("content").asText();
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get Current Weather by City (non-streaming)
     */
    public String generateCurrentWeatherByCity(String city, String lang) {
        try {
            String language = (lang != null && !lang.isEmpty()) ? lang : "EN";
            String url = "https://" + openWeatherHost + "/city?city=" + city + "&lang=" + language;

            String response = webClient.get()
                    .uri(url)
                    .header("X-Rapidapi-Key", rapidApiKey)
                    .header("X-Rapidapi-Host", openWeatherHost)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return formatCityWeather(response);

        } catch (WebClientResponseException e) {
            log.error("Open Weather API Error: {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return "Open Weather API Error: " + e.getRawStatusCode();
        } catch (Exception e) {
            log.error("Unexpected Error: {}", e.getMessage());
            return "Unexpected Error: " + e.getMessage();
        }
    }

    /**
     * Get Current Weather by City (streaming)
     */
    public Flux<String> generateCurrentWeatherByCityStream(String city, String lang) {
        try {
            String language = (lang != null && !lang.isEmpty()) ? lang : "EN";
            String url = "https://" + openWeatherHost + "/city?city=" + city + "&lang=" + language + "&stream=true";

            return webClient.get()
                    .uri(url)
                    .header("X-Rapidapi-Key", rapidApiKey)
                    .header("X-Rapidapi-Host", openWeatherHost)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .filter(line -> line.startsWith("data: "))
                    .map(line -> line.substring(6))
                    .filter(json -> !json.equals("[DONE]"))
                    .mapNotNull(json -> formatCityWeatherChunk(json));

        } catch (Exception e) {
            log.error("City Weather Stream Error: {}", e.getMessage());
            return Flux.just("Error: " + e.getMessage());
        }
    }

    private String formatCityWeather(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            StringBuilder formatted = new StringBuilder("🌡️ Current Weather:\n\n");

            if (root.has("main")) {
                JsonNode main = root.get("main");
                double temp = main.has("temp") ? main.get("temp").asDouble() : 0;
                String description = root.has("weather") && root.get("weather").isArray() && root.get("weather").size() > 0
                        ? root.get("weather").get(0).get("description").asText()
                        : "N/A";

                formatted.append("Temperature: ").append(temp).append("°C\n");
                formatted.append("Description: ").append(description).append("\n");
            } else {
                formatted.append(jsonResponse);
            }
            return formatted.toString();
        } catch (Exception e) {
            return jsonResponse;
        }
    }

    private String formatCityWeatherChunk(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("content")) {
                return node.get("content").asText();
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }
}