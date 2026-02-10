package com.novaTech.Nova.controller;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.DocumentType;
import com.novaTech.Nova.Entities.Enums.ProjectStatus;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.DocumentViewService;
import com.novaTech.Nova.Services.ProjectService;
import com.novaTech.Nova.Services.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;
    private final DocumentViewService documentViewService;

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


    // ========================
    // CREATE PROJECT
    // ========================
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProject(
            @ModelAttribute ProjectCreateDTO projectDto,
            @RequestParam(value = "documents", required = false) List<MultipartFile> documents
    ) {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            // Rebuild DTO with documents included
            ProjectCreateDTO dto = ProjectCreateDTO.builder()
                    .title(projectDto.getTitle())
                    .description(projectDto.getDescription())
                    .startDate(projectDto.getStartDate())
                    .endDate(projectDto.getEndDate())
                    .status(projectDto.getStatus())
                    .documents(documents)
                    .documentDescription(projectDto.getDocumentDescription())
                    .build();

            ProjectResponseDTO response = projectService.createProject(userId, dto);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Project created successfully",
                    "project", response
            ));

        } catch (RuntimeException | IOException e) {
            log.error("Error creating project: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }


    // ========================
// UPDATE PROJECT
// ========================
    @PutMapping(value = "/{projectId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProject(
            @PathVariable UUID projectId,
            @ModelAttribute ProjectUpdateDTO projectDto,
            @RequestParam(value = "documents", required = false) List<MultipartFile> documents
    ) {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            ProjectUpdateDTO dto = ProjectUpdateDTO.builder()
                    .title(projectDto.getTitle())
                    .description(projectDto.getDescription())
                    .startDate(projectDto.getStartDate())
                    .endDate(projectDto.getEndDate())
                    .status(projectDto.getStatus())
                    .documents(documents)
                    .documentDescription(projectDto.getDocumentDescription())
                    .build();

            ProjectResponseDTO response = projectService.updateProject(projectId, userId, dto);

            return ResponseEntity.ok(Map.of(
                    "message", "Project updated successfully",
                    "project", response
            ));

        } catch (RuntimeException | IOException e) {
            log.error("Error updating project: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ========================
    // GET PROJECT BY ID
    // ========================
    @GetMapping("/{projectId}")
    public ResponseEntity<?> getProject(
            @PathVariable UUID projectId,
            @RequestParam(value = "includeDocuments", defaultValue = "true") boolean includeDocuments) {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            ProjectResponseDTO response = projectService.getProjectById(projectId, userId, includeDocuments);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error fetching project: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ========================
    // GET ALL PROJECTS
    // ========================
    @GetMapping
    public ResponseEntity<?> getAllProjects(
            @RequestParam(value = "status", required = false) ProjectStatus status,
            @RequestParam(value = "includeDocuments", defaultValue = "false") boolean includeDocuments) {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            List<ProjectResponseDTO> projects = projectService.getAllProjects(userId , status, includeDocuments);
            return ResponseEntity.ok(Map.of(
                    "count", projects.size(),
                    "projects", projects
            ));

        } catch (RuntimeException e) {
            log.error("Error fetching projects: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ========================
    // DELETE PROJECT
    // ========================
    @DeleteMapping("/{projectId}")
    public ResponseEntity<?> deleteProject(
            @PathVariable UUID projectId) {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            projectService.deleteProject(projectId,userId);

            return ResponseEntity.ok(Map.of(
                    "message", "Project deleted successfully"
            ));

        } catch (RuntimeException e) {
            log.error("Error deleting project: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ========================
    // UPLOAD DOCUMENTS TO PROJECT
    // ========================
    @PostMapping(value = "/{projectId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocuments(
            @PathVariable UUID projectId,
            @RequestParam("documents") List<MultipartFile> documents,
            @RequestParam(value = "description", required = false) String description) {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            List<ProjectDocumentResponseDTO> uploadedDocuments = projectService.uploadDocumentsToProject(
                    projectId, userId, documents, description);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", uploadedDocuments.size() + " document(s) uploaded successfully",
                    "documents", uploadedDocuments
            ));

        } catch (RuntimeException | IOException e) {
            log.error("Error uploading documents: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ========================
    // GET ALL DOCUMENTS FOR PROJECT
    // ========================
    @GetMapping("/{projectId}/documents")
    public ResponseEntity<?> getProjectDocuments(
            @PathVariable UUID projectId,
            @RequestParam(value = "type", required = false) DocumentType type) {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();
            List<ProjectDocumentResponseDTO> documents = projectService.getProjectDocuments(projectId, userId, type);

            return ResponseEntity.ok(Map.of(
                    "count", documents.size(),
                    "documents", documents
            ));

        } catch (RuntimeException e) {
            log.error("Error fetching documents: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }


    // to get all user projects
    @GetMapping("/projects")
    public ResponseEntity<?> getAllProjects(){
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        List<ProjectResponseDTO> documents = projectService.getAllDocumentsForUser(userId);

        return ResponseEntity.ok(Map.of(
                "count", documents.size(),
                "documents", documents
        ));
    }

    // to view most prjects according to most recent
    @GetMapping("/projects/ordered")
    public ResponseEntity<?> getProjectOrders(){
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        List<ProjectResponseDTO> documents = projectService.getOrderedProjects(userId);
        return ResponseEntity.ok(Map.of(
                "count", documents.size(),
                "documents", documents
        ));
    }





    // ========================
    // DOWNLOAD DOCUMENT
    // ========================
    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable UUID documentId) {
        try {
           UserPrincipal principal = userPrincipal();
           UUID userId = principal.getUserId();

            byte[] documentBytes = documentViewService.getDocumentContent(documentId, userId);

            // Get document details for headers
            ProjectDocumentResponseDTO documentInfo = projectService.getProjectDocuments(null, userId, null)
                    .stream()
                    .filter(doc -> doc.getId().equals(documentId))
                    .findFirst()
                    .orElse(null);

            HttpHeaders headers = new HttpHeaders();
            if (documentInfo != null) {
                MediaType mediaType = documentViewService.determineMediaType(documentBytes, documentInfo.getMimeType());
                headers.setContentType(mediaType);
                headers.setContentLength(documentBytes.length);
                headers.setContentDisposition(
                        ContentDisposition.builder("attachment")
                                .filename(documentInfo.getOriginalFileName())
                                .build()
                );
            } else {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }

            headers.setCacheControl("max-age=3600, must-revalidate");

            return new ResponseEntity<>(documentBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            log.error("Error downloading document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ========================
    // VIEW DOCUMENT (Inline)
    // ========================
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<byte[]> viewDocument(
            @PathVariable UUID documentId) {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            byte[] documentBytes = documentViewService.getDocumentContent(documentId, userId);

            // Get document details for headers
            ProjectDocumentResponseDTO documentInfo = projectService.getProjectDocuments(null, userId, null)
                    .stream()
                    .filter(doc -> doc.getId().equals(documentId))
                    .findFirst()
                    .orElse(null);

            HttpHeaders headers = new HttpHeaders();
            if (documentInfo != null) {
                MediaType mediaType = documentViewService.determineMediaType(documentBytes, documentInfo.getMimeType());
                headers.setContentType(mediaType);
                headers.setContentLength(documentBytes.length);
                headers.setContentDisposition(
                        ContentDisposition.builder("inline")
                                .filename(documentInfo.getOriginalFileName())
                                .build()
                );
            } else {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }

            headers.setCacheControl("max-age=3600, must-revalidate");

            return new ResponseEntity<>(documentBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            log.error("Error viewing document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ========================
    // DELETE DOCUMENT
    // ========================
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable UUID documentId) {
        try {
            UserPrincipal principal =  userPrincipal();
            UUID userId = principal.getUserId();
            projectService.deleteDocument(documentId, userId);

            return ResponseEntity.ok(Map.of(
                    "message", "Document deleted successfully"
            ));

        } catch (RuntimeException e) {
            log.error("Error deleting document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ========================
    // SEARCH PROJECTS
    // ========================
    @GetMapping("/search")
    public ResponseEntity<?> searchProjects(
            @RequestParam String keyword) {
        try {
            UserPrincipal principal =  userPrincipal();
            UUID userId = principal.getUserId();

            List<ProjectResponseDTO> projects = projectService.searchForProject(userId, keyword);
            return ResponseEntity.ok(Map.of(
                    "count", projects.size(),
                    "projects", projects
            ));

        } catch (RuntimeException e) {
            log.error("Error searching projects: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
