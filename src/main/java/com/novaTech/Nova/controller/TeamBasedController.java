package com.novaTech.Nova.controller;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.ProjectStatus;
import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.TeamBasedServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
public class TeamBasedController {

    private final TeamBasedServices teamBasedServices;

    // ==================== UTILITY METHOD ====================

    private UserPrincipal userPrincipal() {
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
                userPrincipal.getUsername(), userPrincipal.getUserId());

        return userPrincipal;
    }

    // ==================== TEAM PROJECT ENDPOINTS ====================

    /**
     * Create a new team project
     * POST /api/v1/team/{teamId}/projects
     */
    @PostMapping(value = "/{teamId}/projects", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TeamProjectResponse> createTeamProject(
            @PathVariable UUID teamId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "status", required = false) ProjectStatus status,
            @RequestParam(value = "documents", required = false) List<MultipartFile> documents,
            @RequestParam(value = "documentDescription", required = false) String documentDescription) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            TeamProjectCreateDTO dto = new TeamProjectCreateDTO(
                    title,
                    description,
                    java.time.LocalDate.parse(startDate),
                    java.time.LocalDate.parse(endDate),
                    status,
                    documents,
                    documentDescription
            );

            log.info("Creating team project for user: {}, team: {}", userId, teamId);
            TeamProjectResponse response = teamBasedServices.createTeamProject(dto, userId, teamId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IOException e) {
            log.error("Error uploading documents: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Error creating team project: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Update a team project
     * PUT /api/v1/team/{teamId}/projects/{projectId}
     */
    @PutMapping(value = "/{teamId}/projects/{projectId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TeamProjectResponse> updateTeamProject(
            @PathVariable UUID teamId,
            @PathVariable UUID projectId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "status", required = false) ProjectStatus status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "documents", required = false) List<MultipartFile> documents,
            @RequestParam(value = "documentDescription", required = false) String documentDescription) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            TeamProjectUpdateDTO dto = TeamProjectUpdateDTO.builder()
                    .title(title)
                    .description(description)
                    .status(status)
                    .startDate(startDate != null ? java.time.LocalDate.parse(startDate) : null)
                    .endDate(endDate != null ? java.time.LocalDate.parse(endDate) : null)
                    .documents(documents)
                    .documentDescription(documentDescription)
                    .build();

            log.info("Updating team project: {} for user: {}", projectId, userId);
            TeamProjectResponse response = teamBasedServices.updateTeamProject(projectId, userId, dto, teamId);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Error uploading documents: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Error updating team project: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get a specific project by ID
     * GET /api/v1/team/{teamId}/projects/{projectId}
     */
    @GetMapping("/{teamId}/projects/{projectId}")
    public ResponseEntity<TeamProjectResponse> getProjectById(
            @PathVariable UUID teamId,
            @PathVariable UUID projectId) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Fetching project: {} for user: {}", projectId, userId);
            TeamProjectResponse response = teamBasedServices.getProjectById(projectId, userId, teamId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching project: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get all projects for the authenticated user
     * GET /api/v1/team/projects
     */
    @GetMapping("/projects")
    public ResponseEntity<List<TeamProjectResponse>> getAllProjects() {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Fetching all projects for user: {}", userId);
            List<TeamProjectResponse> response = teamBasedServices.getAllProjects(userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching projects: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a project
     * DELETE /api/v1/team/{teamId}/projects/{projectId}
     */
    @DeleteMapping("/{teamId}/projects/{projectId}")
    public ResponseEntity<String> deleteProject(
            @PathVariable UUID teamId,
            @PathVariable UUID projectId) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Deleting project: {} for user: {}", projectId, userId);
            teamBasedServices.deleteProject(projectId, userId);

            return ResponseEntity.ok("Project deleted successfully");

        } catch (Exception e) {
            log.error("Error deleting project: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Get project summary for a team
     * GET /api/v1/team/{teamId}/projects/summary
     */
    @GetMapping("/{teamId}/projects/summary")
    public ResponseEntity<List<TeamProjectSummaryDto>> getProjectSummary(
            @PathVariable UUID teamId) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Fetching project summary for team: {}, user: {}", teamId, userId);
            List<TeamProjectSummaryDto> response = teamBasedServices.viewAllProjectSummary(userId, teamId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching project summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get project count
     * GET /api/v1/team/{teamId}/projects/count
     */
    @GetMapping("/{teamId}/projects/count")
    public ResponseEntity<Long> getProjectCount(@PathVariable UUID teamId) {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Fetching project count for team: {}, user: {}", teamId, userId);
            long count = teamBasedServices.getProjectCount(userId, teamId);

            return ResponseEntity.ok(count);

        } catch (Exception e) {
            log.error("Error fetching project count: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get project count by status
     * GET /api/v1/team/{teamId}/projects/count/status/{status}
     */
    @GetMapping("/{teamId}/projects/count/status/{status}")
    public ResponseEntity<Long> getProjectCountByStatus(
            @PathVariable UUID teamId,
            @PathVariable ProjectStatus status) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Fetching project count by status {} for team: {}, user: {}", status, teamId, userId);
            long count = teamBasedServices.countBasedOnStatusProjects(userId, teamId, status);

            return ResponseEntity.ok(count);

        } catch (Exception e) {
            log.error("Error fetching project count by status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get overdue projects
     * GET /api/v1/team/{teamId}/projects/overdue
     */
    @GetMapping("/{teamId}/projects/overdue")
    public ResponseEntity<List<TeamProjectSummaryDto>> getOverdueProjects(
            @PathVariable UUID teamId) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Fetching overdue projects for team: {}, user: {}", teamId, userId);
            List<TeamProjectSummaryDto> response = teamBasedServices.viewOverdueProjects(userId, teamId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching overdue projects: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== DOCUMENT ENDPOINTS ====================

    /**
     * Upload documents to a project
     * POST /api/v1/team/{teamId}/projects/{projectId}/documents
     */
    @PostMapping(value = "/{teamId}/projects/{projectId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<TeamProjectDocumentResponseDTO>> uploadDocuments(
            @PathVariable UUID teamId,
            @PathVariable UUID projectId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "description", required = false) String description) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Uploading {} documents to project: {}", files.size(), projectId);
            List<TeamProjectDocumentResponseDTO> response = teamBasedServices.uploadDocument(
                    projectId, userId, files, description, teamId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IOException e) {
            log.error("Error uploading documents: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Error uploading documents: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get all documents for a project
     * GET /api/v1/team/{teamId}/projects/{projectId}/documents
     */
    @GetMapping("/{teamId}/projects/{projectId}/documents")
    public ResponseEntity<List<TeamProjectDocumentResponseDTO>> getProjectDocuments(
            @PathVariable UUID teamId,
            @PathVariable UUID projectId) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Fetching documents for project: {}", projectId);
            List<TeamProjectDocumentResponseDTO> response = teamBasedServices.getProjectDocuments(projectId, userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching project documents: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all documents for the authenticated user
     * GET /api/v1/team/documents
     */
    @GetMapping("/documents")
    public ResponseEntity<List<TeamProjectDocumentResponseDTO>> getAllDocuments() {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Fetching all documents for user: {}", userId);
            List<TeamProjectDocumentResponseDTO> response = teamBasedServices.getAllDocumentsForUser(userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching documents: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a document
     * DELETE /api/v1/team/{teamId}/projects/{projectId}/documents/{documentId}
     */
    @DeleteMapping("/{teamId}/projects/{projectId}/documents/{documentId}")
    public ResponseEntity<String> deleteDocument(
            @PathVariable UUID teamId,
            @PathVariable UUID projectId,
            @PathVariable UUID documentId) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Deleting document: {} from project: {}", documentId, projectId);
            String response = teamBasedServices.deleteDocument(documentId, userId, teamId, projectId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting document: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // ==================== TASK ENDPOINTS ====================

    /**
     * Create a task for a project
     * POST /api/v1/team/{teamId}/projects/{projectId}/tasks
     */
    @PostMapping("/{teamId}/projects/{projectId}/tasks")
    public ResponseEntity<TeamTaskResponseDTO> createTask(
            @PathVariable UUID teamId,
            @PathVariable UUID projectId,
            @RequestBody TeamTaskCreateDTO dto) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Creating task for project: {} by user: {}", projectId, userId);
            TeamTaskResponseDTO response = teamBasedServices.createTask(projectId, userId, teamId, dto);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error creating task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Update a task
     * PUT /api/v1/team/{teamId}/tasks/{taskId}
     */
    @PutMapping("/{teamId}/tasks/{taskId}")
    public ResponseEntity<TeamTaskResponseDTO> updateTask(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId,
            @RequestBody TeamTaskUpdateDTO dto) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Updating task: {} by user: {}", taskId, userId);
            TeamTaskResponseDTO response = teamBasedServices.updateTask(taskId, userId, dto);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Delete a task
     * DELETE /api/v1/team/{teamId}/tasks/{taskId}
     */
    @DeleteMapping("/{teamId}/tasks/{taskId}")
    public ResponseEntity<String> deleteTask(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Deleting task: {} by user: {}", taskId, userId);
            String response = teamBasedServices.deleteTask(taskId, userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Get all tasks for a project
     * GET /api/v1/team/{teamId}/projects/{projectId}/tasks
     */
    @GetMapping("/{teamId}/projects/{projectId}/tasks")
    public ResponseEntity<List<TeamTaskResponseDTO>> getProjectTasks(
            @PathVariable UUID teamId,
            @PathVariable UUID projectId) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Fetching tasks for project: {}", projectId);
            List<TeamTaskResponseDTO> response = teamBasedServices.getProjectTasks(projectId, userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching project tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get tasks assigned to the authenticated user
     * GET /api/v1/team/{teamId}/tasks/my-tasks
     */
    @GetMapping("/{teamId}/tasks/my-tasks")
    public ResponseEntity<List<TeamTaskResponseDTO>> getMyTasks(
            @PathVariable UUID teamId) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Fetching tasks for user: {} in team: {}", userId, teamId);
            List<TeamTaskResponseDTO> response = teamBasedServices.getUserTasks(userId, teamId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching user tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get overdue tasks for the authenticated user
     * GET /api/v1/team/{teamId}/tasks/overdue
     */
    @GetMapping("/{teamId}/tasks/overdue")
    public ResponseEntity<List<TeamTaskResponseDTO>> getOverdueTasks(
            @PathVariable UUID teamId) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Fetching overdue tasks for user: {} in team: {}", userId, teamId);
            List<TeamTaskResponseDTO> response = teamBasedServices.getOverdueTasks(userId, teamId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching overdue tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== REMINDER ENDPOINTS ====================

    /**
     * Create a reminder
     * POST /api/v1/team/{teamId}/reminders
     */
    @PostMapping("/{teamId}/reminders")
    public ResponseEntity<TeamReminderResponseDTO> createReminder(
            @PathVariable UUID teamId,
            @RequestBody TeamReminderCreateDTO dto) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Creating reminder for user: {} in team: {}", userId, teamId);
            TeamReminderResponseDTO response = teamBasedServices.createReminder(userId, teamId, dto);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error creating reminder: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Update a reminder
     * PUT /api/v1/team/{teamId}/reminders/{reminderId}
     */
    @PutMapping("/{teamId}/reminders/{reminderId}")
    public ResponseEntity<TeamReminderResponseDTO> updateReminder(
            @PathVariable UUID teamId,
            @PathVariable UUID reminderId,
            @RequestBody TeamReminderUpdateDTO dto) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Updating reminder: {} by user: {}", reminderId, userId);
            TeamReminderResponseDTO response = teamBasedServices.updateReminder(reminderId, userId, dto);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating reminder: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Delete a reminder
     * DELETE /api/v1/team/{teamId}/reminders/{reminderId}
     */
    @DeleteMapping("/{teamId}/reminders/{reminderId}")
    public ResponseEntity<String> deleteReminder(
            @PathVariable UUID teamId,
            @PathVariable UUID reminderId) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Deleting reminder: {} by user: {}", reminderId, userId);
            String response = teamBasedServices.deleteReminder(reminderId, userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting reminder: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Get all reminders for the authenticated user in a team
     * GET /api/v1/team/{teamId}/reminders
     */
    @GetMapping("/{teamId}/reminders")
    public ResponseEntity<List<TeamReminderResponseDTO>> getUserReminders(
            @PathVariable UUID teamId) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Fetching reminders for user: {} in team: {}", userId, teamId);
            List<TeamReminderResponseDTO> response = teamBasedServices.getUserReminders(userId, teamId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching user reminders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get reminders for a specific project
     * GET /api/v1/team/{teamId}/projects/{projectId}/reminders
     */
    @GetMapping("/{teamId}/projects/{projectId}/reminders")
    public ResponseEntity<List<TeamReminderResponseDTO>> getProjectReminders(
            @PathVariable UUID teamId,
            @PathVariable UUID projectId) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Fetching reminders for project: {}", projectId);
            List<TeamReminderResponseDTO> response = teamBasedServices.getProjectReminders(projectId, userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching project reminders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get reminders for a specific task
     * GET /api/v1/team/{teamId}/tasks/{taskId}/reminders
     */
    @GetMapping("/{teamId}/tasks/{taskId}/reminders")
    public ResponseEntity<List<TeamReminderResponseDTO>> getTaskReminders(
            @PathVariable UUID teamId,
            @PathVariable UUID taskId) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Fetching reminders for task: {}", taskId);
            List<TeamReminderResponseDTO> response = teamBasedServices.getTaskReminders(taskId, userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching task reminders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== MESSAGING ENDPOINTS ====================

    /**
     * Send a message to team members
     * POST /api/v1/team/{teamId}/messages/send
     */
    @PostMapping("/{teamId}/messages/send")
    public ResponseEntity<String> sendTeamMessage(
            @PathVariable UUID teamId,
            @RequestBody TeamMessageDTO dto) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Sending team message from user: {} to team: {}", userId, teamId);
            String response = teamBasedServices.sendTeamMessage(userId, teamId, dto);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error sending team message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Send a project update to team members
     * POST /api/v1/team/{teamId}/projects/{projectId}/send-update
     */
    @PostMapping("/{teamId}/projects/{projectId}/send-update")
    public ResponseEntity<String> sendProjectUpdate(
            @PathVariable UUID teamId,
            @PathVariable UUID projectId,
            @RequestBody ProjectUpdateMessageDTO dto) {

        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            log.info("Sending project update for project: {} from user: {}", projectId, userId);
            String response = teamBasedServices.sendProjectUpdate(projectId, userId, dto);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error sending project update: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}