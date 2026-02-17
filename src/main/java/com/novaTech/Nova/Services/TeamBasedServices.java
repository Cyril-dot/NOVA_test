package com.novaTech.Nova.Services;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.*;
import com.novaTech.Nova.Entities.Enums.DocumentType;
import com.novaTech.Nova.Entities.Enums.ProjectStatus;
import com.novaTech.Nova.Entities.Enums.TaskStatus;
import com.novaTech.Nova.Entities.Enums.TaskPriority;
import com.novaTech.Nova.Entities.Enums.TeamStatus;
import com.novaTech.Nova.Entities.repo.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "teamProjects")
public class TeamBasedServices {

    private final UserRepo userRepo;
    private final TeamRepository teamRepository;
    private final TeamProjectRepo teamProjectRepo;
    private final TeamProjectDocumentRepo teamprojectDocumentRepo;
    private final TeamTaskRepo teamTaskRepo;
    private final TeamReminderRepo teamReminderRepo;
    private final EmailService emailService;

    @PersistenceContext
    private EntityManager entityManager;

    private static final Map<String, DocumentType> MIME_TYPE_MAP = Map.of(
            "application/pdf", DocumentType.PDF,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", DocumentType.WORD,
            "application/msword", DocumentType.WORD,
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", DocumentType.POWERPOINT,
            "application/vnd.ms-powerpoint", DocumentType.POWERPOINT,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", DocumentType.EXCEL,
            "application/vnd.ms-excel", DocumentType.EXCEL
    );

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    // ==================== TEAM PROJECT METHODS ====================

    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'team:' + #teamId + '_projects'"),
            @CacheEvict(key = "'user:' + #adminId + '_projects'"),
            @CacheEvict(key = "'team:' + #teamId + '_projectCount'")
    })
    public TeamProjectResponse createTeamProject(TeamProjectCreateDTO dto, UUID adminId, UUID teamId) throws IOException {
        log.info("Creating team project for admin ID: {}", adminId);

        User user = userRepo.findById(adminId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        if (!isTeamAdminOrOwner(user.getId(), team)) {
            log.warn("User {} is not authorized to create projects in team {}", adminId, teamId);
            throw new RuntimeException("Only team admins can create projects");
        }

        if (dto.startDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Start date cannot be in the past");
        }

        TeamProject project = TeamProject.builder()
                .title(dto.title())
                .description(dto.description())
                .status(dto.status() != null ? dto.status() : ProjectStatus.ACTIVE)
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .team(team)
                .build();

        TeamProject savedProject = teamProjectRepo.save(project);
        entityManager.flush();

        log.info("Team project created successfully - ID: {}, Name: {}", savedProject.getId(), savedProject.getTitle());

        if (dto.documents() != null && !dto.documents().isEmpty()) {
            log.info("Uploading {} documents for project {}", dto.documents().size(), savedProject.getId());
            uploadDocumentsForProject(savedProject, user, dto.documents(), dto.documentDescription());
        }

        return buildProjectResponse(savedProject, true, adminId);
    }

    private List<TeamProjectDocumentResponseDTO> uploadDocumentsForProject(
            TeamProject project, User user, List<MultipartFile> files, String description) throws IOException {

        List<TeamProjectDocumentResponseDTO> uploadedDocuments = new ArrayList<>();
        log.info("Starting upload of {} files for project {}", files.size(), project.getId());

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                log.warn("Skipping empty file");
                continue;
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new RuntimeException("File " + file.getOriginalFilename() + " exceeds maximum size of 50MB");
            }

            String mimeType = file.getContentType();
            if (mimeType == null || !MIME_TYPE_MAP.containsKey(mimeType)) {
                throw new RuntimeException("Unsupported file type: " + mimeType + ". Supported types: PDF, Word, PowerPoint, Excel");
            }

            try {
                byte[] fileBytes = file.getBytes();
                String base64Content = Base64.getEncoder().encodeToString(fileBytes);
                DocumentType docType = MIME_TYPE_MAP.get(mimeType);
                String fileName = UUID.randomUUID() + getFileExtension(file.getOriginalFilename());

                TeamProjectDocument document = TeamProjectDocument.builder()
                        .fileName(fileName)
                        .originalFileName(file.getOriginalFilename())
                        .fileContent(base64Content)
                        .documentType(docType)
                        .fileSize(file.getSize())
                        .mimeType(mimeType)
                        .description(description)
                        .teamProject(project)
                        .uploadedBy(user)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                TeamProjectDocument savedDocument = teamprojectDocumentRepo.save(document);
                log.info("Document uploaded - ID: {}, Name: {}", savedDocument.getId(), savedDocument.getOriginalFileName());
                uploadedDocuments.add(buildDocumentResponse(savedDocument));

            } catch (IOException e) {
                log.error("Failed to process file: {}", file.getOriginalFilename(), e);
                throw new RuntimeException("Failed to process file: " + file.getOriginalFilename());
            }
        }

        log.info("Upload complete. Total documents uploaded: {}", uploadedDocuments.size());
        return uploadedDocuments;
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }

    private TeamProjectDocumentResponseDTO buildDocumentResponse(TeamProjectDocument document) {
        return TeamProjectDocumentResponseDTO.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .originalFileName(document.getOriginalFileName())
                .documentType(document.getDocumentType())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .description(document.getDescription())
                .projectId(document.getTeamProject().getId())
                .projectName(document.getTeamProject().getTitle())
                .uploadedById(document.getUploadedBy().getId())
                .uploadedByEmail(document.getUploadedBy().getEmail())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private TeamProjectResponse buildProjectResponse(TeamProject project, boolean includeDocuments, UUID currentUserId) {
        List<TeamProjectDocument> documents = new ArrayList<>();
        long documentCount;

        if (includeDocuments) {
            documents = teamprojectDocumentRepo.findByProjectAndIsDeletedFalse(project);
            documentCount = documents.size();
        } else {
            documentCount = teamprojectDocumentRepo.countByProjectAndIsDeletedFalse(project);
        }

        // FIX: replaced instanceof cast with pattern variable
        TeamStatus memberRole = project.getTeam().getMembers().stream()
                .filter(m -> m.getUser().getId().equals(currentUserId))
                .map(TeamMember::getRole)
                .findFirst()
                .orElse(TeamStatus.MEMBER);

        TeamProjectResponse.TeamProjectResponseBuilder builder = TeamProjectResponse.builder()
                .id(project.getId())
                .name(project.getTitle())
                .description(project.getDescription())
                .ownerName(project.getTeam().getUser().getFirstName() + " " + project.getTeam().getUser().getLastName())
                .teamName(project.getTeam().getName())
                .role(memberRole)
                .teamId(project.getTeam().getId())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .documentCount(documentCount);

        if (includeDocuments && !documents.isEmpty()) {
            builder.documents(documents.stream()
                    .map(this::buildDocumentResponse)
                    .collect(Collectors.toList()));
        } else {
            builder.documents(new ArrayList<>());
        }

        return builder.build();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'project:' + #teamProjectId"),
            @CacheEvict(key = "'project:' + #teamProjectId + '_documents'"),
            @CacheEvict(key = "'team:' + #teamId + '_projects'"),
            @CacheEvict(key = "'user:' + #adminId + '_projects'")
    })
    public TeamProjectResponse updateTeamProject(UUID teamProjectId, UUID adminId, TeamProjectUpdateDTO dto, UUID teamId) throws IOException {
        User user = userRepo.findById(adminId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        if (!isTeamMember(user.getId(), team)) {
            throw new RuntimeException("User is not a member of this team");
        }

        if (!isTeamAdminOrOwner(user.getId(), team)) {
            throw new RuntimeException("Only team admins can update projects");
        }

        TeamProject project = teamProjectRepo.findById(teamProjectId)
                .orElseThrow(() -> new RuntimeException("Team project not found"));

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) project.setTitle(dto.getTitle());
        if (dto.getDescription() != null) project.setDescription(dto.getDescription());
        if (dto.getStatus() != null) project.setStatus(dto.getStatus());
        if (dto.getStartDate() != null) project.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) project.setEndDate(dto.getEndDate());

        TeamProject updatedProject = teamProjectRepo.save(project);
        entityManager.flush();

        if (dto.getDocuments() != null && !dto.getDocuments().isEmpty()) {
            uploadDocumentsForProject(updatedProject, user, dto.getDocuments(), dto.getDocumentDescription());
            entityManager.flush();
            entityManager.refresh(updatedProject);
        }

        return buildProjectResponse(updatedProject, true, adminId);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'project:' + #teamProjectId")
    public TeamProjectResponse getProjectById(UUID teamProjectId, UUID userId, UUID teamId) {
        TeamProject project = teamProjectRepo.findByIdAndUserIsTeamMember(teamProjectId, userId)
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));

        if (!project.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Project does not belong to the specified team");
        }

        return buildProjectResponse(project, true, userId);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_projects'")
    public List<TeamProjectResponse> getAllProjects(UUID userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TeamProject> projects = teamProjectRepo.findAllByUserIsTeamMember(userId);

        if (projects == null) return Collections.emptyList();

        return projects.stream()
                .map(project -> buildProjectResponse(project, true, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "projectDocuments", key = "'user:' + #userId + '_allDocuments'")
    public List<TeamProjectDocumentResponseDTO> getAllDocumentsForUser(UUID userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TeamProjectDocument> documents = teamprojectDocumentRepo.findAllByUserIsTeamMember(userId);

        if (documents == null) return Collections.emptyList();

        return documents.stream()
                .map(this::buildDocumentResponse)
                .collect(Collectors.toList());
    }

    public List<TeamProjectDocumentResponseDTO> getProjectDocuments(UUID projectId, UUID userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<TeamProjectDocument> documents = teamprojectDocumentRepo
                .findAllByProjectAndUserIsTeamMember(project.getId(), userId);

        if (documents == null) return Collections.emptyList();

        return documents.stream()
                .map(this::buildDocumentResponse)
                .collect(Collectors.toList());
    }

    // FIX: added teamId parameter for ownership validation (was ignored before)
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'project:' + #projectId"),
            @CacheEvict(key = "'project:' + #projectId + '_documents'", cacheNames = "projectDocuments"),
            @CacheEvict(key = "'project:' + #projectId + '_tasks'", cacheNames = "teamTasks"),
            @CacheEvict(allEntries = true, cacheNames = "teamProjects")
    })
    public void deleteProject(UUID projectId, UUID userId, UUID teamId) {
        log.info("Deleting project ID: {} for user ID: {} in team ID: {}", projectId, userId, teamId);

        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // FIX: validate project actually belongs to the requested team
        if (!project.getTeam().getId().equals(teamId)) {
            log.warn("Project {} does not belong to team {}", projectId, teamId);
            throw new RuntimeException("Project does not belong to the specified team");
        }

        // FIX: validate user has permission to delete
        if (!isTeamAdminOrOwner(userId, team)) {
            log.warn("User {} is not authorized to delete project {} in team {}", userId, projectId, teamId);
            throw new RuntimeException("Only team admins can delete projects");
        }

        teamProjectRepo.delete(project);
        log.info("Project deleted successfully - ID: {}", projectId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "projectDocuments", key = "'project:' + #projectId + '_documents'"),
            @CacheEvict(cacheNames = "projectDocuments", key = "'user:' + #userId + '_allDocuments'"),
            @CacheEvict(key = "'project:' + #projectId")
    })
    public List<TeamProjectDocumentResponseDTO> uploadDocument(
            UUID projectId, UUID userId, List<MultipartFile> files, String description, UUID teamId) throws IOException {

        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // FIX: fetch user entity for uploadDocumentsForProject
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        if (!project.getTeam().getId().equals(team.getId())) {
            throw new RuntimeException("Unauthorized access: project does not belong to team");
        }

        if (!isTeamAdminOrOwner(user.getId(), team)) {
            throw new RuntimeException("Only team admins can upload documents");
        }

        List<TeamProjectDocumentResponseDTO> uploadDocs = uploadDocumentsForProject(project, user, files, description);
        entityManager.flush();
        entityManager.refresh(project);

        return uploadDocs;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "projectDocuments", key = "'project:' + #projectId + '_documents'"),
            @CacheEvict(cacheNames = "projectDocuments", key = "'user:' + #userId + '_allDocuments'"),
            @CacheEvict(key = "'project:' + #projectId")
    })
    public String deleteDocument(UUID documentId, UUID userId, UUID teamId, UUID projectId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        if (!project.getTeam().getId().equals(team.getId())) {
            throw new RuntimeException("Unauthorized access: project does not belong to team");
        }

        if (!isTeamAdminOrOwner(userId, team)) {
            throw new RuntimeException("Only team admins can delete documents");
        }

        TeamProjectDocument document = teamprojectDocumentRepo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        document.markAsDeleted();
        teamprojectDocumentRepo.delete(document);

        log.info("Document deleted successfully - ID: {}", documentId);
        return "Document deleted successfully";
    }

    @Cacheable(key = "'team:' + #teamId + '_user:' + #userId + '_summary'")
    public List<TeamProjectSummaryDto> viewAllProjectSummary(UUID userId, UUID teamId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        return teamProjectRepo.findAllByTeamIdAndUserIsTeamMember(team.getId(), userId).stream()
                .map(project -> {
                    long daysLeft = project.getEndDate() != null
                            ? ChronoUnit.DAYS.between(LocalDate.now(), project.getEndDate())
                            : 0;

                    return TeamProjectSummaryDto.builder()
                            .id(project.getId())
                            .name(project.getTitle())
                            .description(project.getDescription())
                            .dueDate(project.getEndDate())
                            .daysLeft(daysLeft)
                            .build();
                }).collect(Collectors.toList());
    }

    @Cacheable(key = "'team:' + #teamId + '_user:' + #userId + '_count'")
    public long getProjectCount(UUID userId, UUID teamId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        if (!team.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        return teamProjectRepo.countByTeamIdAndUserIsTeamMember(team.getId(), user.getId());
    }

    @Cacheable(key = "'team:' + #teamId + '_user:' + #userId + '_countBasedOnStatus'")
    public long countBasedOnStatusProjects(UUID userId, UUID teamId, ProjectStatus status) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        if (!team.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        return teamProjectRepo.countByTeamIdAndUserAndStatus(team.getId(), user.getId(), status);
    }

    @Cacheable(key = "'team:' + #teamId + '_user:' + #userId + '_overdue'")
    public List<TeamProjectSummaryDto> viewOverdueProjects(UUID userId, UUID teamId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        return teamProjectRepo.findOverdueProjects(team.getId(), userId).stream()
                .map(project -> TeamProjectSummaryDto.builder()
                        .id(project.getId())
                        .name(project.getTitle())
                        .description(project.getDescription())
                        .dueDate(project.getEndDate())
                        .daysLeft(ChronoUnit.DAYS.between(LocalDate.now(), project.getEndDate()))
                        .build())
                .toList();
    }

    // ==================== TEAM BASED TASKS ====================

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "teamTasks", key = "'project:' + #projectId + '_tasks'"),
            @CacheEvict(cacheNames = "teamTasks", key = "'user:' + #userId + '_team:' + #teamId + '_tasks'"),
            @CacheEvict(cacheNames = "teamTasks", allEntries = true, condition = "#dto.assignedToUserId != null")
    })
    public TeamTaskResponseDTO createTask(UUID projectId, UUID userId, UUID teamId, TeamTaskCreateDTO dto) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Project does not belong to the specified team");
        }

        if (!isTeamMember(userId, team)) {
            throw new RuntimeException("Only team members can create tasks");
        }

        User assignedUser = null;
        if (dto.getAssignedToUserId() != null) {
            assignedUser = userRepo.findById(dto.getAssignedToUserId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));

            if (!isTeamMember(assignedUser.getId(), team)) {
                throw new RuntimeException("Can only assign tasks to team members");
            }
        }

        TeamTask task = TeamTask.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus() != null ? dto.getStatus() : TaskStatus.TODO)
                .priority(dto.getPriority() != null ? dto.getPriority() : TaskPriority.MEDIUM)
                .dueDate(dto.getDueDate())
                .teamProject(project)
                .createdBy(user)
                .assignedTo(assignedUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TeamTask savedTask = teamTaskRepo.save(task);
        log.info("Task created - ID: {}, Title: {}", savedTask.getId(), savedTask.getTitle());

        if (assignedUser != null) {
            sendTaskAssignmentEmail(assignedUser, task, project);
        }

        return buildTaskResponse(savedTask);
    }

    // FIX: added teamId parameter for ownership validation
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "teamTasks", key = "'task:' + #taskId"),
            @CacheEvict(cacheNames = "teamTasks", allEntries = true)
    })
    public TeamTaskResponseDTO updateTask(UUID taskId, UUID userId, UUID teamId, TeamTaskUpdateDTO dto) {
        log.info("Updating task ID: {} by user ID: {} in team ID: {}", taskId, userId, teamId);

        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // FIX: validate team exists and user is a member of it
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        TeamTask task = teamTaskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // FIX: validate task belongs to this team
        if (!task.getTeamProject().getTeam().getId().equals(teamId)) {
            log.warn("Task {} does not belong to team {}", taskId, teamId);
            throw new RuntimeException("Task does not belong to the specified team");
        }

        if (!isTeamMember(userId, team)) {
            throw new RuntimeException("Only team members can update tasks");
        }

        boolean statusChanged = false;
        TaskStatus oldStatus = task.getStatus();

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) task.setTitle(dto.getTitle());
        if (dto.getDescription() != null) task.setDescription(dto.getDescription());
        if (dto.getStatus() != null && !dto.getStatus().equals(task.getStatus())) {
            task.setStatus(dto.getStatus());
            statusChanged = true;
        }
        if (dto.getPriority() != null) task.setPriority(dto.getPriority());
        if (dto.getDueDate() != null) task.setDueDate(dto.getDueDate());

        if (dto.getAssignedToUserId() != null) {
            User newAssignedUser = userRepo.findById(dto.getAssignedToUserId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));

            if (!isTeamMember(newAssignedUser.getId(), task.getTeamProject().getTeam())) {
                throw new RuntimeException("Can only assign tasks to team members");
            }

            User oldAssignedUser = task.getAssignedTo();
            task.setAssignedTo(newAssignedUser);

            if (oldAssignedUser == null || !oldAssignedUser.getId().equals(newAssignedUser.getId())) {
                sendTaskReassignmentEmail(newAssignedUser, task, task.getTeamProject());
            }
        }

        task.setUpdatedAt(LocalDateTime.now());
        TeamTask updatedTask = teamTaskRepo.save(task);

        if (statusChanged && task.getAssignedTo() != null) {
            sendTaskStatusChangeEmail(task.getAssignedTo(), task, oldStatus, task.getStatus());
        }

        log.info("Task updated successfully - ID: {}", updatedTask.getId());
        return buildTaskResponse(updatedTask);
    }

    // FIX: added teamId parameter for ownership validation
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "teamTasks", key = "'task:' + #taskId"),
            @CacheEvict(cacheNames = "teamTasks", allEntries = true)
    })
    public String deleteTask(UUID taskId, UUID userId, UUID teamId) {
        log.info("Deleting task ID: {} by user ID: {} in team ID: {}", taskId, userId, teamId);

        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // FIX: validate team exists
        teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        TeamTask task = teamTaskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // FIX: validate task belongs to this team
        if (!task.getTeamProject().getTeam().getId().equals(teamId)) {
            log.warn("Task {} does not belong to team {}", taskId, teamId);
            throw new RuntimeException("Task does not belong to the specified team");
        }

        boolean isCreator = task.getCreatedBy().getId().equals(userId);
        boolean isAdmin = isTeamAdminOrOwner(userId, task.getTeamProject().getTeam());

        if (!isCreator && !isAdmin) {
            throw new RuntimeException("Only task creator or team admin can delete tasks");
        }

        teamTaskRepo.delete(task);
        log.info("Task deleted successfully - ID: {}", taskId);
        return "Task deleted successfully";
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "teamTasks", key = "'project:' + #projectId + '_tasks'")
    public List<TeamTaskResponseDTO> getProjectTasks(UUID projectId, UUID userId) {
        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!isTeamMember(userId, project.getTeam())) {
            throw new RuntimeException("Only team members can view tasks");
        }

        return teamTaskRepo.findByTeamProjectId(projectId).stream()
                .map(this::buildTaskResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "teamTasks", key = "'user:' + #userId + '_team:' + #teamId + '_tasks'")
    @Transactional(readOnly = true)
    public List<TeamTaskResponseDTO> getUserTasks(UUID userId, UUID teamId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        return teamTaskRepo.findByAssignedToIdAndTeamId(userId, teamId).stream()
                .map(this::buildTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shortLivedCache", key = "'user:' + #userId + '_team:' + #teamId + '_overdueTasks'")
    public List<TeamTaskResponseDTO> getOverdueTasks(UUID userId, UUID teamId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        return teamTaskRepo.findOverdueTasks(teamId, userId).stream()
                .map(this::buildTaskResponse)
                .collect(Collectors.toList());
    }

    private TeamTaskResponseDTO buildTaskResponse(TeamTask task) {
        return TeamTaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .projectId(task.getTeamProject().getId())
                .projectName(task.getTeamProject().getTitle())
                .createdById(task.getCreatedBy().getId())
                .createdByName(task.getCreatedBy().getFirstName() + " " + task.getCreatedBy().getLastName())
                .assignedToId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .assignedToName(task.getAssignedTo() != null
                        ? task.getAssignedTo().getFirstName() + " " + task.getAssignedTo().getLastName()
                        : null)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    // ==================== TEAM BASED REMINDERS ====================

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "teamReminders", key = "'user:' + #userId + '_team:' + #teamId + '_reminders'"),
            @CacheEvict(cacheNames = "teamReminders", key = "'project:' + #dto.projectId + '_reminders'", condition = "#dto.projectId != null"),
            @CacheEvict(cacheNames = "teamReminders", key = "'task:' + #dto.taskId + '_reminders'", condition = "#dto.taskId != null")
    })
    public TeamReminderResponseDTO createReminder(UUID userId, UUID teamId, TeamReminderCreateDTO dto) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        if (!isTeamMember(userId, team)) {
            throw new RuntimeException("Only team members can create reminders");
        }

        TeamProject project = null;
        if (dto.getProjectId() != null) {
            project = teamProjectRepo.findById(dto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            if (!project.getTeam().getId().equals(teamId)) {
                throw new RuntimeException("Project does not belong to the team");
            }
        }

        TeamTask task = null;
        if (dto.getTaskId() != null) {
            task = teamTaskRepo.findById(dto.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            if (!task.getTeamProject().getTeam().getId().equals(teamId)) {
                throw new RuntimeException("Task does not belong to the team");
            }
        }

        TeamReminder reminder = TeamReminder.builder()
                .title(dto.getTitle())
                .message(dto.getMessage())
                .reminderDateTime(dto.getReminderDateTime())
                .isRecurring(dto.isRecurring())
                .recurringInterval(dto.getRecurringInterval())
                .teamProject(project)
                .teamTask(task)
                .user(user)
                .team(team)
                .isSent(false)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        TeamReminder savedReminder = teamReminderRepo.save(reminder);
        log.info("Reminder created - ID: {}", savedReminder.getId());
        return buildReminderResponse(savedReminder);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "teamReminders", allEntries = true)
    })
    public TeamReminderResponseDTO updateReminder(UUID reminderId, UUID userId, TeamReminderUpdateDTO dto) {
        TeamReminder reminder = teamReminderRepo.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));

        if (!reminder.getUser().getId().equals(userId)) {
            throw new RuntimeException("Only reminder creator can update it");
        }

        if (dto.getTitle() != null) reminder.setTitle(dto.getTitle());
        if (dto.getMessage() != null) reminder.setMessage(dto.getMessage());
        if (dto.getReminderDateTime() != null) reminder.setReminderDateTime(dto.getReminderDateTime());
        if (dto.getIsRecurring() != null) reminder.setRecurring(dto.getIsRecurring());
        if (dto.getRecurringInterval() != null) reminder.setRecurringInterval(dto.getRecurringInterval());
        if (dto.getIsActive() != null) reminder.setActive(dto.getIsActive());

        TeamReminder updated = teamReminderRepo.save(reminder);
        log.info("Reminder updated - ID: {}", updated.getId());
        return buildReminderResponse(updated);
    }

    @Transactional
    @CacheEvict(cacheNames = "teamReminders", allEntries = true)
    public String deleteReminder(UUID reminderId, UUID userId) {
        TeamReminder reminder = teamReminderRepo.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));

        if (!reminder.getUser().getId().equals(userId)) {
            throw new RuntimeException("Only reminder creator can delete it");
        }

        teamReminderRepo.delete(reminder);
        log.info("Reminder deleted - ID: {}", reminderId);
        return "Reminder deleted successfully";
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "teamReminders", key = "'user:' + #userId + '_team:' + #teamId + '_reminders'")
    public List<TeamReminderResponseDTO> getUserReminders(UUID userId, UUID teamId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        return teamReminderRepo.findByUserIdAndTeamIdAndIsActiveTrue(userId, teamId).stream()
                .map(this::buildReminderResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "teamReminders", key = "'project:' + #projectId + '_reminders'")
    @Transactional(readOnly = true)
    public List<TeamReminderResponseDTO> getProjectReminders(UUID projectId, UUID userId) {
        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!isTeamMember(userId, project.getTeam())) {
            throw new RuntimeException("Only team members can view reminders");
        }

        return teamReminderRepo.findByTeamProjectIdAndIsActiveTrue(projectId).stream()
                .map(this::buildReminderResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "teamReminders", key = "'task:' + #taskId + '_reminders'")
    @Transactional(readOnly = true)
    public List<TeamReminderResponseDTO> getTaskReminders(UUID taskId, UUID userId) {
        TeamTask task = teamTaskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!isTeamMember(userId, task.getTeamProject().getTeam())) {
            throw new RuntimeException("Only team members can view reminders");
        }

        return teamReminderRepo.findByTeamTaskIdAndIsActiveTrue(taskId).stream()
                .map(this::buildReminderResponse)
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendScheduledReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<TeamReminder> dueReminders = teamReminderRepo.findDueReminders(now, now.plusMinutes(1));

        log.info("Found {} reminders to send", dueReminders.size());

        for (TeamReminder reminder : dueReminders) {
            try {
                sendReminderEmail(reminder);
                reminder.setSent(true);

                if (reminder.isRecurring() && reminder.getRecurringInterval() != null) {
                    reminder.setReminderDateTime(
                            calculateNextReminderTime(reminder.getReminderDateTime(), reminder.getRecurringInterval()));
                    reminder.setSent(false);
                } else {
                    reminder.setActive(false);
                }

                teamReminderRepo.save(reminder);
            } catch (Exception e) {
                log.error("Failed to send reminder ID: {}", reminder.getId(), e);
            }
        }
    }

    private LocalDateTime calculateNextReminderTime(LocalDateTime currentTime, String interval) {
        return switch (interval.toUpperCase()) {
            case "DAILY" -> currentTime.plusDays(1);
            case "WEEKLY" -> currentTime.plusWeeks(1);
            case "MONTHLY" -> currentTime.plusMonths(1);
            default -> currentTime.plusDays(1);
        };
    }

    private TeamReminderResponseDTO buildReminderResponse(TeamReminder reminder) {
        return TeamReminderResponseDTO.builder()
                .id(reminder.getId())
                .title(reminder.getTitle())
                .message(reminder.getMessage())
                .reminderDateTime(reminder.getReminderDateTime())
                .isRecurring(reminder.isRecurring())
                .recurringInterval(reminder.getRecurringInterval())
                .projectId(reminder.getTeamProject() != null ? reminder.getTeamProject().getId() : null)
                .projectName(reminder.getTeamProject() != null ? reminder.getTeamProject().getTitle() : null)
                .taskId(reminder.getTeamTask() != null ? reminder.getTeamTask().getId() : null)
                .taskName(reminder.getTeamTask() != null ? reminder.getTeamTask().getTitle() : null)
                .userId(reminder.getUser().getId())
                .userName(reminder.getUser().getFirstName() + " " + reminder.getUser().getLastName())
                .teamId(reminder.getTeam().getId())
                .teamName(reminder.getTeam().getName())
                .isSent(reminder.isSent())
                .isActive(reminder.isActive())
                .createdAt(reminder.getCreatedAt())
                .build();
    }

    // ==================== TEAM MESSAGES ====================

    @Transactional
    public String sendTeamMessage(UUID userId, UUID teamId, TeamMessageDTO dto) {
        User sender = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        if (!isTeamMember(userId, team)) {
            throw new RuntimeException("Only team members can send team messages");
        }

        List<String> recipientEmails;

        if (dto.isSendToAll()) {
            recipientEmails = team.getMembers().stream()
                    .map(m -> m.getUser().getEmail())
                    .filter(email -> !email.equals(sender.getEmail()))
                    .collect(Collectors.toList());

            if (!team.getUser().getEmail().equals(sender.getEmail())) {
                recipientEmails.add(team.getUser().getEmail());
            }
        } else if (dto.getRecipientUserIds() != null && !dto.getRecipientUserIds().isEmpty()) {
            recipientEmails = new ArrayList<>();
            for (UUID recipientId : dto.getRecipientUserIds()) {
                User recipient = userRepo.findById(recipientId)
                        .orElseThrow(() -> new RuntimeException("Recipient user not found: " + recipientId));

                if (!isTeamMember(recipientId, team)) {
                    throw new RuntimeException("Recipient must be a team member: " + recipientId);
                }
                recipientEmails.add(recipient.getEmail());
            }
        } else {
            throw new RuntimeException("Must specify recipients or send to all");
        }

        if (recipientEmails.isEmpty()) {
            throw new RuntimeException("No recipients found");
        }

        String subject = String.format("[%s] %s", team.getName(), dto.getSubject());
        String body = String.format("From: %s %s (%s)\nTeam: %s\n\n%s",
                sender.getFirstName(), sender.getLastName(), sender.getEmail(),
                team.getName(), dto.getMessage());

        int successCount = 0;
        for (String email : recipientEmails) {
            try {
                emailService.sendEmail(email, subject, body);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send email to: {}", email, e);
            }
        }

        return String.format("Message sent successfully to %d/%d recipients", successCount, recipientEmails.size());
    }

    @Transactional
    public String sendProjectUpdate(UUID projectId, UUID userId, ProjectUpdateMessageDTO dto) {
        User sender = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Team team = project.getTeam();

        if (!isTeamMember(userId, team)) {
            throw new RuntimeException("Only team members can send project updates");
        }

        List<String> recipientEmails = team.getMembers().stream()
                .map(m -> m.getUser().getEmail())
                .filter(email -> !email.equals(sender.getEmail()))
                .collect(Collectors.toList());

        if (!team.getUser().getEmail().equals(sender.getEmail())) {
            recipientEmails.add(team.getUser().getEmail());
        }

        String subject = String.format("[%s] Project Update: %s", team.getName(), project.getTitle());
        String body = String.format("Project: %s\nFrom: %s %s\n\n%s\n\nProject Status: %s\nDue Date: %s",
                project.getTitle(), sender.getFirstName(), sender.getLastName(),
                dto.getMessage(), project.getStatus(), project.getEndDate());

        int successCount = 0;
        for (String email : recipientEmails) {
            try {
                emailService.sendEmail(email, subject, body);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send project update to: {}", email, e);
            }
        }

        return String.format("Project update sent to %d/%d team members", successCount, recipientEmails.size());
    }

    // ==================== EMAIL HELPERS ====================

    private void sendTaskAssignmentEmail(User assignedUser, TeamTask task, TeamProject project) {
        try {
            emailService.sendEmail(
                    assignedUser.getEmail(),
                    "New Task Assigned: " + task.getTitle(),
                    String.format("Hi %s,\n\nYou have been assigned a new task:\n\nTask: %s\nProject: %s\nDescription: %s\nDue Date: %s\nPriority: %s",
                            assignedUser.getFirstName(), task.getTitle(), project.getTitle(),
                            task.getDescription(), task.getDueDate(), task.getPriority()));
        } catch (Exception e) {
            log.error("Failed to send task assignment email", e);
        }
    }

    private void sendTaskReassignmentEmail(User newAssignedUser, TeamTask task, TeamProject project) {
        try {
            emailService.sendEmail(
                    newAssignedUser.getEmail(),
                    "Task Reassigned: " + task.getTitle(),
                    String.format("Hi %s,\n\nA task has been reassigned to you:\n\nTask: %s\nProject: %s\nDescription: %s\nDue Date: %s\nPriority: %s",
                            newAssignedUser.getFirstName(), task.getTitle(), project.getTitle(),
                            task.getDescription(), task.getDueDate(), task.getPriority()));
        } catch (Exception e) {
            log.error("Failed to send task reassignment email", e);
        }
    }

    private void sendTaskStatusChangeEmail(User assignedUser, TeamTask task, TaskStatus oldStatus, TaskStatus newStatus) {
        try {
            emailService.sendEmail(
                    assignedUser.getEmail(),
                    "Task Status Updated: " + task.getTitle(),
                    String.format("Hi %s,\n\nThe status of your task has been updated:\n\nTask: %s\nOld Status: %s\nNew Status: %s",
                            assignedUser.getFirstName(), task.getTitle(), oldStatus, newStatus));
        } catch (Exception e) {
            log.error("Failed to send task status change email", e);
        }
    }

    private void sendReminderEmail(TeamReminder reminder) {
        try {
            StringBuilder body = new StringBuilder(
                    String.format("Hi %s,\n\nThis is a reminder:\n\n%s\n\n",
                            reminder.getUser().getFirstName(), reminder.getMessage()));

            if (reminder.getTeamProject() != null) {
                body.append("Related Project: ").append(reminder.getTeamProject().getTitle()).append("\n");
            }
            if (reminder.getTeamTask() != null) {
                body.append("Related Task: ").append(reminder.getTeamTask().getTitle()).append("\n");
            }

            emailService.sendEmail(reminder.getUser().getEmail(), "Reminder: " + reminder.getTitle(), body.toString());
        } catch (Exception e) {
            log.error("Failed to send reminder email", e);
        }
    }

    // ==================== HELPER METHODS ====================

    private boolean isTeamMember(UUID userId, Team team) {
        return team.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userId))
                || team.getUser().getId().equals(userId);
    }

    private boolean isTeamAdminOrOwner(UUID userId, Team team) {
        boolean isAdmin = team.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userId) && m.getRole() == TeamStatus.ADMIN);
        return isAdmin || team.getUser().getId().equals(userId);
    }
}