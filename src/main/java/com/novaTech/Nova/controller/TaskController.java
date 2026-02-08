package com.novaTech.Nova.controller;

import com.novaTech.Nova.DTO.TaskCreationDTO;
import com.novaTech.Nova.DTO.TaskResponseDTO;
import com.novaTech.Nova.DTO.TaskResponseForProjectDto;
import com.novaTech.Nova.DTO.TaskUpdateDTO;
import com.novaTech.Nova.Entities.Enums.TaskStatus;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Services.TaskService;
import com.novaTech.Nova.Services.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class TaskController {

    private final UserRegistrationService userRegistrationService;
    private final TaskService taskService;


    @PostMapping("/create/user")
    public ResponseEntity<?> addUserTask(@RequestBody TaskCreationDTO dto, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            TaskResponseDTO response = taskService.createUserTask(user.getId(), dto);

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
            @RequestBody TaskCreationDTO dto,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            TaskResponseForProjectDto response = taskService.createProjectTask(projectId, user.getId(), dto);

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
    public ResponseEntity<?> viewAllUserTasks(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            List<TaskResponseDTO> tasks = taskService.viewAllUserTasks(user.getId());
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            log.error("Error fetching user tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/user/{taskId}")
    public ResponseEntity<?> viewUserTaskById(@PathVariable UUID taskId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            TaskResponseDTO task = taskService.viewTaskByIdForUser(taskId, user.getId());
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
    public ResponseEntity<?> viewUserTasksByStatus(@PathVariable TaskStatus status, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            List<TaskResponseDTO> tasks = taskService.viewTasksByStatus(user.getId(), status);
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            log.error("Error fetching user tasks by status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/project/{projectId}/all")
    public ResponseEntity<?> viewAllProjectTasks(@PathVariable UUID projectId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            List<TaskResponseForProjectDto> tasks = taskService.viewAllProjectTasks(projectId, user.getId());
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            log.error("Error fetching project tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/project/{projectId}/{taskId}")
    public ResponseEntity<?> viewProjectTaskById(@PathVariable UUID projectId, @PathVariable UUID taskId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            TaskResponseForProjectDto task = taskService.viewTaskByIdForProject(taskId, projectId, user.getId());
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
    public ResponseEntity<?> viewProjectTasksByStatus(@PathVariable UUID projectId, @PathVariable TaskStatus status, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            List<TaskResponseForProjectDto> tasks = taskService.viewTasksByStatusForProject(projectId, user.getId(), status);
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            log.error("Error fetching project tasks by status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/user/search")
    public ResponseEntity<?> searchUserTasks(@RequestParam String keyword, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            List<TaskResponseDTO> tasks = taskService.searchUserTasks(user.getId(), keyword);
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            log.error("Error searching user tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/project/{projectId}/search")
    public ResponseEntity<?> searchProjectTasks(@PathVariable UUID projectId, @RequestParam String keyword, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            List<TaskResponseForProjectDto> tasks = taskService.searchProjectTasks(projectId, keyword, user.getId());
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            log.error("Error searching project tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/user/{taskId}")
    public ResponseEntity<?> updateUserTask(@PathVariable UUID taskId, @RequestBody TaskUpdateDTO dto, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            TaskResponseDTO response = taskService.updateUserTask(taskId, user.getId(), dto);
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
    public ResponseEntity<?> deleteUserTask(@PathVariable UUID taskId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            taskService.deleteUserTask(taskId, user.getId());
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
    public ResponseEntity<?> updateProjectTask(@PathVariable UUID projectId, @PathVariable UUID taskId, @RequestBody TaskUpdateDTO dto, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            TaskResponseForProjectDto response = taskService.updateProjectTask(taskId, projectId, user.getId(), dto);
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
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            taskService.deleteProjectTask(taskId, projectId, user.getId());
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
