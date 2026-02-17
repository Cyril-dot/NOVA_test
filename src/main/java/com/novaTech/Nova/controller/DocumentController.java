package com.novaTech.Nova.controller;

import com.novaTech.Nova.DTO.DocumentProcessRequest;
import com.novaTech.Nova.DTO.DocumentProcessResponse;
import com.novaTech.Nova.DTO.DocumentUploadResponse;
import com.novaTech.Nova.DTO.ProcessingHistoryResponse;
import com.novaTech.Nova.Entities.Document;
import com.novaTech.Nova.Entities.Enums.FunctionalityType;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.DocumentProcessingService;
import com.novaTech.Nova.Services.DocumentService;
import com.novaTech.Nova.Services.UserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentProcessingService processingService;
    private final UserRegistrationService userService;

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
     * Upload document only (without AI processing)
     */
    @PostMapping(value = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestPart("file") MultipartFile file) {

        UserPrincipal principal = userPrincipal();
        String username = principal.getEmail();

        User user = userService.findByEmail(username);
        if (user == null){
            log.error("User not found: {}", username);
            throw new RuntimeException("User not found");
        }

        log.info("POST /api/v1/documents/upload - Uploading file: {} for user: {}",
                file.getOriginalFilename(), user.getUsername());

        try {
            DocumentUploadResponse response = documentService.uploadDocument(file, user);
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Error uploading document", e);
            DocumentUploadResponse errorResponse = DocumentUploadResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Upload and process document in one call (NotebookLM-style)
     */
    @PostMapping(value = "/process",consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<DocumentProcessResponse> processDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam("functionality") FunctionalityType functionality,
            @RequestParam(value = "question", required = false) String question,
            @RequestParam(value = "customPrompt", required = false) String customPrompt) {

        UserPrincipal principal = userPrincipal();
        String username = principal.getUsername();

        User user = userService.findByEmail(username);
        if (user == null){
            log.error("User not found: {}", username);
            throw new RuntimeException("User not found");
        }

        log.info("POST /api/v1/documents/process - Processing file: {} with functionality: {} for user: {}",
                file.getOriginalFilename(), functionality, user.getUsername());

        try {
            DocumentProcessRequest request = DocumentProcessRequest.builder()
                    .functionality(functionality)
                    .question(question)
                    .customPrompt(customPrompt)
                    .build();

            DocumentProcessResponse response = processingService.processDocument(file, request, user);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            log.error("Error processing document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DocumentProcessResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build());
        }
    }

    /**
     * Process existing document with AI
     */
    @PostMapping("/{documentId}/process")
    public ResponseEntity<DocumentProcessResponse> processExistingDocument(
            @PathVariable Long documentId,
            @Valid @RequestBody DocumentProcessRequest request) {

        UserPrincipal principal = userPrincipal();
        String username = principal.getUsername();

        User user = userService.findByEmail(username);
        if (user == null){
            log.error("User not found: {}", username);
            throw new RuntimeException("User not found");
        }

        log.info("POST /api/v1/documents/{}/process - Functionality: {} for user: {}",
                documentId, request.getFunctionality(), user.getUsername());

        try {
            DocumentProcessResponse response =
                    processingService.processExistingDocument(documentId, request, user);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            log.error("Error processing existing document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DocumentProcessResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build());
        }
    }

    /**
     * Get all documents for current user
     */
    @GetMapping
    public ResponseEntity<List<Document>> getUserDocuments() {

        UserPrincipal principal = userPrincipal();
        String username = principal.getUsername();

        User user = userService.findByEmail(username);
        if (user == null){
            log.error("User not found: {}", username);
            throw new RuntimeException("User not found");
        }

        log.info("GET /api/v1/documents - Getting all documents for user: {}", user.getUsername());

        try {
            List<Document> documents = documentService.getUserDocuments(user);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error getting user documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get document by ID (user-specific)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentById(@PathVariable Long id) {

        UserPrincipal principal = userPrincipal();
        String username = principal.getUsername();

        User user = userService.findByEmail(username);
        if (user == null){
            log.error("User not found: {}", username);
            throw new RuntimeException("User not found");
        }

        log.info("GET /api/v1/documents/{} for user: {}", id, user.getUsername());

        try {
            Document document = documentService.getDocumentById(id, user);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            log.error("Document not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete document (user-specific)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {

        UserPrincipal principal = userPrincipal();
        String username = principal.getUsername();

        User user = userService.findByEmail(username);
        if (user == null){
            log.error("User not found: {}", username);
            throw new RuntimeException("User not found");
        }
        log.info("DELETE /api/v1/documents/{} for user: {}", id, user.getUsername());

        try {
            documentService.deleteDocument(id, user);
            return ResponseEntity.ok().body(java.util.Map.of("message", "Document deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting document: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get processing history for a document (user-specific)
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<?> getDocumentHistory(@PathVariable Long id) {

        UserPrincipal principal = userPrincipal();
        String username = principal.getUsername();
        User user = userService.findByEmail(username);
        if (user == null){
            log.error("User not found: {}", username);
            throw new RuntimeException("User not found");
        }
        log.info("GET /api/v1/documents/{}/history for user: {}", id, user.getUsername());

        try {
            List<ProcessingHistoryResponse> history = processingService.getDocumentHistory(id, user);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error getting document history: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get user's document count
     */
    @GetMapping("/count")
    public ResponseEntity<?> getUserDocumentCount() {

        UserPrincipal principal = userPrincipal();
        String username = principal.getUsername();

        User user = userService.findByEmail(username);
        if (user == null){
            log.error("User not found: {}", username);
            throw new RuntimeException("User not found");
        }

        log.info("GET /api/v1/documents/count for user: {}", user.getUsername());

        try {
            Long count = documentService.getUserDocumentCount(user);
            return ResponseEntity.ok(java.util.Map.of("count", count));
        } catch (Exception e) {
            log.error("Error getting document count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get available functionalities
     */
    @GetMapping("/functionalities")
    public ResponseEntity<List<FunctionalityInfo>> getFunctionalities() {
        log.info("GET /api/v1/documents/functionalities");

        List<FunctionalityInfo> functionalities = Arrays.stream(FunctionalityType.values())
                .map(type -> new FunctionalityInfo(
                        type.name(),
                        type.getDescription(),
                        type.getDefaultModel()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(functionalities);
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> healthCheck() {
        return ResponseEntity.ok(new HealthResponse(
                true,
                "Document AI Assistant is running",
                System.currentTimeMillis()
        ));
    }

    // Inner classes for responses
    record FunctionalityInfo(String name, String description, String defaultModel) {}
    record HealthResponse(boolean healthy, String message, long timestamp) {}
}