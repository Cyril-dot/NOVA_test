package com.novaTech.Nova.controller.WorkSpace;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.DocType;
import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.workSpace.TeamWorkSpaceService;
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
 * REST Controller for Team Workspace operations
 * Handles all workspace-related endpoints for team collaboration
 * Supports admin-only operations and member contributions with approval workflow
 * 
 * @author NovaTech
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/team/workspace")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Team Workspace", description = "Team workspace management and collaboration endpoints")
@PreAuthorize("isAuthenticated()")
public class TeamWorkSpaceController {

    private final TeamWorkSpaceService teamWorkSpaceService;

    // ==================== WORKSPACE CREATION (ADMIN ONLY) ====================

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


    @PostMapping("/create/{teamId}")
    @Operation(summary = "Create team workspace (Admin only)", 
               description = "Creates a new workspace for the team. Only team admins and owners can create workspaces.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Team workspace created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Unauthorized - user is not admin or owner"),
        @ApiResponse(responseCode = "404", description = "Team or user not found")
    })
    public ResponseEntity<WorkSpaceCreationResponse> createTeamWorkspace(
            @Parameter(description = "Team ID", required = true) @PathVariable UUID teamId,
            @Valid @RequestBody WorkSpaceCreationDto creationDto) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Admin {} creating team workspace for team: {}", userId, teamId);
        log.debug("REST - Workspace details - Title: {}, Description: {}", 
                 creationDto.title(), creationDto.description());
        
        try {
            WorkSpaceCreationResponse response = teamWorkSpaceService
                    .createTeamWorkspace(userId, teamId, creationDto);
            log.info("REST - Successfully created team workspace with ID: {} for team: {}", 
                    response.id(), teamId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("REST - Error creating team workspace for team: {}, admin: {}", 
                     teamId, userId, e);
            throw e;
        }
    }


    @PostMapping("/template/{teamId}/{docId}")
    @Operation(summary = "Create team workspace template (Admin only)", 
               description = "Creates a code/document template for team workspace. Admin access required.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid document type"),
        @ApiResponse(responseCode = "403", description = "Unauthorized - user is not admin or owner"),
        @ApiResponse(responseCode = "404", description = "Workspace, team, or user not found")
    })
    public ResponseEntity<ActiveWorkSpaceDocs> createTeamTemplate(
            @Parameter(description = "Document type", required = true) @RequestParam DocType docType,
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId,
            @Parameter(description = "Team ID", required = true) @PathVariable UUID teamId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Admin {} creating template for team workspace: {}, team: {}, docType: {}", 
                userId, docId, teamId, docType);
        
        try {
            ActiveWorkSpaceDocs response = teamWorkSpaceService
                    .createWorkSpaceTemplate(docType, userId, docId, teamId);
            log.info("REST - Successfully created {} template for team workspace: {}", 
                    docType, docId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error creating template for team workspace: {}, team: {}", 
                     docId, teamId, e);
            throw e;
        }
    }


    @PostMapping("/contribute/{teamId}/{docId}")
    @Operation(summary = "Submit contribution (Team members)", 
               description = "Team members can submit contributions which require admin approval before being merged")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Contribution submitted successfully, pending approval"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Unauthorized - user is not a team member"),
        @ApiResponse(responseCode = "404", description = "Workspace or team not found")
    })
    public ResponseEntity<WorkSpaceContributionResponse> submitContribution(
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId,
            @Parameter(description = "Team ID", required = true) @PathVariable UUID teamId,
            @Valid @RequestBody UpdateWorkSpaceDocsDto updateDto) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Team member {} submitting contribution to workspace: {}, team: {}", 
                userId, docId, teamId);
        log.debug("REST - Contribution content length: {} characters", updateDto.content().length());
        
        try {
            WorkSpaceContributionResponse response = teamWorkSpaceService
                    .submitContribution(userId, docId, teamId, updateDto);
            log.info("REST - Contribution submitted successfully with ID: {}, status: PENDING", 
                    response.getContributionId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("REST - Error submitting contribution for workspace: {}, team: {}", 
                     docId, teamId, e);
            throw e;
        }
    }



    @PatchMapping("/approve/{teamId}/{contributionId}")
    @Operation(summary = "Approve contribution (Admin only)", 
               description = "Admins can approve and merge pending contributions into the main workspace")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contribution approved and merged successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized - user is not admin or owner"),
        @ApiResponse(responseCode = "404", description = "Contribution or team not found")
    })
    public ResponseEntity<ActiveWorkSpaceDocs> approveContribution(
            @Parameter(description = "Contribution ID", required = true) @PathVariable Long contributionId,
            @Parameter(description = "Team ID", required = true) @PathVariable UUID teamId) {

        UserPrincipal principal = userPrincipal();
        UUID adminId = principal.getUserId();

        log.info("REST - Admin {} approving contribution: {}, team: {}", 
                adminId, contributionId, teamId);
        
        try {
            ActiveWorkSpaceDocs response = teamWorkSpaceService
                    .approveContribution(adminId, contributionId, teamId);
            log.info("REST - Successfully approved and merged contribution: {}", contributionId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error approving contribution: {}, team: {}", contributionId, teamId, e);
            throw e;
        }
    }


    @PatchMapping("/reject/{teamId}/{contributionId}")
    @Operation(summary = "Reject contribution (Admin only)", 
               description = "Admins can reject pending contributions with a reason")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contribution rejected successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid rejection reason"),
        @ApiResponse(responseCode = "403", description = "Unauthorized - user is not admin or owner"),
        @ApiResponse(responseCode = "404", description = "Contribution or team not found")
    })
    public ResponseEntity<String> rejectContribution(
            @Parameter(description = "Contribution ID", required = true) @PathVariable Long contributionId,
            @Parameter(description = "Team ID", required = true) @PathVariable UUID teamId,
            @Parameter(description = "Rejection reason", required = true) @RequestParam String reason) {

        UserPrincipal principal = userPrincipal();
        UUID adminId = principal.getUserId();

        log.info("REST - Admin {} rejecting contribution: {}, team: {}, reason: '{}'", 
                adminId, contributionId, teamId, reason);
        
        try {
            String response = teamWorkSpaceService
                    .rejectContribution(adminId, contributionId, teamId, reason);
            log.info("REST - Successfully rejected contribution: {}", contributionId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error rejecting contribution: {}, team: {}", contributionId, teamId, e);
            throw e;
        }
    }


    @GetMapping("/pending/{teamId}/{workspaceId}")
    @Operation(summary = "View pending contributions (Admin only)", 
               description = "Admins can view all pending contributions for a workspace awaiting approval")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pending contributions retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized - user is not admin or owner"),
        @ApiResponse(responseCode = "404", description = "Workspace or team not found")
    })
    public ResponseEntity<List<WorkSpaceContributionResponse>> viewPendingContributions(
            @Parameter(description = "Workspace ID", required = true) @PathVariable Long workspaceId,
            @Parameter(description = "Team ID", required = true) @PathVariable UUID teamId) {

        UserPrincipal principal = userPrincipal();
        UUID adminId = principal.getUserId();

        log.info("REST - Admin {} viewing pending contributions for workspace: {}, team: {}", 
                adminId, workspaceId, teamId);
        
        try {
            List<WorkSpaceContributionResponse> response = teamWorkSpaceService
                    .viewPendingContributions(adminId, workspaceId, teamId);
            log.info("REST - Retrieved {} pending contributions for workspace: {}", 
                    response.size(), workspaceId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing pending contributions for workspace: {}, team: {}", 
                     workspaceId, teamId, e);
            throw e;
        }
    }

    // ==================== VIEW WORKSPACE (ALL MEMBERS) ====================


    @GetMapping("/view/{teamId}/{docId}")
    @Operation(summary = "View team workspace (All members)", 
               description = "All team members can view team workspace content")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized - user is not a team member"),
        @ApiResponse(responseCode = "404", description = "Workspace or team not found")
    })
    public ResponseEntity<ViewWorkSpaceDocsData> viewTeamWorkspace(
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId,
            @Parameter(description = "Team ID", required = true) @PathVariable UUID teamId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Team member {} viewing workspace: {}, team: {}", userId, docId, teamId);
        
        try {
            ViewWorkSpaceDocsData response = teamWorkSpaceService
                    .viewTeamWorkspace(userId, docId, teamId);
            log.info("REST - Successfully retrieved team workspace: {} for user: {}", docId, userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing team workspace: {}, team: {}", docId, teamId, e);
            throw e;
        }
    }



    @GetMapping("/all/{teamId}")
    @Operation(summary = "View all team workspaces (All members)", 
               description = "All team members can view list of all workspaces in the team")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspaces retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized - user is not a team member"),
        @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<List<UserWorkSpaceDocs>> viewAllTeamWorkspaces(@Parameter(description = "Team ID", required = true) @PathVariable UUID teamId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Team member {} viewing all workspaces for team: {}", userId, teamId);
        
        try {
            List<UserWorkSpaceDocs> response = teamWorkSpaceService
                    .viewAllTeamWorkspaces(userId, teamId);
            log.info("REST - Retrieved {} workspaces for team: {}", response.size(), teamId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing all team workspaces for team: {}", teamId, e);
            throw e;
        }
    }



    @GetMapping("/by-type/{teamId}")
    @Operation(summary = "View team workspaces by type (All members)", 
               description = "All team members can view workspaces filtered by document type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspaces retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized - user is not a team member"),
        @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<List<UserWorkSpaceDocs>> viewTeamWorkspacesByType(
            @Parameter(description = "Team ID", required = true) @PathVariable UUID teamId,
            @Parameter(description = "Document type", required = true) @RequestParam DocType docType) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Team member {} viewing workspaces by type: {} for team: {}", 
                userId, docType, teamId);
        
        try {
            List<UserWorkSpaceDocs> response = teamWorkSpaceService
                    .viewTeamWorkspacesByType(userId, teamId, docType);
            log.info("REST - Found {} workspaces of type {} for team: {}", 
                    response.size(), docType, teamId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error viewing team workspaces by type for team: {}", teamId, e);
            throw e;
        }
    }

    // ==================== DOWNLOAD (ALL MEMBERS) ====================
    @GetMapping("/download/{teamId}/{docId}")
    @Operation(summary = "Download team workspace (All members)", 
               description = "All team members can download workspace files")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized - user is not a team member"),
        @ApiResponse(responseCode = "404", description = "Workspace or team not found")
    })
    public ResponseEntity<byte[]> downloadTeamWorkspace(
            @Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId,
            @Parameter(description = "Team ID", required = true) @PathVariable UUID teamId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Team member {} downloading workspace: {}, team: {}", 
                userId, docId, teamId);
        
        try {
            ResponseEntity<byte[]> response = teamWorkSpaceService
                    .downloadTeamWorkspaceFile(docId, userId, teamId);
            log.info("REST - Successfully prepared download for team workspace: {}", docId);
            return response;
            
        } catch (Exception e) {
            log.error("REST - Error downloading team workspace: {}, team: {}", docId, teamId, e);
            throw e;
        }
    }



    @GetMapping("/download-all/{teamId}")
    @Operation(summary = "Download all team workspaces (All members)", 
               description = "All team members can download all team workspaces as a single ZIP file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ZIP file created successfully"),
        @ApiResponse(responseCode = "204", description = "No workspaces found"),
        @ApiResponse(responseCode = "403", description = "Unauthorized - user is not a team member"),
        @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<byte[]> downloadAllTeamWorkspaces(@Parameter(description = "Team ID", required = true) @PathVariable UUID teamId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("REST - Team member {} downloading all workspaces for team: {}", userId, teamId);
        
        try {
            ResponseEntity<byte[]> response = teamWorkSpaceService
                    .downloadAllTeamWorkspaces(userId, teamId);
            
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.warn("REST - No workspaces found for team: {}", teamId);
            } else {
                log.info("REST - Successfully created ZIP file for team: {}", teamId);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("REST - Error creating ZIP file for team: {}", teamId, e);
            throw e;
        }
    }

    // ==================== DELETE (ADMIN ONLY) ====================
    @DeleteMapping("/delete/{teamId}/{docId}")
    @Operation(summary = "Delete team workspace (Admin only)", 
               description = "Only team admins and owners can permanently delete team workspaces")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized - user is not admin or owner"),
        @ApiResponse(responseCode = "404", description = "Workspace or team not found")
    })
    public ResponseEntity<String> deleteTeamWorkspace(@Parameter(description = "Workspace document ID", required = true) @PathVariable Long docId,
            @Parameter(description = "Team ID", required = true) @PathVariable UUID teamId) {

        UserPrincipal principal = userPrincipal();
        UUID adminId = principal.getUserId();

        log.info("REST - Admin {} deleting team workspace: {}, team: {}", adminId, docId, teamId);
        
        try {
            String response = teamWorkSpaceService.deleteTeamWorkspace(adminId, docId, teamId);
            log.info("REST - Successfully deleted team workspace: {} from team: {}", docId, teamId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("REST - Error deleting team workspace: {}, team: {}", docId, teamId, e);
            throw e;
        }
    }
}