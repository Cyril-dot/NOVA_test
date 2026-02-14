package com.novaTech.Nova.controller.WorkSpace;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.DocType;
import com.novaTech.Nova.Entities.workSpace.WorkSpaceDocs;
import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.workSpace.UserWorkSpace;
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

/**
 * REST Controller for User Workspace operations
 * Handles all workspace-related endpoints for individual users
 * 
 * @author NovaTech
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/user/workspace")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Workspace", description = "User workspace management endpoints")
@PreAuthorize("isAuthenticated()")
public class UserWorkSpaceController {

    private final UserWorkSpace userWorkSpaceService;


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


    @PostMapping("/create")
    @Operation(summary = "Create a new user workspace", description = "Creates a new workspace for the specified user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Workspace created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<WorkSpaceCreationResponse> createWorkspace(@Valid @RequestBody WorkSpaceCreationDto creationDto) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Received request to create workspace for user: {}", userId);
        log.debug("REST - Workspace creation details - Title: {}, Description: {}", 
                 creationDto.title(), creationDto.description());
        
        try {
            WorkSpaceCreationResponse response = userWorkSpaceService.createWorkspaceForUser(userId, creationDto);
            log.info("REST - Successfully created workspace with ID: {} for user: {}", 
                    response.id(), userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("REST - Error creating workspace for user: {}", userId, e);
            throw e;
        }
    }



    @PostMapping("/template/{docId}")
    @Operation(summary = "Create workspace template", description = "Creates a code/document template for the specified workspace")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid document type"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace or user not found")
    })
    public ResponseEntity<ActiveWorkSpaceDocs> createTemplate(
            @Parameter(description = "Document type", required = true) @RequestParam DocType docType,
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Creating template for workspace: {}, user: {}, docType: {}", 
                docId, userId, docType);
        
        try {
            ActiveWorkSpaceDocs response = userWorkSpaceService.getWorkSpaceDocsTemplate(docType, userId, docId);
            log.info("REST - Successfully created {} template for workspace: {}", docType, docId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error creating template for workspace: {}, user: {}", docId, userId, e);
            throw e;
        }
    }



    @GetMapping("/view/{docId}")
    @Operation(summary = "View workspace document", description = "Retrieves the content of a workspace document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<ViewWorkSpaceDocsData> viewWorkspace(
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Viewing workspace: {} for user: {}", docId, userId);
        
        try {
            ViewWorkSpaceDocsData response = userWorkSpaceService.viewDoc(userId, docId);
            log.info("REST - Successfully retrieved workspace: {} for user: {}", docId, userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing workspace: {} for user: {}", docId, userId, e);
            throw e;
        }
    }



    @PutMapping("/work/{docId}")
    @Operation(summary = "Work in workspace", description = "Updates workspace content by injecting new content into template")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<ActiveWorkSpaceDocs> workInWorkspace(
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId,
            @Valid @RequestBody UpdateWorkSpaceDocsDto updateDto) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Working in workspace: {} for user: {}", docId, userId);
        log.debug("REST - Content length: {} characters", updateDto.content().length());
        
        try {
            ActiveWorkSpaceDocs response = userWorkSpaceService.workInSpaceDocs(userId, docId, updateDto);
            log.info("REST - Successfully updated workspace: {} for user: {}", docId, userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error working in workspace: {} for user: {}", docId, userId, e);
            throw e;
        }
    }



    @PatchMapping("/continue/{docId}")
    @Operation(summary = "Continue workspace work", description = "Appends new content to existing workspace content")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<ActiveWorkSpaceDocs> continueWork(
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId,
            @Valid @RequestBody UpdateWorkSpaceDocsDto updateDto) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Continuing work on workspace: {} for user: {}", docId, userId);
        log.debug("REST - Appending content length: {} characters", updateDto.content().length());
        
        try {
            ActiveWorkSpaceDocs response = userWorkSpaceService.updateWorkSpaceExistingDocs(userId, docId, updateDto);
            log.info("REST - Successfully continued work on workspace: {} for user: {}", docId, userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error continuing work on workspace: {} for user: {}", docId, userId, e);
            throw e;
        }
    }



    @GetMapping("/download/{docId}")
    @Operation(summary = "Download workspace file", description = "Downloads the workspace document as a file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<byte[]> downloadWorkspace(
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId) {
        
       UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Download request for workspace: {} by user: {}", docId, userId);
        
        try {

            ResponseEntity<byte[]> response = userWorkSpaceService.downloadFile(docId, userId);
            log.info("REST - Successfully prepared download for workspace: {}", docId);
            return response;
            
        } catch (Exception e) {
            log.error("REST - Error downloading workspace: {} for user: {}", docId, userId, e);
            throw e;
        }
    }


    @DeleteMapping("/delete/{docId}")
    @Operation(summary = "Delete workspace", description = "Permanently deletes a workspace document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access"),
        @ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ResponseEntity<String> deleteWorkspace(
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Delete request for workspace: {} by user: {}", docId, userId);
        
        try {
            String response = userWorkSpaceService.deleteDocs(userId, docId);
            log.info("REST - Successfully deleted workspace: {} for user: {}", docId, userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error deleting workspace: {} for user: {}", docId, userId, e);
            throw e;
        }
    }


    @GetMapping("/download-all")
    @Operation(summary = "Download all workspaces", description = "Downloads all user workspaces as a single ZIP file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ZIP file created successfully"),
        @ApiResponse(responseCode = "204", description = "No workspaces found"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Error creating ZIP file")
    })
    public ResponseEntity<byte[]> downloadAllWorkspaces() {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        
        log.info("REST - Download all workspaces request for user: {}", userId);
        
        try {
            ResponseEntity<byte[]> response = userWorkSpaceService.workSpaceFolder(userId);
            
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.warn("REST - No workspaces found for user: {}", userId);
            } else {
                log.info("REST - Successfully created ZIP file for user: {}", userId);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("REST - Error creating ZIP file for user: {}", userId, e);
            throw e;
        }
    }


    @GetMapping("/by-type")
    @Operation(summary = "View workspaces by type", description = "Retrieves all workspaces of a specific document type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspaces retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<UserWorkSpaceDocs>> viewWorkspacesByType(@Parameter(description = "Document type", required = true) @RequestParam DocType docType) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Viewing workspaces by type: {} for user: {}", docType, userId);
        
        try {
            List<UserWorkSpaceDocs> response = userWorkSpaceService.viewDocsByType(userId, docType);
            log.info("REST - Found {} workspaces of type {} for user: {}", 
                    response.size(), docType, userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing workspaces by type for user: {}", userId, e);
            throw e;
        }
    }



    @GetMapping("/search")
    @Operation(summary = "Search workspaces", description = "Searches workspaces by keyword in title or description")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid search keyword"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<UserWorkSpaceDocs>> searchWorkspaces(
            @Parameter(description = "Search keyword", required = true) @RequestParam String keyword) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Searching workspaces for user: {} with keyword: '{}'", userId, keyword);
        
        try {
            List<UserWorkSpaceDocs> response = userWorkSpaceService.searchDocs(userId, keyword);
            log.info("REST - Found {} workspaces matching '{}' for user: {}", 
                    response.size(), keyword, userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error searching workspaces for user: {} with keyword: '{}'", 
                     userId, keyword, e);
            throw e;
        }
    }



    @GetMapping("/recent")
    @Operation(summary = "View recent workspaces", description = "Retrieves the 10 most recently created workspaces")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recent workspaces retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<UserWorkSpaceDocs>> viewRecentWorkspaces() {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Viewing recent workspaces for user: {}", userId);
        
        try {
            List<UserWorkSpaceDocs> response = userWorkSpaceService.viewRecentDocs(userId);
            log.info("REST - Retrieved {} recent workspaces for user: {}", response.size(), userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing recent workspaces for user: {}", userId, e);
            throw e;
        }
    }




    @GetMapping("/recently-accessed")
    @Operation(summary = "View recently accessed workspaces", description = "Retrieves the 10 most recently accessed workspaces")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recently accessed workspaces retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<UserWorkSpaceDocs>> viewRecentlyAccessedWorkspaces() {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Viewing recently accessed workspaces for user: {}", userId);
        
        try {
            List<UserWorkSpaceDocs> response = userWorkSpaceService.viewRecentlyAccessedDocs(userId);
            log.info("REST - Retrieved {} recently accessed workspaces for user: {}", 
                    response.size(), userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing recently accessed workspaces for user: {}", userId, e);
            throw e;
        }
    }
}