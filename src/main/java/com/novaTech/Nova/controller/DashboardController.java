package com.novaTech.Nova.controller;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.TeamStatus;
import com.novaTech.Nova.Security.UserPrincipal;
import com.novaTech.Nova.Services.ProjectService;
import com.novaTech.Nova.Services.TaskService;
import com.novaTech.Nova.Services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard") // FIX: consistent versioning with /api/v1/
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final UserService userService;
    // FIX: removed UserRegistrationService - no longer needed in controller

    // ==================== UTILITY METHOD ====================

    // FIX: shared helper - same pattern as TeamBasedController
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

    // ==================== DASHBOARD STATS ====================

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats() {
        try {
            // FIX: get userId directly from principal - no DB lookup needed
            UUID userId = userPrincipal().getUserId();

            long totalProjects = projectService.getProjectCount(userId);
            long completedProjects = projectService.getCompletedProjectCount(userId);
            long inProgressProjects = projectService.getInProgressProjectCount(userId);

            long totalTasks = taskService.getTaskCount(userId);
            long completedTasks = taskService.getCompletedTaskCount(userId);
            long inProgressTasks = taskService.getInProgressTaskCount(userId);

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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/projects")
    public ResponseEntity<?> getProjectSummaries() {
        try {
            UUID userId = userPrincipal().getUserId();
            List<ProjectSummaryDTO> projects = projectService.viewAllProjectsSummary(userId);
            return ResponseEntity.ok(projects);
        } catch (RuntimeException e) {
            log.error("Error fetching project summaries: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/projects/overdue")
    public ResponseEntity<?> getOverdueProjects() {
        try {
            UUID userId = userPrincipal().getUserId();
            List<ProjectSummaryDTO> projects = projectService.viewOverdueProjects(userId);
            return ResponseEntity.ok(projects);
        } catch (RuntimeException e) {
            log.error("Error fetching overdue projects: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tasks")
    public ResponseEntity<?> getTaskSummaries() {
        try {
            UUID userId = userPrincipal().getUserId();
            List<TaskSummaryDTO> tasks = taskService.viewAllTasksSummary(userId);
            return ResponseEntity.ok(tasks);
        } catch (RuntimeException e) {
            log.error("Error fetching task summaries: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tasks/overdue")
    public ResponseEntity<?> getOverdueTasks() {
        try {
            UUID userId = userPrincipal().getUserId();
            List<TaskSummaryDTO> tasks = taskService.viewOverdueTasks(userId);
            return ResponseEntity.ok(tasks);
        } catch (RuntimeException e) {
            log.error("Error fetching overdue tasks: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================
    // USER & TEAM MANAGEMENT
    // ========================

    @PostMapping("/users/search")
    public ResponseEntity<?> searchUsers(@RequestBody SearchUserRequest request) {
        try {
            UUID userId = userPrincipal().getUserId();
            return ResponseEntity.ok(userService.searchUser(request, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/teams")
    public ResponseEntity<?> createTeam(@RequestBody CreateTeamRequest request) {
        try {
            UUID userId = userPrincipal().getUserId();
            return ResponseEntity.status(HttpStatus.CREATED).body(userService.createTeam(request, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/teams/{teamId}/members")
    public ResponseEntity<?> addTeamMember(@PathVariable UUID teamId, @RequestBody AddTeamRequest request) {
        try {
            UUID userId = userPrincipal().getUserId();
            return ResponseEntity.ok(userService.addMember(teamId, request, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/teams/{teamId}/members")
    public ResponseEntity<?> viewTeamMembers(@PathVariable UUID teamId) {
        try {
            UUID userId = userPrincipal().getUserId();
            return ResponseEntity.ok(userService.viewTeamMembers(teamId, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/teams/{teamId}/members/details")
    public ResponseEntity<?> viewTeamMembersWithRole(@PathVariable UUID teamId) {
        try {
            UUID userId = userPrincipal().getUserId();
            return ResponseEntity.ok(userService.viewTeamMembersWithRole(teamId, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/teams/{teamId}/members/{memberId}")
    public ResponseEntity<?> removeTeamMember(@PathVariable UUID teamId, @PathVariable UUID memberId) {
        try {
            UUID userId = userPrincipal().getUserId();
            return ResponseEntity.ok(userService.removeMember(teamId, memberId, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/teams/{teamId}")
    public ResponseEntity<?> deleteTeam(@PathVariable UUID teamId) {
        try {
            UUID userId = userPrincipal().getUserId();
            userService.deleteTeam(teamId, userId);
            return ResponseEntity.ok(Map.of("message", "Team deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/teams/joined")
    public ResponseEntity<?> viewJoinedTeams() {
        try {
            UUID userId = userPrincipal().getUserId();
            return ResponseEntity.ok(userService.viewJoinedTeams(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/teams/count")
    public ResponseEntity<?> getJoinedTeamsCount() {
        try {
            UUID userId = userPrincipal().getUserId();
            return ResponseEntity.ok(Map.of("count", userService.numberOfTeamsJoined(userId)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/teams/{teamId}/members/{memberId}/role")
    public ResponseEntity<?> updateMemberRole(
            @PathVariable UUID teamId,
            @PathVariable UUID memberId,
            @RequestParam TeamStatus role) {
        try {
            UUID userId = userPrincipal().getUserId();
            return ResponseEntity.ok(userService.updateMemberRole(teamId, memberId, role, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}