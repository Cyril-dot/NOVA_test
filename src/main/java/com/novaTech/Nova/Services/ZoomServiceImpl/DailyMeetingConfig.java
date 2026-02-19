package com.novaTech.Nova.Services.ZoomServiceImpl;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Daily.co REST API integration.
 *
 * Required in your .env / deployment environment:
 *   DAILY_API_KEY           → Your Daily.co API key (never hard-code)
 *   DAILY_DOMAIN            → e.g. noav.daily.co
 *
 * Optional (have safe defaults):
 *   DAILY_API_URL           → https://api.daily.co/v1  (default)
 *   DAILY_ROOM_EXPIRY_HOURS → 24                       (default)
 *
 * Maps to application.properties:
 *   daily.api.key, daily.domain, daily.api.url, daily.room.expiry-hours
 */
@Configuration
@Getter
public class DailyMeetingConfig {

    /** Daily.co API key — injected from DAILY_API_KEY env var. */
    @Value("${daily.api.key}")
    private String apiKey;

    /** Base URL for all Daily REST calls. */
    @Value("${daily.api.url:https://api.daily.co/v1}")
    private String apiBaseUrl;

    /** Your Daily subdomain, e.g. noav.daily.co */
    @Value("${daily.domain:noav.daily.co}")
    private String domain;

    /**
     * How many hours a newly created room stays alive before Daily auto-deletes it.
     * Acts as a safety net — rooms are also explicitly deleted via deleteRoom().
     * Default: 24 hours.
     */
    @Value("${daily.room.expiry-hours:24}")
    private long roomExpiryHours;

    /**
     * Room expiry in seconds — used when setting the Unix timestamp `exp`
     * field on Daily room properties.
     */
    public long getRoomExpirySeconds() {
        return roomExpiryHours * 3600L;
    }

    /**
     * How long (seconds) a participant meeting token stays valid.
     * 2 hours for authenticated users; guest tokens use 30 min (hardcoded in service).
     */
    public long getTokenExpirySeconds() {
        return 7200L;
    }

    /**
     * RestTemplate pre-configured with the Daily API key Authorization header
     * and JSON content-type. Named "dailyRestTemplate" to avoid conflicts with
     * any other RestTemplate beans in the application context.
     */
    @Bean("dailyRestTemplate")
    public RestTemplate dailyRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            HttpHeaders headers = request.getHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}