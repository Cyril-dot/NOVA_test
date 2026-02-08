package com.novaTech.Nova.controller;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.TeamStatus;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Services.ProjectService;
import com.novaTech.Nova.Services.TaskService;
import com.novaTech.Nova.Services.UserRegistrationService;
import com.novaTech.Nova.Services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final UserRegistrationService userRegistrationService;
    private final UserService userService;

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            long totalProjects = projectService.getProjectCount(user.getId());
            long completedProjects = projectService.getCompletedProjectCount(user.getId());
            long inProgressProjects = projectService.getInProgressProjectCount(user.getId());

            long totalTasks = taskService.getTaskCount(user.getId());
            long completedTasks = taskService.getCompletedTaskCount(user.getId());
            long inProgressTasks = taskService.getInProgressTaskCount(user.getId());

            DashboardStatsDTO stats = DashboardStatsDTO.builder()
                    .totalProjects(totalProjects)
                    .completedProjects(completedProjects)
                    .inProgressProjects(inProgressProjects)
                    .totalTasks(totalTasks)
                    .completedTasks(completedTasks)
                    .inProgressTasks(inProgressTasks)
                    .build();

            return ResponseEntity.ok(stats);

        } catch (RuntimeException e) {
            log.error("Error fetching dashboard stats: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/projects")
    public ResponseEntity<?> getProjectSummaries(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            List<ProjectSummaryDTO> projects = projectService.viewAllProjectsSummary(user.getId());
            return ResponseEntity.ok(projects);

        } catch (RuntimeException e) {
            log.error("Error fetching project summaries: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/projects/overdue")
    public ResponseEntity<?> getOverdueProjects(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            List<ProjectSummaryDTO> projects = projectService.viewOverdueProjects(user.getId());
            return ResponseEntity.ok(projects);

        } catch (RuntimeException e) {
            log.error("Error fetching overdue projects: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/tasks")
    public ResponseEntity<?> getTaskSummaries(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            List<TaskSummaryDTO> tasks = taskService.viewAllTasksSummary(user.getId());
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            log.error("Error fetching task summaries: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/tasks/overdue")
    public ResponseEntity<?> getOverdueTasks(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            List<TaskSummaryDTO> tasks = taskService.viewOverdueTasks(user.getId());
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            log.error("Error fetching overdue tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ========================
    // USER & TEAM MANAGEMENT
    // ========================

    @PostMapping("/users/search")
    public ResponseEntity<?> searchUsers(@RequestBody SearchUserRequest request, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }
            return ResponseEntity.ok(userService.searchUser(request, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/teams")
    public ResponseEntity<?> createTeam(@RequestBody CreateTeamRequest request, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(userService.createTeam(request, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/teams/{teamId}/members")
    public ResponseEntity<?> addTeamMember(@PathVariable UUID teamId, @RequestBody AddTeamRequest request, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }
            return ResponseEntity.ok(userService.addMember(teamId, request, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/teams/{teamId}/members")
    public ResponseEntity<?> viewTeamMembers(@PathVariable UUID teamId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }
            return ResponseEntity.ok(userService.viewTeamMembers(teamId, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/teams/{teamId}/members/details")
    public ResponseEntity<?> viewTeamMembersWithRole(@PathVariable UUID teamId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }
            return ResponseEntity.ok(userService.viewTeamMembersWithRole(teamId, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/teams/{teamId}/members/{memberId}")
    public ResponseEntity<?> removeTeamMember(@PathVariable UUID teamId, @PathVariable UUID memberId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }
            return ResponseEntity.ok(userService.removeMember(teamId, memberId, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/teams/{teamId}")
    public ResponseEntity<?> deleteTeam(@PathVariable UUID teamId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }
            userService.deleteTeam(teamId, user.getId());
            return ResponseEntity.ok(Map.of("message", "Team deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/teams/joined")
    public ResponseEntity<?> viewJoinedTeams(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }
            return ResponseEntity.ok(userService.viewJoinedTeams(user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/teams/count")
    public ResponseEntity<?> getJoinedTeamsCount(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }
            return ResponseEntity.ok(Map.of("count", userService.numberOfTeamsJoined(user.getId())));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/teams/{teamId}/members/{memberId}/role")
    public ResponseEntity<?> updateMemberRole(@PathVariable UUID teamId, @PathVariable UUID memberId, @RequestParam TeamStatus role, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }
            return ResponseEntity.ok(userService.updateMemberRole(teamId, memberId, role, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
