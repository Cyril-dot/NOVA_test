package com.novaTech.Nova.controller;

import com.novaTech.Nova.DTO.ProcessingHistoryResponse;
import com.novaTech.Nova.Entities.Enums.Role;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.DocumentProcessingService;
import com.novaTech.Nova.Services.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HistoryController {

    private final DocumentProcessingService processingService;
    private final UserRegistrationService userRegistrationService;

    private UserPrincipal userPrincipal(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("No authentication found in SecurityContext");
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            log.error("Invalid principal type: {}", principal != null ? principal.getClass().getName() : "null");
            throw new RuntimeException("Invalid authentication principal");
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;
        log.debug("Successfully retrieved UserPrincipal for user: {} (ID: {})",
                userPrincipal.getEmail(), userPrincipal.getUserId());

        return userPrincipal;
    }

    /**
     * Get all processing history for current user
     */
    @GetMapping
    public ResponseEntity<List<ProcessingHistoryResponse>> getUserHistory() {

      UserPrincipal principal = userPrincipal();
      String username = principal.getUsername();// ✅ Single fetch
        log.info("GET /api/v1/history - Getting all processing history for user: {}", username);

        User user = userRegistrationService.findByEmail(username);
        log.info("GET /api/v1/history - Processing history for user: {}", username);
        try {
            List<ProcessingHistoryResponse> history = processingService.getUserHistory(user);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error getting user history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get history by functionality type for current user
     */
    @GetMapping("/functionality/{type}")
    public ResponseEntity<List<ProcessingHistoryResponse>> getUserHistoryByFunctionality(@PathVariable String type) {

        UserPrincipal principal = userPrincipal();
        String username = principal.getUsername();

        User user = userRegistrationService.findByEmail(username);
        log.info("GET /api/v1/history/functionality/{} for user: {}", type, user.getUsername());

        try {
            List<ProcessingHistoryResponse> history =
                    processingService.getUserHistoryByFunctionality(user, type);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error getting user history by functionality", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all processing history (admin only)
     */
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllHistory() {

        UserPrincipal principal = userPrincipal();
        String username = principal.getUsername();

        User user = userRegistrationService.findByEmail(username);

        // ✅ Check if user is admin
        if (user.getRole() != Role.ADMIN) {
            log.warn("Non-admin user {} attempted to access admin endpoint", user.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "Access denied. Admin role required."));
        }

        log.info("GET /api/v1/history/admin/all - Admin {} getting all processing history", user.getUsername());

        try {
            List<ProcessingHistoryResponse> history = processingService.getAllHistory();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error getting all history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get history by functionality (admin only)
     */
    @GetMapping("/admin/functionality/{type}")
    public ResponseEntity<?> getHistoryByFunctionality(@PathVariable String type) {
        UserPrincipal principal = userPrincipal();
        String username = principal.getUsername();

        User user = userRegistrationService.findByEmail(username);

        // ✅ Check if user is admin
        if (user.getRole() != Role.ADMIN) {
            log.warn("Non-admin user {} attempted to access admin endpoint", user.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "Access denied. Admin role required."));
        }

        log.info("GET /api/v1/history/admin/functionality/{} - Admin {} getting history",
                type, user.getUsername());

        try {
            List<ProcessingHistoryResponse> history = processingService.getHistoryByFunctionality(type);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error getting history by functionality", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }
}