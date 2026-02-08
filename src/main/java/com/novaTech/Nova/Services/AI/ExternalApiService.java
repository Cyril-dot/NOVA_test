package com.novaTech.Nova.Services.AI;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

@Service
@Slf4j
public class ExternalApiService {

    private final OkHttpClient client = new OkHttpClient();
    private final WebClient webClient;

    private final String rapidApiKey;
    private final String netflixHost;
    private final String netflixUrl;
    private final String spotifyHost;
    private final String spotifyUrl;
    private final String workoutHost;
    private final String workoutBaseUrl;

    public ExternalApiService(
            @Value("${rapidapi.key}") String rapidApiKey,
            @Value("${rapidapi.netflix.host}") String netflixHost,
            @Value("${rapidapi.netflix.url}") String netflixUrl,
            @Value("${rapidapi.spotify.host}") String spotifyHost,
            @Value("${rapidapi.spotify.url}") String spotifyUrl,
            @Value("${rapidapi.workout.host}") String workoutHost,
            @Value("${rapidapi.workout.base-url}") String workoutBaseUrl) {
        this.rapidApiKey = rapidApiKey;
        this.netflixHost = netflixHost;
        this.netflixUrl = netflixUrl;
        this.spotifyHost = spotifyHost;
        this.spotifyUrl = spotifyUrl;
        this.workoutHost = workoutHost;
        this.workoutBaseUrl = workoutBaseUrl;
        this.webClient = WebClient.builder().build();
    }

    // --- Netflix ---
    public String searchNetflix(String query, int offset, int limitTitles, int limitSuggestions, String lang) throws IOException {
        HttpUrl url = HttpUrl.parse(netflixUrl).newBuilder()
                .addQueryParameter("query", query)
                .addQueryParameter("offset", String.valueOf(offset))
                .addQueryParameter("limit_titles", String.valueOf(limitTitles))
                .addQueryParameter("limit_suggestions", String.valueOf(limitSuggestions))
                .addQueryParameter("lang", lang)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-key", rapidApiKey)
                .addHeader("x-rapidapi-host", netflixHost)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    // --- Spotify ---
    public String searchSpotify(String q, int offset, int limit, int numberOfTopResults) throws IOException {
        HttpUrl url = HttpUrl.parse(spotifyUrl).newBuilder()
                .addQueryParameter("q", q)
                .addQueryParameter("type", "multi")
                .addQueryParameter("offset", String.valueOf(offset))
                .addQueryParameter("limit", String.valueOf(limit))
                .addQueryParameter("numberOfTopResults", String.valueOf(numberOfTopResults))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-key", rapidApiKey)
                .addHeader("x-rapidapi-host", spotifyHost)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    // --- AI Workout Planner ---
    public String generateWorkoutPlan(String goal, String fitnessLevel, String[] preferences,
                                      String[] healthConditions, int daysPerWeek, int sessionDuration, int planDurationWeeks, String lang) throws IOException {
        String json = "{"
                + "\"goal\":\"" + goal + "\","
                + "\"fitness_level\":\"" + fitnessLevel + "\","
                + "\"preferences\":" + toJsonArray(preferences) + ","
                + "\"health_conditions\":" + toJsonArray(healthConditions) + ","
                + "\"schedule\":{\"days_per_week\":" + daysPerWeek + ",\"session_duration\":" + sessionDuration + "},"
                + "\"plan_duration_weeks\":" + planDurationWeeks + ","
                + "\"lang\":\"" + lang + "\""
                + "}";

        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(workoutBaseUrl + "/generateWorkoutPlan")
                .post(body)
                .addHeader("x-rapidapi-key", rapidApiKey)
                .addHeader("x-rapidapi-host", workoutHost)
                .addHeader("Content-Type", "application/json")
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public String nutritionAdvice(String goal, String[] dietaryRestrictions, double currentWeight, double targetWeight,
                                  String dailyActivityLevel, String lang) throws IOException {
        String json = "{"
                + "\"goal\":\"" + goal + "\","
                + "\"dietary_restrictions\":" + toJsonArray(dietaryRestrictions) + ","
                + "\"current_weight\":" + currentWeight + ","
                + "\"target_weight\":" + targetWeight + ","
                + "\"daily_activity_level\":\"" + dailyActivityLevel + "\","
                + "\"lang\":\"" + lang + "\""
                + "}";

        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(workoutBaseUrl + "/nutritionAdvice")
                .post(body)
                .addHeader("x-rapidapi-key", rapidApiKey)
                .addHeader("x-rapidapi-host", workoutHost)
                .addHeader("Content-Type", "application/json")
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public String exerciseDetails(String exerciseName, String lang) throws IOException {
        String json = "{"
                + "\"exercise_name\":\"" + exerciseName + "\","
                + "\"lang\":\"" + lang + "\""
                + "}";

        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(workoutBaseUrl + "/exerciseDetails")
                .post(body)
                .addHeader("x-rapidapi-key", rapidApiKey)
                .addHeader("x-rapidapi-host", workoutHost)
                .addHeader("Content-Type", "application/json")
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    // --- Helper ---
    private String toJsonArray(String[] arr) {
        if (arr == null || arr.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append("\"").append(arr[i]).append("\"");
            if (i != arr.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}