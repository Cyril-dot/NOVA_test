package com.novaTech.Nova.controller;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.DocumentType;
import com.novaTech.Nova.Entities.Enums.ProjectStatus;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Services.DocumentViewService;
import com.novaTech.Nova.Services.ProjectService;
import com.novaTech.Nova.Services.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
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
    private final UserRegistrationService userRegistrationService;
    private final DocumentViewService documentViewService;

    // ========================
    // CREATE PROJECT
    // ========================
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProject(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "status", required = false) ProjectStatus status,
            @RequestParam(value = "documents", required = false) List<MultipartFile> documents,
            @RequestParam(value = "documentDescription", required = false) String documentDescription,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            ProjectCreateDTO dto = ProjectCreateDTO.builder()
                    .title(title)
                    .description(description)
                    .startDate(startDate != null ? LocalDate.parse(startDate) : null)
                    .endDate(endDate != null ? LocalDate.parse(endDate) : null)
                    .status(status)
                    .documents(documents)
                    .documentDescription(documentDescription)
                    .build();

            ProjectResponseDTO response = projectService.createProject(user.getId(), dto);

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
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "status", required = false) ProjectStatus status,
            @RequestParam(value = "documents", required = false) List<MultipartFile> documents,
            @RequestParam(value = "documentDescription", required = false) String documentDescription,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            ProjectUpdateDTO dto = ProjectUpdateDTO.builder()
                    .title(name)
                    .description(description)
                    .startDate(startDate != null ? LocalDate.parse(startDate) : null)
                    .endDate(endDate != null ? LocalDate.parse(endDate) : null)
                    .status(status)
                    .documents(documents)
                    .documentDescription(documentDescription)
                    .build();

            ProjectResponseDTO response = projectService.updateProject(projectId, user.getId(), dto);

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
            @RequestParam(value = "includeDocuments", defaultValue = "true") boolean includeDocuments,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            ProjectResponseDTO response = projectService.getProjectById(projectId, user.getId(), includeDocuments);
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
            @RequestParam(value = "includeDocuments", defaultValue = "false") boolean includeDocuments,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            List<ProjectResponseDTO> projects = projectService.getAllProjects(user.getId(), status, includeDocuments);
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
            @PathVariable UUID projectId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            projectService.deleteProject(projectId, user.getId());

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
            @RequestParam(value = "description", required = false) String description,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            List<ProjectDocumentResponseDTO> uploadedDocuments = projectService.uploadDocumentsToProject(
                    projectId, user.getId(), documents, description);

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
            @RequestParam(value = "type", required = false) DocumentType type,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            List<ProjectDocumentResponseDTO> documents = projectService.getProjectDocuments(projectId, user.getId(), type);

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

    // ========================
    // DOWNLOAD DOCUMENT
    // ========================
    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable UUID documentId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            byte[] documentBytes = documentViewService.getDocumentContent(documentId, user.getId());

            // Get document details for headers
            ProjectDocumentResponseDTO documentInfo = projectService.getProjectDocuments(null, user.getId(), null)
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
            @PathVariable UUID documentId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            byte[] documentBytes = documentViewService.getDocumentContent(documentId, user.getId());

            // Get document details for headers
            ProjectDocumentResponseDTO documentInfo = projectService.getProjectDocuments(null, user.getId(), null)
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
            @PathVariable UUID documentId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            projectService.deleteDocument(documentId, user.getId());

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
            @RequestParam String keyword,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            List<ProjectResponseDTO> projects = projectService.searchForProject(user.getId(), keyword);
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
