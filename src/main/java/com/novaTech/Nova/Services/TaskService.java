package com.novaTech.Nova.Services;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.TaskStatus;
import com.novaTech.Nova.Entities.Project;
import com.novaTech.Nova.Entities.Task;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.ProjectRepo;
import com.novaTech.Nova.Entities.repo.TaskRepo;
import com.novaTech.Nova.Entities.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
@CacheConfig(cacheNames = "userTasks")
public class TaskService {

    private final ProjectRepo projectRepo;
    private final UserRepo userRepo;
    private final TaskRepo taskRepo;
    private final EmailService emailService;

    // to create a task for a user
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(key = "'user:' + #userId + '_tasks'"),
                    @CacheEvict(key = "'user:' + #userId + '_taskCount'"),
                    @CacheEvict(key = "'user:' + #userId + '_completedTaskCount'")
            }
    )
    public TaskResponseDTO createUserTask(UUID userId, TaskCreationDTO dto){
        log.info("Creating task for user with ID: {}", userId);
        // to check for the user
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new RuntimeException("User not found");
                });

        log.debug("User found: {}", user.getEmail());

        // to collect task info
        Task task = Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .priority(dto.getPriority())
                .dueDate(dto.getDueDate())
                .user(user)
                .status(TaskStatus.TO_DO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        taskRepo.save(task);
        log.info("Task created successfully with ID: {}", task.getId());

        // Send task creation email
            log.info("Sending task creation email to: {}", user.getEmail());
            emailService.taskUploadMail(
                    user.getEmail(),
                    task.getTitle(),
                    task.getPriority(),
                    task.getStatus(),
                    task.getDueDate()
            );


        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .userId(task.getUser().getId())
                .userName(task.getUser().getFirstName() + " " + task.getUser().getLastName())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }


    //now to create task for project
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(key = "'project:' + #projectId + '_tasks'"),
                    @CacheEvict(key = "'project:' + #projectId + '_taskCount'"),
                    @CacheEvict(key = "'project:' + #projectId + '_completedTaskCount'"),
                    @CacheEvict(key = "'user:' + #userId + '_userProjectTasks'")
            }
    )
    public TaskResponseForProjectDto createProjectTask(UUID projectId, UUID userId, TaskCreationDTO dto){
        log.info("Creating task for project with ID: {} by user with ID: {}", projectId, userId);
        // to check for user
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new RuntimeException("User not found");
                });

        // now for project
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> {
                    log.error("Project not found with ID: {}", projectId);
                    return new RuntimeException("Project not found");
                });

        // to see if the user and project id match
        if (!project.getUser().getId().equals(user.getId())){
            log.warn("User with ID: {} is not authorized to create task for project with ID: {}", userId, projectId);
            throw new RuntimeException("You are not authorized to create a task for this project");
        }

        // to collect task info
        Task task = Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .priority(dto.getPriority())
                .dueDate(dto.getDueDate())
                .project(project)
                .status(TaskStatus.TO_DO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        taskRepo.save(task);
        log.info("Project task created successfully with ID: {}", task.getId());

        // Send task creation email
            log.info("Sending project task creation email to: {}", user.getEmail());
            emailService.taskUploadMail(
                    user.getEmail(),
                    task.getTitle(),
                    task.getPriority(),
                    task.getStatus(),
                    task.getDueDate()
            );
            log.info("Project task creation email sent successfully to: {}", user.getEmail());
          // Task is still created successfully even if email fails to send

        return TaskResponseForProjectDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .projectId(task.getProject().getId())
                .projectTitle(task.getProject().getTitle())
                .build();
    }

    //to view all tasks for user
    @Cacheable(key = "'user:' + #userId + '_tasks'")
    public List<TaskResponseDTO> viewAllUserTasks(UUID userId){
        log.info("Fetching all tasks for user with ID: {}", userId);
        List<Task> tasks = taskRepo.findByUserId(userId);
        if (tasks == null || tasks.isEmpty()){
            log.warn("No tasks found for user with ID: {}", userId);
            return List.of();
        }

        // 2. Convert (map) the list of entities to a list of DTOs
        List<TaskResponseDTO> taskResponses = tasks.stream()
                .map(task -> TaskResponseDTO.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .description(task.getDescription())
                        .status(task.getStatus())
                        .priority(task.getPriority())
                        .dueDate(task.getDueDate())
                        .userId(task.getUser().getId())
                        .userName(task.getUser().getFirstName() + " " + task.getUser().getLastName())
                        .createdAt(task.getCreatedAt())
                        .updatedAt(task.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        log.info("Successfully fetched {} tasks for user with ID: {}", taskResponses.size(), userId);
        return taskResponses;
    }

    // to get task by id for user
    @Cacheable(key = "'user:' + #userId + '_task:' + #taskId")
    public TaskResponseDTO viewTaskByIdForUser(UUID taskId, UUID userId){
        log.info("Fetching task with ID: {} for user with ID: {}", taskId, userId);
        Task task = taskRepo.findByIdAndUserId(taskId, userId);
        if (task == null || !task.getUser().getId().equals(userId)){
            log.warn("Task not found with ID: {} for user with ID: {}", taskId, userId);
            return null;
        }

        log.info("Successfully fetched task with ID: {}", taskId);
        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .userId(task.getUser().getId())
                .userName(task.getUser().getUsername())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    // to view task by status
    @Cacheable(key = "'user:' + #userId + '_tasksByStatus:' + #status")
    public List<TaskResponseDTO> viewTasksByStatus(UUID userId, TaskStatus status){
        log.info("Fetching tasks for user with ID: {} with status: {}", userId, status);
        List<Task> tasks = taskRepo.findByUser_IdAndStatus(userId, status);
        if (tasks == null || tasks.isEmpty()){
            log.warn("No tasks found for user with ID: {} with status: {}", userId, status);
            return List.of();
        }

        List<TaskResponseDTO> taskResponses = tasks.stream()
                .map(task -> TaskResponseDTO.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .description(task.getDescription())
                        .status(task.getStatus())
                        .priority(task.getPriority())
                        .dueDate(task.getDueDate())
                        .userId(task.getUser().getId())
                        .userName(task.getUser().getFirstName() + " " + task.getUser().getLastName())
                        .createdAt(task.getCreatedAt())
                        .updatedAt(task.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
        log.info("Successfully fetched {} tasks for user with ID: {} with status: {}", taskResponses.size(), userId, status);
        return taskResponses;
    }


    // now for projects
    @Cacheable(key = "'project:' + #projectId + '_tasks'")
    public List<TaskResponseForProjectDto> viewAllProjectTasks(UUID projectId, UUID useId){
        log.info("Fetching all tasks for project with ID: {} by user with ID: {}", projectId, useId);
        // to see if user exists
        User user = userRepo.findById(useId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", useId);
                    return new RuntimeException("User does not exit");
                });

        // to see if project is linked to the user
        Project project = projectRepo.findByIdAndUser(projectId, user)
                .orElseThrow(() -> {
                    log.error("Project not found with ID: {} for user: {}", projectId, user.getId());
                    return new RuntimeException("Project not found or you do not have access to it");
                });

        List<Task> task = taskRepo.findByProjectId(project.getId());
        if (task == null || task.isEmpty()){
            log.warn("No tasks found for project with ID: {}", projectId);
            return List.of();
        }

        List<TaskResponseForProjectDto> taskResponses = task.stream()
                .map(task1 -> TaskResponseForProjectDto.builder()
                        .id(task1.getId())
                        .title(task1.getTitle())
                        .description(task1.getDescription())
                        .status(task1.getStatus())
                        .priority(task1.getPriority())
                        .dueDate(task1.getDueDate())
                        .createdAt(task1.getCreatedAt())
                        .updatedAt(task1.getUpdatedAt())
                        .projectId(task1.getProject().getId())
                        .projectTitle(task1.getProject().getTitle())
                        .build())
                .collect(Collectors.toList());

        log.info("Successfully fetched {} tasks for project with ID: {}", taskResponses.size(), projectId);
        return taskResponses;
    }


    // to get task by id for project
    @Cacheable(key = "'project:' + #projectId + '_task:' + #taskId")
    public TaskResponseForProjectDto viewTaskByIdForProject(UUID taskId, UUID projectId, UUID useId){
        log.info("Fetching task with ID: {} for project with ID: {} by user with ID: {}", taskId, projectId, useId);

        // to see if user exists
        User user = userRepo.findById(useId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", useId);
                    return new RuntimeException("User does not exit");
                });

        // to see if project is linked to the user
        Project project = projectRepo.findByIdAndUser(projectId, user)
                .orElseThrow(() -> {
                    log.error("Project not found with ID: {} for user: {}", projectId, user.getId());
                    return new RuntimeException("Project not found or you do not have access to it");
                });


        Task task = taskRepo.findByIdAndProjectId(taskId, project.getId());
        if (task == null){
            log.warn("No task found with ID: {} for project with ID: {}", taskId, projectId);
            return null;
        }

        log.info("Successfully fetched task with ID: {} for project with ID: {}", taskId, projectId);
        return TaskResponseForProjectDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .projectId(task.getProject().getId())
                .projectTitle(task.getProject().getTitle())
                .build();
    }

    //now by status
    @Cacheable(key = "'project:' + #projectId + '_tasksByStatus:' + #status")
    public List<TaskResponseForProjectDto> viewTasksByStatusForProject(UUID projectId, UUID useId, TaskStatus status){
        log.info("Fetching tasks for project with ID: {} with status: {} by user with ID: {}", projectId, status, useId);
        // to see if user exists
        User user = userRepo.findById(useId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", useId);
                    return new RuntimeException("User does not exit");
                });

        // to see if project is linked to the user
        Project project = projectRepo.findByIdAndUser(projectId, user)
                .orElseThrow(() -> {
                    log.error("Project not found with ID: {} for user: {}", projectId, user.getId());
                    return new RuntimeException("Project not found or you do not have access to it");
                });

        List<Task> tasks = taskRepo.findByProject_IdAndStatus(project.getId(), status);
        if (tasks == null || tasks.isEmpty()){
            log.warn("No tasks found for project with ID: {} with status: {}", projectId, status);
            return List.of();
        }

        List<TaskResponseForProjectDto> taskResponses = tasks.stream()
                .map(task -> TaskResponseForProjectDto.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .description(task.getDescription())
                        .status(task.getStatus())
                        .priority(task.getPriority())
                        .dueDate(task.getDueDate())
                        .createdAt(task.getCreatedAt())
                        .updatedAt(task.getUpdatedAt())
                        .projectId(task.getProject().getId())
                        .projectTitle(task.getProject().getTitle())
                        .build())
                .collect(Collectors.toList());

        log.info("Successfully fetched {} tasks for project with ID: {} with status: {}", taskResponses.size(), projectId, status);
        return taskResponses;
    }


    // to search for task for user
    @Cacheable(key = "'user:' + #userId + '_tasksByKeyword:' + #keyword")
    public List<TaskResponseDTO> searchUserTasks(UUID userId, String keyword){
        log.info("Searching tasks for user with ID: {} with keyword: {}", userId, keyword);
        List<Task> tasks = taskRepo.searchByUser(userId, keyword);
        if (tasks == null || tasks.isEmpty()){
            log.warn("No tasks found for user with ID: {} with keyword: {}", userId, keyword);
            return List.of();
        }

        List<TaskResponseDTO> taskResponses = tasks.stream()
                .map(task -> TaskResponseDTO.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .description(task.getDescription())
                        .status(task.getStatus())
                        .priority(task.getPriority())
                        .dueDate(task.getDueDate())
                        .userId(task.getUser().getId())
                        .userName(task.getUser().getFirstName() + " " + task.getUser().getLastName())
                        .createdAt(task.getCreatedAt())
                        .updatedAt(task.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        log.info("Successfully fetched {} tasks for user with ID: {} with keyword: {}", taskResponses.size(), userId, keyword);
        return taskResponses;
    }

    // now for project task general task
    @Cacheable(key = "'project:' + #projectId + '_tasksByKeyword:' + #keyword")
    public List<TaskResponseForProjectDto> searchProjectTasks(UUID projectId, String keyword, UUID userId){
        log.info("Searching tasks for project with ID: {} with keyword: {} by user with ID: {}", projectId, keyword, userId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new RuntimeException("User does not exit");
                });

        Project project = projectRepo.findByIdAndUser(projectId, user)
                .orElseThrow(() -> {
                    log.error("Project not found with ID: {} for user: {}", projectId, user.getId());
                    return new RuntimeException("Project not found or you do not have access to it");
                });

        List<Task> tasks = taskRepo.searchByProject(project.getId(), keyword);
        if (tasks == null || tasks.isEmpty()){
            log.warn("No tasks found for project with ID: {} with keyword: {}", projectId, keyword);
            return List.of();
        }

        List<TaskResponseForProjectDto> taskResponses = tasks.stream()
                .map(task -> TaskResponseForProjectDto.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .description(task.getDescription())
                        .status(task.getStatus())
                        .priority(task.getPriority())
                        .dueDate(task.getDueDate())
                        .createdAt(task.getCreatedAt())
                        .updatedAt(task.getUpdatedAt())
                        .projectId(task.getProject().getId())
                        .projectTitle(task.getProject().getTitle())
                        .build())
                .collect(Collectors.toList());

        log.info("Successfully fetched {} tasks for project with ID: {} with keyword: {}", taskResponses.size(), projectId, keyword);
        return taskResponses;
    }

    @Caching(evict = {
            @CacheEvict(key = "'user:' + #userId + '_tasks'"),
            @CacheEvict(key = "'user:' + #userId + '_taskCount'"),
            @CacheEvict(key = "'user:' + #userId + '_completedTaskCount'")
    })
    public TaskResponseDTO updateUserTask(UUID taskId, UUID userId, TaskUpdateDTO dto) {
        log.info("Updating task with ID: {} for user with ID: {}", taskId, userId);
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to update this task");
        }

        if (dto.getTitle() != null) {
            task.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
        }
        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
        }
        if (dto.getDueDate() != null) {
            task.setDueDate(dto.getDueDate());
        }
        task.setUpdatedAt(LocalDateTime.now());

        taskRepo.save(task);
        log.info("Task updated successfully with ID: {}", task.getId());

        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .userId(task.getUser().getId())
                .userName(task.getUser().getFirstName() + " " + task.getUser().getLastName())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    @Caching(evict = {
            @CacheEvict(key = "'user:' + #userId + '_tasks'"),
            @CacheEvict(key = "'user:' + #userId + '_taskCount'"),
            @CacheEvict(key = "'user:' + #userId + '_completedTaskCount'")
    })
    public void deleteUserTask(UUID taskId, UUID userId) {
        log.info("Deleting task with ID: {} for user with ID: {}", taskId, userId);
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this task");
        }

        taskRepo.delete(task);
        log.info("Task deleted successfully with ID: {}", taskId);
    }

    public TaskResponseForProjectDto updateProjectTask(UUID taskId, UUID projectId, UUID userId, TaskUpdateDTO dto) {
        log.info("Updating task with ID: {} for project with ID: {} by user with ID: {}", taskId, projectId, userId);
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Task does not belong to this project");
        }

        if (!task.getProject().getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to update this task");
        }

        if (dto.getTitle() != null) {
            task.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
        }
        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
        }
        if (dto.getDueDate() != null) {
            task.setDueDate(dto.getDueDate());
        }
        task.setUpdatedAt(LocalDateTime.now());

        taskRepo.save(task);
        log.info("Task updated successfully with ID: {}", task.getId());

        return TaskResponseForProjectDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .projectId(task.getProject().getId())
                .projectTitle(task.getProject().getTitle())
                .build();
    }

    @Caching(evict = {
            @CacheEvict(key = "'project:' + #projectId + '_tasks'"),
            @CacheEvict(key = "'project:' + #projectId + '_taskCount'"),
            @CacheEvict(key = "'project:' + #projectId + '_completedTaskCount'"),
            @CacheEvict(key = "'user:' + #userId + '_userProjectTasks'")
    })
    public void deleteProjectTask(UUID taskId, UUID projectId, UUID userId) {
        log.info("Deleting task with ID: {} for project with ID: {} by user with ID: {}", taskId, projectId, userId);
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Task does not belong to this project");
        }

        if (!task.getProject().getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this task");
        }

        taskRepo.delete(task);
        log.info("Task deleted successfully with ID: {}", taskId);
    }

    // ========================
    // NEW METHODS FOR SUMMARY AND STATS
    // ========================

    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_taskSummary'")
    public List<TaskSummaryDTO> viewAllTasksSummary(UUID userId) {
        log.info("Fetching task summaries for user ID: {}", userId);
        List<Task> tasks = taskRepo.findByUserId(userId);

        return tasks.stream()
                .map(task -> {
                    long daysLeft = 0;
                    if (task.getDueDate() != null) {
                        daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), task.getDueDate());
                    }
                    return TaskSummaryDTO.builder()
                            .id(task.getId())
                            .title(task.getTitle())
                            .description(task.getDescription())
                            .dueDate(task.getDueDate())
                            .daysLeft(daysLeft)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_taskCount'")
    public long getTaskCount(UUID userId) {
        return taskRepo.countByUserId(userId);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_completedTaskCount'")
    public long getCompletedTaskCount(UUID userId) {
        return taskRepo.countByUserIdAndStatus(userId, TaskStatus.DONE);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_inProgressTaskCount'")
    public long getInProgressTaskCount(UUID userId) {
        return taskRepo.countByUserIdAndStatus(userId, TaskStatus.IN_PROGRESS);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_overdueTaskCount'")
    public List<TaskSummaryDTO> viewOverdueTasks(UUID userId) {
        log.info("Fetching overdue tasks for user ID: {}", userId);
        List<Task> tasks = taskRepo.findByUserId(userId);

        return tasks.stream()
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now()))
                .map(task -> {
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), task.getDueDate());
                    return TaskSummaryDTO.builder()
                            .id(task.getId())
                            .title(task.getTitle())
                            .description(task.getDescription())
                            .dueDate(task.getDueDate())
                            .daysLeft(daysLeft)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
