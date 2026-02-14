package com.novaTech.Nova.controller.WorkSpace;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.DocType;
import com.novaTech.Nova.Entities.workSpace.WorkSpaceDocs;
import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.workSpace.ProjectWorkSpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;



@RestController
@RequestMapping("/api/v1/project/workspace")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Project Workspace", description = "Project workspace management endpoints")
@PreAuthorize("isAuthenticated()")
public class ProjectWorkSpaceController {

    private final ProjectWorkSpaceService projectWorkSpaceService;

    // ==================== PROJECT WORKSPACE ENDPOINTS ====================

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



    @PostMapping("/create/{projectId}")
    @Operation(summary = "Create project workspace", description = "Creates a new workspace for a specific project")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Project workspace created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Unauthorized - project doesn't belong to user"),
        @ApiResponse(responseCode = "404", description = "Project or user not found")
    })
    public ResponseEntity<ProjectWorkSpaceCreationResponse> createProjectWorkspace(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Valid @RequestBody WorkSpaceCreationDto creationDto) {

        UserPrincipal userPrincipal = userPrincipal();
        UUID userId = userPrincipal.getUserId();

        log.info("REST - Creating project workspace for project: {}, user: {}", projectId, userId);
        log.debug("REST - Workspace details - Title: {}, Description: {}", 
                 creationDto.title(), creationDto.description());
        
        try {
            ProjectWorkSpaceCreationResponse response = projectWorkSpaceService
                    .createProjectWorkSpace(creationDto, projectId, userId);
            log.info("REST - Successfully created project workspace with ID: {} for project: {}", 
                    response.id(), projectId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("REST - Error creating project workspace for project: {}, user: {}", 
                     projectId, userId, e);
            throw e;
        }
    }

    /**
     * Create template for project workspace document
     */
    @PostMapping("/template/{projectId}/{docId}")
    @Operation(summary = "Create project workspace template", 
               description = "Creates a code/document template for project workspace")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid document type"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Project, user, or workspace not found")
    })
    public ResponseEntity<ActiveProjectWorkSpaceDocs> createProjectTemplate(
            @Parameter(description = "Document type", required = true) @RequestParam DocType docType,
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId) {

        UserPrincipal userPrincipal = userPrincipal();
        UUID userId = userPrincipal.getUserId();

        log.info("REST - Creating template for project workspace: {}, project: {}, docType: {}", 
                docId, projectId, docType);
        
        try {
            ActiveProjectWorkSpaceDocs response = projectWorkSpaceService
                    .createProjectWorkSpaceDocsTemplate(docType, userId, docId, projectId);
            log.info("REST - Successfully created {} template for project workspace: {}", 
                    docType, docId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error creating template for project workspace: {}, project: {}", 
                     docId, projectId, e);
            throw e;
        }
    }

    /**
     * View project workspace document
     */
    @GetMapping("/view/{projectId}/{docId}")
    @Operation(summary = "View project workspace", description = "Retrieves project workspace content")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<ViewProjectWorkSpaceDocs> viewProjectWorkspace(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId) {

        UserPrincipal userPrincipal = userPrincipal();
        UUID userId = userPrincipal.getUserId();

        log.info("REST - Viewing project workspace: {}, project: {}, user: {}",
                docId, projectId, userId);
        
        try {
            ViewProjectWorkSpaceDocs response = projectWorkSpaceService
                    .viewProjectWorkSpaceDocs(projectId, userId, docId);
            log.info("REST - Successfully retrieved project workspace: {}", docId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing project workspace: {}, project: {}", 
                     docId, projectId, e);
            throw e;
        }
    }

    /**
     * Work in project workspace (replaces content within template)
     */
    @PutMapping("/work/{projectId}/{docId}")
    @Operation(summary = "Work in project workspace", 
               description = "Updates workspace content by injecting new content into template")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<ActiveProjectWorkSpaceDocs> workInProjectWorkspace(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId,
            @Valid @RequestBody UpdateWorkSpaceDocsDto updateDto) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Working in project workspace: {}, project: {}", docId, projectId);
        log.debug("REST - Content length: {} characters", updateDto.content().length());
        
        try {
            ActiveProjectWorkSpaceDocs response = projectWorkSpaceService
                    .workInSpaceDocs(userId, docId, updateDto, projectId);
            log.info("REST - Successfully updated project workspace: {}", docId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error working in project workspace: {}, project: {}", 
                     docId, projectId, e);
            throw e;
        }
    }

    /**
     * Continue work on existing project workspace (appends to existing content)
     */
    @PatchMapping("/continue/{projectId}/{docId}")
    @Operation(summary = "Continue project workspace work", 
               description = "Appends new content to existing workspace content")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<ActiveProjectWorkSpaceDocs> continueProjectWork(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId,
            @Valid @RequestBody UpdateWorkSpaceDocsDto updateDto) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Continuing work on project workspace: {}, project: {}", docId, projectId);
        log.debug("REST - Appending content length: {} characters", updateDto.content().length());
        
        try {
            ActiveProjectWorkSpaceDocs response = projectWorkSpaceService
                    .continueWorkOnExistingDocs(userId, projectId, docId, updateDto);
            log.info("REST - Successfully continued work on project workspace: {}", docId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error continuing work on project workspace: {}, project: {}", 
                     docId, projectId, e);
            throw e;
        }
    }

    /**
     * Download project workspace file
     */
    @GetMapping("/download/{projectId}/{docId}")
    @Operation(summary = "Download project workspace", description = "Downloads workspace as a file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<byte[]> downloadProjectWorkspace(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Download request for project workspace: {}, project: {}", docId, projectId);
        
        try {

            ResponseEntity<byte[]> response = projectWorkSpaceService
                    .downloadWorkSpaceDoc(docId, userId, projectId);
            log.info("REST - Successfully prepared download for project workspace: {}", docId);
            return response;
            
        } catch (Exception e) {
            log.error("REST - Error downloading project workspace: {}, project: {}", 
                     docId, projectId, e);
            throw e;
        }
    }

    /**
     * Delete project workspace
     */
    @DeleteMapping("/delete/{projectId}/{docId}")
    @Operation(summary = "Delete project workspace", description = "Permanently deletes a project workspace")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<String> deleteProjectWorkspace(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Delete request for project workspace: {}, project: {}", docId, projectId);
        
        try {
            String response = projectWorkSpaceService.deleteDocs(docId, userId, projectId);
            log.info("REST - Successfully deleted project workspace: {}", docId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error deleting project workspace: {}, project: {}", 
                     docId, projectId, e);
            throw e;
        }
    }

    /**
     * Download all project workspaces as ZIP
     */
    @GetMapping("/download-all/{projectId}")
    @Operation(summary = "Download all project workspaces", 
               description = "Downloads all workspaces for a project as a ZIP file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ZIP file created successfully"),
        @ApiResponse(responseCode = "204", description = "No workspaces found"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<byte[]> downloadAllProjectWorkspaces(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();


        log.info("REST - Download all workspaces for project: {}, user: {}", projectId, userId);
        
        try {
            ResponseEntity<byte[]> response = projectWorkSpaceService
                    .projectWorkSpaceFolder(userId, projectId);
            
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.warn("REST - No workspaces found for project: {}", projectId);
            } else {
                log.info("REST - Successfully created ZIP file for project: {}", projectId);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("REST - Error creating ZIP file for project: {}", projectId, e);
            throw e;
        }
    }

    /**
     * View project workspaces by document type
     */
    @GetMapping("/by-type/{projectId}")
    @Operation(summary = "View project workspaces by type", 
               description = "Retrieves all project workspaces of a specific document type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspaces retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<List<ProjectWorkSpaceDocs>> viewProjectWorkspacesByType(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Document type", required = true) @RequestParam DocType docType) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();


        log.info("REST - Viewing project workspaces by type: {} for project: {}", 
                docType, projectId);
        
        try {
            List<ProjectWorkSpaceDocs> response = projectWorkSpaceService
                    .viewDocsByType(userId, projectId, docType);
            log.info("REST - Found {} workspaces of type {} for project: {}", 
                    response.size(), docType, projectId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing project workspaces by type for project: {}", 
                     projectId, e);
            throw e;
        }
    }

    /**
     * View most recent project workspaces
     */
    @GetMapping("/recent/{projectId}")
    @Operation(summary = "View recent project workspaces", 
               description = "Retrieves the 10 most recently created project workspaces")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recent workspaces retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<List<ProjectWorkSpaceDocs>> viewRecentProjectWorkspaces(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Viewing recent workspaces for project: {}", projectId);
        
        try {
            List<ProjectWorkSpaceDocs> response = projectWorkSpaceService
                    .viewMostRecentProjectSpaceDocs(userId, projectId);
            log.info("REST - Retrieved {} recent workspaces for project: {}", 
                    response.size(), projectId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing recent workspaces for project: {}", projectId, e);
            throw e;
        }
    }

    /**
     * Search project workspaces
     */
    @GetMapping("/search/{projectId}")
    @Operation(summary = "Search project workspaces", 
               description = "Searches project workspaces by keyword")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid search keyword"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<List<ProjectWorkSpaceDocs>> searchProjectWorkspaces(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Search keyword", required = true) @RequestParam String keyword) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Searching workspaces for project: {} with keyword: '{}'", 
                projectId, keyword);
        
        try {
            List<ProjectWorkSpaceDocs> response = projectWorkSpaceService
                    .searchProjectWorkSpaceDocs(userId, projectId, keyword);
            log.info("REST - Found {} workspaces matching '{}' for project: {}", 
                    response.size(), keyword, projectId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error searching workspaces for project: {} with keyword: '{}'", 
                     projectId, keyword, e);
            throw e;
        }
    }

    // ==================== PROJECT DOCUMENT WORKSPACE ENDPOINTS ====================

    /**
     * Create workspace for project document
     */
    @PostMapping("/document/create/{projectId}/{documentId}")
    @Operation(summary = "Create project document workspace", 
               description = "Creates a workspace for a specific project document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Document workspace created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Project or document not found")
    })
    public ResponseEntity<ProjectDocSpaceDocsCreationResponse> createProjectDocumentWorkspace(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Project document ID", required = true) @PathVariable UUID documentId,
            @Valid @RequestBody WorkSpaceCreationDto creationDto) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Creating workspace for project document: {}, project: {}", 
                documentId, projectId);
        log.debug("REST - Workspace details - Title: {}, Description: {}", 
                 creationDto.title(), creationDto.description());
        
        try {
            ProjectDocSpaceDocsCreationResponse response = projectWorkSpaceService
                    .createWorkForDocs(userId, projectId, documentId, creationDto);
            log.info("REST - Successfully created workspace for project document: {}", documentId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("REST - Error creating workspace for project document: {}, project: {}", 
                     documentId, projectId, e);
            throw e;
        }
    }

    /**
     * Create template for project document workspace
     */
    @PostMapping("/document/template/{projectId}/{documentId}/{spaceDocId}")
    @Operation(summary = "Create project document workspace template", 
               description = "Creates a template for project document workspace")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid document type"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Document or workspace not found")
    })
    public ResponseEntity<ActiveProjectDocumentWorkSpaceDocs> createProjectDocTemplate(
            @Parameter(description = "Document type", required = true) @RequestParam DocType docType,
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Project document ID", required = true) @PathVariable UUID documentId,
            @Parameter(description = "Workspace ID", required = true) @PathVariable Long spaceDocId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Creating template for project document workspace: {}, docType: {}", 
                spaceDocId, docType);
        
        try {
            ActiveProjectDocumentWorkSpaceDocs response = projectWorkSpaceService
                    .getDocTemplate(docType, userId, projectId, documentId, spaceDocId);
            log.info("REST - Successfully created {} template for document workspace: {}", 
                    docType, spaceDocId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error creating template for document workspace: {}", spaceDocId, e);
            throw e;
        }
    }

    /**
     * View project document workspace
     */
    @GetMapping("/document/view/{projectId}/{documentId}/{spaceDocId}")
    @Operation(summary = "View project document workspace", 
               description = "Retrieves project document workspace content")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<ViewWorkSpaceDocsData> viewProjectDocWorkspace(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Project document ID", required = true) @PathVariable UUID documentId,
            @Parameter(description = "Workspace ID", required = true) @PathVariable Long spaceDocId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Viewing project document workspace: {}, document: {}", 
                spaceDocId, documentId);
        
        try {
            ViewWorkSpaceDocsData response = projectWorkSpaceService
                    .viewProjectDocSpaceDocs(userId, projectId, documentId, spaceDocId);
            log.info("REST - Successfully retrieved document workspace: {}", spaceDocId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing document workspace: {}", spaceDocId, e);
            throw e;
        }
    }

    /**
     * Work in project document workspace
     */
    @PutMapping("/document/work/{projectId}/{documentId}/{spaceDocId}")
    @Operation(summary = "Work in project document workspace", 
               description = "Updates project document workspace content")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<ActiveProjectDocumentWorkSpaceDocs> workInProjectDocWorkspace(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Project document ID", required = true) @PathVariable UUID documentId,
            @Parameter(description = "Workspace ID", required = true) @PathVariable Long spaceDocId,
            @Valid @RequestBody UpdateWorkSpaceDocsDto updateDto) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Working in project document workspace: {}", spaceDocId);
        log.debug("REST - Content length: {} characters", updateDto.content().length());
        
        try {
            ActiveProjectDocumentWorkSpaceDocs response = projectWorkSpaceService
                    .workInSpaceDocsforProjectDocument(userId, projectId, documentId, spaceDocId, updateDto);
            log.info("REST - Successfully updated document workspace: {}", spaceDocId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error working in document workspace: {}", spaceDocId, e);
            throw e;
        }
    }

    /**
     * Continue work on project document workspace
     */
    @PatchMapping("/document/continue/{projectId}/{documentId}/{spaceDocId}")
    @Operation(summary = "Continue project document workspace work", 
               description = "Appends new content to existing document workspace content")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<ActiveProjectDocumentWorkSpaceDocs> continueProjectDocWork(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Project document ID", required = true) @PathVariable UUID documentId,
            @Parameter(description = "Workspace ID", required = true) @PathVariable Long spaceDocId,
            @Valid @RequestBody UpdateWorkSpaceDocsDto updateDto) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Continuing work on document workspace: {}", spaceDocId);
        log.debug("REST - Appending content length: {} characters", updateDto.content().length());
        
        try {
            ActiveProjectDocumentWorkSpaceDocs response = projectWorkSpaceService
                    .updateProjectDocSpaceDocs(userId, projectId, documentId, spaceDocId, updateDto);
            log.info("REST - Successfully continued work on document workspace: {}", spaceDocId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error continuing work on document workspace: {}", spaceDocId, e);
            throw e;
        }
    }

    /**
     * Download project document workspace
     */
    @GetMapping("/document/download/{projectId}/{documentId}/{spaceDocId}")
    @Operation(summary = "Download project document workspace", 
               description = "Downloads project document workspace as a file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<byte[]> downloadProjectDocWorkspace(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Project document ID", required = true) @PathVariable UUID documentId,
            @Parameter(description = "Workspace ID", required = true) @PathVariable Long spaceDocId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Download request for document workspace: {}", spaceDocId);
        
        try {
            ResponseEntity<byte[]> response = projectWorkSpaceService
                    .downloadProjectDocumentSpaceDocs(userId, projectId, documentId, spaceDocId);
            log.info("REST - Successfully prepared download for document workspace: {}", spaceDocId);
            return response;
            
        } catch (Exception e) {
            log.error("REST - Error downloading document workspace: {}", spaceDocId, e);
            throw e;
        }
    }

    /**
     * Delete project document workspace
     */
    @DeleteMapping("/document/delete/{projectId}/{documentId}/{spaceDocId}")
    @Operation(summary = "Delete project document workspace", 
               description = "Permanently deletes a project document workspace")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<String> deleteProjectDocWorkspace(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Project document ID", required = true) @PathVariable UUID documentId,
            @Parameter(description = "Workspace ID", required = true) @PathVariable Long spaceDocId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Delete request for document workspace: {}", spaceDocId);
        
        try {
            String response = projectWorkSpaceService
                    .deleteProjectDocSpaceDocs(userId, projectId, documentId, spaceDocId);
            log.info("REST - Successfully deleted document workspace: {}", spaceDocId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error deleting document workspace: {}", spaceDocId, e);
            throw e;
        }
    }

    /**
     * Download all project document workspaces as ZIP
     */
    @GetMapping("/document/download-all/{projectId}/{documentId}")
    @Operation(summary = "Download all project document workspaces", 
               description = "Downloads all workspaces for a project document as a ZIP file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ZIP file created successfully"),
        @ApiResponse(responseCode = "204", description = "No workspaces found"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Project document not found")
    })
    public ResponseEntity<byte[]> downloadAllProjectDocWorkspaces(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Project document ID", required = true) @PathVariable UUID documentId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Download all workspaces for project document: {}", documentId);
        
        try {
            ResponseEntity<byte[]> response = projectWorkSpaceService
                    .projectDocumentWorkSpaceFolder(userId, projectId, documentId);
            
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.warn("REST - No workspaces found for project document: {}", documentId);
            } else {
                log.info("REST - Successfully created ZIP file for project document: {}", documentId);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("REST - Error creating ZIP file for project document: {}", documentId, e);
            throw e;
        }
    }

    /**
     * View most recent project document workspaces
     */
    @GetMapping("/document/recent/{projectId}/{documentId}")
    @Operation(summary = "View recent project document workspaces", 
               description = "Retrieves the most recently created workspaces for a project document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recent workspaces retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Project document not found")
    })
    public ResponseEntity<List<ProjectDocumentWorkSpaceDocs>> viewRecentProjectDocWorkspaces(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Project document ID", required = true) @PathVariable UUID documentId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Viewing recent workspaces for project document: {}", documentId);
        
        try {
            List<ProjectDocumentWorkSpaceDocs> response = projectWorkSpaceService
                    .viewMostRecentProjectDocuments(userId, projectId, documentId);
            log.info("REST - Retrieved {} recent workspaces for project document: {}", 
                    response.size(), documentId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing recent workspaces for project document: {}", 
                     documentId, e);
            throw e;
        }
    }

    /**
     * View project document workspaces by type
     */
    @GetMapping("/document/by-type/{projectId}/{documentId}")
    @Operation(summary = "View project document workspaces by type", 
               description = "Retrieves workspaces for a project document filtered by document type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspaces retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Project document not found")
    })
    public ResponseEntity<List<ProjectDocumentWorkSpaceDocs>> viewProjectDocWorkspacesByType(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Project document ID", required = true) @PathVariable UUID documentId,
            @Parameter(description = "Document type", required = true) @RequestParam DocType docType) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Viewing workspaces by type: {} for project document: {}", 
                docType, documentId);
        
        try {
            List<ProjectDocumentWorkSpaceDocs> response = projectWorkSpaceService
                    .viewProjectDocumentByDocType(userId, projectId, documentId, docType);
            log.info("REST - Found {} workspaces of type {} for project document: {}", 
                    response.size(), docType, documentId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing workspaces by type for project document: {}", 
                     documentId, e);
            throw e;
        }
    }

    /**
     * View last viewed project document workspaces
     */
    @GetMapping("/document/last-viewed/{projectId}/{documentId}")
    @Operation(summary = "View last viewed project document workspaces", 
               description = "Retrieves workspaces sorted by last viewed timestamp")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspaces retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Project document not found")
    })
    public ResponseEntity<List<ProjectDocumentWorkSpaceDocs>> viewLastViewedProjectDocWorkspaces(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Project document ID", required = true) @PathVariable UUID documentId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Viewing last viewed workspaces for project document: {}", documentId);
        
        try {
            List<ProjectDocumentWorkSpaceDocs> response = projectWorkSpaceService
                    .viewLastViewedProjectDocuments(userId, projectId, documentId);
            log.info("REST - Retrieved {} last viewed workspaces for project document: {}", 
                    response.size(), documentId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing last viewed workspaces for project document: {}", 
                     documentId, e);
            throw e;
        }
    }

    /**
     * Search project document workspaces
     */
    @GetMapping("/document/search/{projectId}/{documentId}")
    @Operation(summary = "Search project document workspaces", 
               description = "Searches workspaces for a project document by keyword")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid search keyword"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Project document not found")
    })
    public ResponseEntity<List<ProjectDocumentWorkSpaceDocs>> searchProjectDocWorkspaces(
            @Parameter(description = "Project ID", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Project document ID", required = true) @PathVariable UUID documentId,
            @Parameter(description = "Search keyword", required = true) @RequestParam String keyword) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Searching workspaces for project document: {} with keyword: '{}'", 
                documentId, keyword);
        
        try {
            List<ProjectDocumentWorkSpaceDocs> response = projectWorkSpaceService
                    .generalProjectDocumentSearch(userId, projectId, documentId, keyword);
            log.info("REST - Found {} workspaces matching '{}' for project document: {}", 
                    response.size(), keyword, documentId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error searching workspaces for project document: {} with keyword: '{}'", 
                     documentId, keyword, e);
            throw e;
        }
    }
}