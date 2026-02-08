package com.novaTech.Nova.controller.AI;

import com.novaTech.Nova.Services.AI.LLMService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {

    private final LLMService llmService;

    @PostConstruct
    public void printTestUrls() {
        log.info("=".repeat(80));
        log.info("🚀 AI CONTROLLER TEST URLS - Port 8080");
        log.info("=".repeat(80));

        log.info("\n📝 TEXT GENERATION:");
        log.info("POST http://localhost:8080/api/v1/ai/text?prompt=Hello, how are you?");
        log.info("Sample cURL:");
        log.info("curl -X POST 'http://localhost:8080/api/v1/ai/text?prompt=Tell me a joke'");

        log.info("\n🔍 GOOGLE SEARCH:");
        log.info("GET http://localhost:8080/api/v1/ai/search?q=artificial intelligence");
        log.info("Sample cURL:");
        log.info("curl 'http://localhost:8080/api/v1/ai/search?q=OpenAI GPT'");

        log.info("\n🎵 SHAZAM - FILE UPLOAD:");
        log.info("POST http://localhost:8080/api/v1/ai/recognize");
        log.info("Sample cURL:");
        log.info("curl -X POST -F 'file=@/path/to/song.mp3' http://localhost:8080/api/v1/ai/recognize");

        log.info("\n🎵 SHAZAM - URL:");
        log.info("POST http://localhost:8080/api/v1/ai/recognize-url?audioUrl=https://example.com/song.mp3");
        log.info("Sample cURL:");
        log.info("curl -X POST 'http://localhost:8080/api/v1/ai/recognize-url?audioUrl=https://example.com/audio.mp3'");

        log.info("\n🌍 COUNTRY INFO:");
        log.info("GET http://localhost:8080/api/v1/ai/country?country=Ghana");
        log.info("Sample cURL:");
        log.info("curl 'http://localhost:8080/api/v1/ai/country?country=USA'");

        log.info("\n🌤️  WEATHER BY CITY:");
        log.info("GET http://localhost:8080/api/v1/ai/open-weather/city?city=Accra&lang=EN");
        log.info("Sample cURL:");
        log.info("curl 'http://localhost:8080/api/v1/ai/open-weather/city?city=London&lang=EN'");

        log.info("\n" + "=".repeat(80));
        log.info("💡 TIP: Use Postman, Thunder Client, or your browser for GET requests");
        log.info("=".repeat(80) + "\n");
    }

    // Text generation
    @PostMapping("/text")
    @Operation(summary = "Generate text based on a prompt")
    public Mono<String> generateText(@RequestParam String prompt) {
        log.info("📝 [TEXT GENERATION] Request received with prompt: {}", prompt);
        return llmService.generateText(prompt)
                .doOnSuccess(response -> log.info("✅ [TEXT GENERATION] Success. Response length: {} chars",
                        response != null ? response.length() : 0))
                .doOnError(error -> log.error("❌ [TEXT GENERATION] Error: {}", error.getMessage()));
    }

    // Google Search
    @GetMapping("/search")
    @Operation(summary = "Search Google using Serper API")
    public ResponseEntity<String> search(@RequestParam String q) {
        log.info("🔍 [SEARCH] Query: {}", q);
        try {
            String result = llmService.search(q);
            log.info("✅ [SEARCH] Success. Result length: {} chars", result.length());
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("❌ [SEARCH] Error: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body("{\"error\": \"Search failed: " + e.getMessage() + "\"}");
        }
    }

    // Shazam recognize - file upload
    @PostMapping("/recognize")
    @Operation(summary = "Recognize song from uploaded audio file")
    public ResponseEntity<String> recognizeFile(@RequestParam("file") MultipartFile file) {
        log.info("🎵 [SHAZAM-FILE] File received: {} ({} bytes)",
                file.getOriginalFilename(), file.getSize());
        try {
            byte[] bytes = file.getBytes();
            String fileName = file.getOriginalFilename();
            String result = llmService.recognizeSong(bytes, fileName);
            log.info("✅ [SHAZAM-FILE] Recognition complete");
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("❌ [SHAZAM-FILE] Error: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body("{\"error\": \"Recognition failed: " + e.getMessage() + "\"}");
        }
    }

    // Shazam: URL Upload
    @PostMapping("/recognize-url")
    @Operation(summary = "Recognize song from audio URL")
    public ResponseEntity<String> recognizeUrl(@RequestParam String audioUrl) {
        log.info("🎵 [SHAZAM-URL] Processing audio from: {}", audioUrl);
        try {
            String result = llmService.recognizeSongFromUrl(audioUrl);
            log.info("✅ [SHAZAM-URL] Recognition complete. Result: {}", result);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("❌ [SHAZAM-URL] Error: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body("{\"error\": \"Recognition from URL failed: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/country")
    @Operation(summary = "Get country weather information")
    public ResponseEntity<String> getCountryInfo(@RequestParam String country) {
        log.info("🌍 [COUNTRY-INFO] Request for country: {}", country);
        try {
            String response = llmService.getCountryInfo(country);
            log.info("✅ [COUNTRY-INFO] Success. Response length: {} chars", response.length());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("❌ [COUNTRY-INFO] Error: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body("{\"error\": \"Error fetching country info: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/open-weather/city")
    @Operation(summary = "Get current weather for a city")
    public ResponseEntity<String> getWeatherByCity(
            @RequestParam String city,
            @RequestParam(required = false, defaultValue = "EN") String lang) {
        log.info("🌤️  [WEATHER] Request for city: {}, language: {}", city, lang);
        try {
            String response = llmService.getCurrentWeatherByCity(city, lang);
            log.info("✅ [WEATHER] Success. Response length: {} chars", response.length());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("❌ [WEATHER] Error: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body("{\"error\": \"Error fetching weather data: " + e.getMessage() + "\"}");
        }
    }

    // Health check endpoint
    @GetMapping("/health")
    @Operation(summary = "Health check for AI endpoints")
    public ResponseEntity<String> healthCheck() {
        log.info("💚 [HEALTH] Health check requested");
        return ResponseEntity.ok("{\"status\": \"UP\", \"service\": \"AI Controller\", \"port\": 8080}");
    }
}