// RateLimitController.java
package com.novaTech.Nova.controller;

import com.novaTech.Nova.Security.RateLimitingConfigs.RateLimitingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/rate-limit")
@RequiredArgsConstructor
public class RateLimitController {

    private final RateLimitingService rateLimitService;

    /**
     * Get cache statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(rateLimitService.getCacheStats());
    }

    /**
     * Clear entire cache
     */
    @DeleteMapping("/cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> clearCache() {
        rateLimitService.clearCache();
        return ResponseEntity.ok("Cache cleared successfully");
    }

    /**
     * Remove specific key from cache
     */
    @DeleteMapping("/cache/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> removeKey(@PathVariable String key) {
        rateLimitService.removeKey(key);
        return ResponseEntity.ok("Key removed successfully");
    }

    /**
     * Get available tokens for a key
     */
    @GetMapping("/tokens/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getTokens(@PathVariable String key) {
        long available = rateLimitService.getAvaliableTokens(key);
        long secondsUntilRefill = rateLimitService.getSecondsUntilRefil(key);
        
        return ResponseEntity.ok(Map.of(
                "key", key,
                "availableTokens", available,
                "secondsUntilRefill", secondsUntilRefill
        ));
    }
}