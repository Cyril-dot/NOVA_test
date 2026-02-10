package com.novaTech.Nova.controller;

import com.novaTech.Nova.DTO.TaskCreationDTO;
import com.novaTech.Nova.DTO.TaskResponseDTO;
import com.novaTech.Nova.DTO.TaskResponseForProjectDto;
import com.novaTech.Nova.DTO.TaskUpdateDTO;
import com.novaTech.Nova.Entities.Enums.TaskStatus;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.TaskService;
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
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

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


    @PostMapping("/create/user")
    public ResponseEntity<?> addUserTask(@RequestBody TaskCreationDTO dto) {
        try {
            UserPrincipal userPrincipal = userPrincipal();
            UUID userId = userPrincipal.getUserId();
            TaskResponseDTO response = taskService.createUserTask(userId, dto);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Task created successfully",
                    "task", response
            ));
        } catch (RuntimeException e) {
            log.error("Error creating task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }


    @PostMapping("/create/project/{projectId}")
    public ResponseEntity<?> addProjectTask(
            @PathVariable UUID projectId,
            @RequestBody TaskCreationDTO dto) {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            TaskResponseForProjectDto response = taskService.createProjectTask(projectId, userId, dto);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Task created successfully for project",
                    "task", response
            ));
        } catch (RuntimeException e) {
            log.error("Error creating project task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/user/all")
    public ResponseEntity<?> viewAllUserTasks() {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            List<TaskResponseDTO> tasks = taskService.viewAllUserTasks(userId);
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            log.error("Error fetching user tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/user/{taskId}")
    public ResponseEntity<?> viewUserTaskById(@PathVariable UUID taskId) {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();
            TaskResponseDTO task = taskService.viewTaskByIdForUser(taskId,userId);
            if (task == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "Task not found"
                ));
            }
            return ResponseEntity.ok(task);

        } catch (RuntimeException e) {
            log.error("Error fetching user task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/user/status/{status}")
    public ResponseEntity<?> viewUserTasksByStatus(@PathVariable TaskStatus status) {
        try {
            UserPrincipal principal =  userPrincipal();
            UUID userId = principal.getUserId();

            List<TaskResponseDTO> tasks = taskService.viewTasksByStatus(userId, status);
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            log.error("Error fetching user tasks by status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/project/{projectId}/all")
    public ResponseEntity<?> viewAllProjectTasks(@PathVariable UUID projectId) {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            List<TaskResponseForProjectDto> tasks = taskService.viewAllProjectTasks(projectId, userId);
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            log.error("Error fetching project tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/project/{projectId}/{taskId}")
    public ResponseEntity<?> viewProjectTaskById(@PathVariable UUID projectId, @PathVariable UUID taskId) {
        try {
            UserPrincipal principal =  userPrincipal();
            UUID userId = principal.getUserId();
            TaskResponseForProjectDto task = taskService.viewTaskByIdForProject(taskId, projectId,userId);
            if (task == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "Task not found"
                ));
            }
            return ResponseEntity.ok(task);

        } catch (RuntimeException e) {
            log.error("Error fetching project task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/project/{projectId}/status/{status}")
    public ResponseEntity<?> viewProjectTasksByStatus(@PathVariable UUID projectId, @PathVariable TaskStatus status) {
        try {
            UserPrincipal principal =  userPrincipal();
            UUID userId = principal.getUserId();
            List<TaskResponseForProjectDto> tasks = taskService.viewTasksByStatusForProject(projectId, userId, status);
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            log.error("Error fetching project tasks by status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/user/search")
    public ResponseEntity<?> searchUserTasks(@RequestParam String keyword) {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();

            List<TaskResponseDTO> tasks = taskService.searchUserTasks(userId, keyword);
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            log.error("Error searching user tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/project/{projectId}/search")
    public ResponseEntity<?> searchProjectTasks(@PathVariable UUID projectId, @RequestParam String keyword) {
        try {
            UserPrincipal principal =  userPrincipal();
            UUID userId = principal.getUserId();
            List<TaskResponseForProjectDto> tasks = taskService.searchProjectTasks(projectId, keyword, userId);
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            log.error("Error searching project tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/user/{taskId}")
    public ResponseEntity<?> updateUserTask(@PathVariable UUID taskId, @RequestBody TaskUpdateDTO dto) {
        try {
            UserPrincipal principal =  userPrincipal();
            UUID userId = principal.getUserId();

            TaskResponseDTO response = taskService.updateUserTask(taskId, userId, dto);
            return ResponseEntity.ok(Map.of(
                    "message", "Task updated successfully",
                    "task", response
            ));

        } catch (RuntimeException e) {
            log.error("Error updating user task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/user/{taskId}")
    public ResponseEntity<?> deleteUserTask(@PathVariable UUID taskId) {
        try {
            UserPrincipal principal =  userPrincipal();
            UUID userId = principal.getUserId();

            taskService.deleteUserTask(taskId, userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Task deleted successfully"
            ));

        } catch (RuntimeException e) {
            log.error("Error deleting user task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/project/{projectId}/{taskId}")
    public ResponseEntity<?> updateProjectTask(@PathVariable UUID projectId, @PathVariable UUID taskId, @RequestBody TaskUpdateDTO dto) {
        try {
            UserPrincipal principal =  userPrincipal();
            UUID userId = principal.getUserId();
            TaskResponseForProjectDto response = taskService.updateProjectTask(taskId, projectId, userId, dto);
            return ResponseEntity.ok(Map.of(
                    "message", "Task updated successfully",
                    "task", response
            ));

        } catch (RuntimeException e) {
            log.error("Error updating project task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/project/{projectId}/{taskId}")
    public ResponseEntity<?> deleteProjectTask(@PathVariable UUID projectId, @PathVariable UUID taskId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            UserPrincipal principal = userPrincipal();
            UUID userId = principal.getUserId();
            taskService.deleteProjectTask(taskId, projectId, userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Task deleted successfully"
            ));

        } catch (RuntimeException e) {
            log.error("Error deleting project task: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
