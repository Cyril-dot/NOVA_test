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

    // Supported file types and their MIME types
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

    // to create a team based project
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'team:' + #teamId + '_projects'"),
            @CacheEvict(key = "'user:' + #adminId + '_projects'"),
            @CacheEvict(key = "'team:' + #teamId + '_projectCount'")
    })
    public TeamProjectResponse createTeamProject(TeamProjectCreateDTO dto, UUID adminId, UUID teamId) throws IOException {
        log.info("Creating team project for admin ID, {}", adminId);

        User user = userRepo.findById(adminId)
                .orElseThrow(() -> {
                    log.warn("User with id {} not found", adminId);
                    return new RuntimeException("User not found");
                });

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> {
                    log.warn("Team with id {} not found", teamId);
                    return new EntityNotFoundException("Team not found");
                });

        // check if user is an admin
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

        // upload documents if provided
        if (dto.documents() != null && !dto.documents().isEmpty()) {
            log.info("Uploading {} documents for project {}", dto.documents().size(), savedProject.getId());
            uploadDocumentsForProject(savedProject, user, dto.documents(), dto.documentDescription());
        }

        // Return the project response with documents included
        return buildProjectResponse(savedProject, true, adminId);
    }

    private List<TeamProjectDocumentResponseDTO> uploadDocumentsForProject(TeamProject project, User user, List<MultipartFile> files, String description) throws IOException {
        List<TeamProjectDocumentResponseDTO> uploadedDocuments = new ArrayList<>();
        log.info("📤 Starting upload of {} files for project {}", files.size(), project.getId());

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                log.warn("⚠️ Skipping empty file");
                continue;
            }

            log.info("📄 Processing file: {} (size: {} bytes, type: {})",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            // Validate file size
            if (file.getSize() > MAX_FILE_SIZE) {
                String errorMsg = "File " + file.getOriginalFilename() + " exceeds maximum size of 50MB";
                log.error("❌ {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }

            // Validate MIME type
            String mimeType = file.getContentType();
            if (mimeType == null || !MIME_TYPE_MAP.containsKey(mimeType)) {
                String errorMsg = "Unsupported file type: " + mimeType + ". Supported types: PDF, Word, PowerPoint, Excel";
                log.error("❌ {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }

            try {
                byte[] fileBytes = file.getBytes();
                String base64Content = Base64.getEncoder().encodeToString(fileBytes);
                DocumentType docType = MIME_TYPE_MAP.get(mimeType);
                String fileName = UUID.randomUUID().toString() + getFileExtension(file.getOriginalFilename());

                log.info("🔄 Creating document entity for: {}", file.getOriginalFilename());

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

                log.info("💾 Saving document to database...");
                TeamProjectDocument savedDocument = teamprojectDocumentRepo.save(document);

                log.info("✅ Document uploaded successfully - ID: {}, Name: {}, Size: {} bytes, Type: {}",
                        savedDocument.getId(), savedDocument.getOriginalFileName(),
                        savedDocument.getFileSize(), savedDocument.getDocumentType());

                uploadedDocuments.add(buildDocumentResponse(savedDocument));

            } catch (IOException e) {
                log.error("❌ Failed to process file: {}", file.getOriginalFilename(), e);
                throw new RuntimeException("Failed to process file: " + file.getOriginalFilename());
            }
        }

        log.info("🎉 Upload complete! Total documents uploaded: {}", uploadedDocuments.size());
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
        log.info("🔨 Building project response for project ID: {}, includeDocuments: {}", project.getId(), includeDocuments);

        // ✅ Query documents directly from repository instead of lazy-loaded collection
        List<TeamProjectDocument> documents = new ArrayList<>();
        long documentCount = 0;

        if (includeDocuments) {
            documents = teamprojectDocumentRepo.findByProjectAndIsDeletedFalse(project);
            documentCount = documents.size();
            log.info("📊 Found {} active documents for project {}", documentCount, project.getId());
        } else {
            documentCount = teamprojectDocumentRepo.countByProjectAndIsDeletedFalse(project);
            log.info("📊 Document count for project {}: {}", project.getId(), documentCount);
        }

        // ✅ Get the role of the current user in the team
        TeamStatus memberRole = project.getTeam().getMembers().stream()
                .filter(m -> m.getUser().getId().equals(currentUserId))
                .map(TeamMember::getRole)
                .findFirst()
                .orElse(TeamStatus.MEMBER); // Default if not found

        log.info("👤 Role of current user {} in project {}: {}", currentUserId, project.getId(), memberRole);

        // ✅ Build the response
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
            List<TeamProjectDocumentResponseDTO> documentDTOs = documents.stream()
                    .map(this::buildDocumentResponse)
                    .collect(Collectors.toList());
            builder.documents(documentDTOs);
            log.info("📎 Added {} documents to response", documentDTOs.size());
        } else {
            builder.documents(new ArrayList<>()); // Empty list instead of null
        }

        TeamProjectResponse response = builder.build();
        log.info("✅ Project response built successfully - Documents included: {}, Role: {}",
                response.getDocumentCount(), memberRole);

        return response;
    }

    // to update team projects
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'project:' + #teamProjectId"),
            @CacheEvict(key = "'project:' + #teamProjectId + '_documents'"),
            @CacheEvict(key = "'team:' + #teamId + '_projects'"),
            @CacheEvict(key = "'user:' + #adminId + '_projects'")
    })
    public TeamProjectResponse updateTeamProject(UUID teamProjectId, UUID adminId, TeamProjectUpdateDTO dto, UUID teamId) throws IOException {
        User user = userRepo.findById(adminId)
                .orElseThrow(() -> {
                    log.warn("User with id {} not found", adminId);
                    return new RuntimeException("User not found");
                });

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> {
                    log.warn("Team with id {} not found", teamId);
                    return new RuntimeException("Team not found");
                });

        // Verify user is team member
        if (!isTeamMember(user.getId(), team)) {
            log.warn("User is not a member of this team");
            throw new RuntimeException("User is not a member of this team");
        }

        // Check if requester is ADMIN of the team
        if (!isTeamAdminOrOwner(user.getId(), team)) {
            log.warn("User {} is not authorized to update projects in team {}", user.getId(), teamId);
            throw new RuntimeException("Only team admins can update projects");
        }

        TeamProject project = teamProjectRepo.findById(teamProjectId)
                .orElseThrow(() -> {
                    log.warn("Team project with id {} not found", teamProjectId);
                    return new RuntimeException("Team project not found");
                });

        // to update the fields
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            project.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            project.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            project.setStatus(dto.getStatus());
        }
        if (dto.getStartDate() != null) {
            project.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            project.setEndDate(dto.getEndDate());
        }

        TeamProject updatedProject = teamProjectRepo.save(project);
        entityManager.flush();

        log.info("Team project updated successfully - ID: {}", updatedProject.getId());

        // if additional documents were uploaded
        if (dto.getDocuments() != null && !dto.getDocuments().isEmpty()) {
            log.info("Uploading {} documents for project {}", dto.getDocuments().size(), updatedProject.getId());
            uploadDocumentsForProject(updatedProject, user, dto.getDocuments(), dto.getDocumentDescription());
            entityManager.flush();
            entityManager.refresh(updatedProject);
        }

        return buildProjectResponse(updatedProject, true, adminId);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'project:' + #teamProjectId")
    public TeamProjectResponse getProjectById(UUID teamProjectId, UUID userId, UUID teamId) {
        // ✅ Fetch the project and ensure the user is a member in one query
        TeamProject project = teamProjectRepo.findByIdAndUserIsTeamMember(teamProjectId, userId)
                .orElseThrow(() -> {
                    log.warn("Project {} not found or user {} is not a member", teamProjectId, userId);
                    return new RuntimeException("Project not found or access denied");
                });

        // ✅ Check that the project belongs to the specified team
        if (!project.getTeam().getId().equals(teamId)) {
            log.warn("Project {} does not belong to team {}", project.getId(), teamId);
            throw new RuntimeException("Project does not belong to the specified team");
        }

        log.info("User {} has access to project {} of team {}", userId, project.getTitle(), teamId);

        // ✅ Build and return the project response (with documents if needed)
        return buildProjectResponse(project, true, userId);
    }

    // get all team projects
    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_projects'")
    public List<TeamProjectResponse> getAllProjects(UUID userId) {
        log.info("Fetching all projects for team user");

        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id, {}", userId);
                    return new RuntimeException("User not found");
                });

        List<TeamProject> projects = teamProjectRepo.findAllByUserIsTeamMember(user.getId());

        if (projects == null) {
            log.warn("No projects found");
            return Collections.emptyList();
        }

        return projects.stream()
                .map(project -> buildProjectResponse(project, true, userId))
                .collect(Collectors.toList());
    }

    // to get all project documents, that the entire history of all documents uploaded
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "projectDocuments",
            key = "'user:' + #userId + '_allDocuments'"
    )
    public List<TeamProjectDocumentResponseDTO> getAllDocumentsForUser(UUID userId) {
        log.info("Fetching all projects for user ID: {}", userId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TeamProjectDocument> documents = teamprojectDocumentRepo.findAllByUserIsTeamMember(user.getId());

        if (documents == null) {
            log.warn("No documents found");
            return Collections.emptyList();
        }

        return documents.stream()
                .map(this::buildDocumentResponse)
                .collect(Collectors.toList());
    }

    // get all project documents
    public List<TeamProjectDocumentResponseDTO> getProjectDocuments(UUID projectId, UUID userId) {
        log.info("Fetching documents for project ID: {}", projectId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<TeamProjectDocument> documents = teamprojectDocumentRepo.findAllByProjectAndUserIsTeamMember(project.getId(), user.getId());

        if (documents == null) {
            log.warn("No documents found");
            return Collections.emptyList();
        }

        return documents.stream()
                .map(this::buildDocumentResponse)
                .collect(Collectors.toList());
    }

    // to delete a team project
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'project:' + #projectId"),
            @CacheEvict(key = "'project:' + #projectId + '_documents'", cacheNames = "projectDocuments"),
            @CacheEvict(key = "'project:' + #projectId + '_tasks'", cacheNames = "teamTasks"),
            @CacheEvict(allEntries = true, cacheNames = "teamProjects")  // Clear all project caches
    })
    public void deleteProject(UUID projectId, UUID userId) {
        log.info("Deleting project ID: {} for user ID: {}", projectId, userId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        teamProjectRepo.delete(project);
        log.info("Project deleted successfully - ID: {}", projectId);
    }

    // to upload documents to a team project
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "projectDocuments", key = "'project:' + #projectId + '_documents'"),
            @CacheEvict(cacheNames = "projectDocuments", key = "'user:' + #userId + '_allDocuments'"),
            @CacheEvict(key = "'project:' + #projectId")  // Project response includes document count
    })
    public List<TeamProjectDocumentResponseDTO> uploadDocument(UUID projectId, UUID userId, List<MultipartFile> files, String description, UUID teamId) throws IOException {
        log.info("Uploading {} documents to project ID: {}", files.size(), projectId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // to see if project belongs to the team
        if (!project.getTeam().getId().equals(team.getId())) {
            log.warn("Unauthorized access: project does not belong to team");
            throw new RuntimeException("Unauthorized access");
        }

        if (!isTeamAdminOrOwner(user.getId(), team)) {
            log.warn("User {} is not authorized to upload documents to team {}", user.getId(), teamId);
            throw new RuntimeException("Only team admins can upload documents");
        }

        List<TeamProjectDocumentResponseDTO> uploadDocs = uploadDocumentsForProject(project, user, files, description);
        entityManager.flush();
        entityManager.refresh(project);

        return uploadDocs;
    }

    // to delete project document
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "projectDocuments", key = "'project:' + #projectId + '_documents'"),
            @CacheEvict(cacheNames = "projectDocuments", key = "'user:' + #userId + '_allDocuments'"),
            @CacheEvict(key = "'project:' + #projectId")
    })
    public String deleteDocument(UUID documentId, UUID userId, UUID teamId, UUID projectId) {
        log.info("Deleting document ID: {} for user ID: {}", documentId, userId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // to see if project belongs to the team
        if (!project.getTeam().getId().equals(team.getId())) {
            log.warn("Unauthorized access: project does not belong to team");
            throw new RuntimeException("Unauthorized access");
        }

        if (!isTeamAdminOrOwner(user.getId(), team)) {
            log.warn("User {} is not authorized to delete documents in team {}", user.getId(), teamId);
            throw new RuntimeException("Only team admins can delete documents");
        }

        TeamProjectDocument document = teamprojectDocumentRepo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        document.markAsDeleted();
        teamprojectDocumentRepo.delete(document);

        log.info("Document deleted successfully");
        return "Document deleted successfully";
    }

    // view team project summary
    @Cacheable(key = "'team:' + #teamId + '_user:' + #userId + '_summary'")
    public List<TeamProjectSummaryDto> viewAllProjectSummary(UUID userId, UUID teamId) {
        log.info("Fetching all projects for team user");

        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id, {}", userId);
                    throw new RuntimeException("User not found");
                });

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> {
                    log.warn("Team not found with id, {}", teamId);
                    throw new RuntimeException("Team not found");
                });

        List<TeamProject> project = teamProjectRepo.findAllByTeamIdAndUserIsTeamMember(team.getId(), user.getId());

        return project.stream()
                .map(teamProject -> {
                    long daysLeft = 0;
                    if (teamProject.getEndDate() != null) {
                        daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), teamProject.getEndDate());
                    }

                    return TeamProjectSummaryDto.builder()
                            .id(teamProject.getId())
                            .name(teamProject.getTitle())
                            .description(teamProject.getDescription())
                            .dueDate(teamProject.getEndDate())
                            .daysLeft(daysLeft)
                            .build();
                }).collect(Collectors.toList());
    }

    // to count the number of projects
    @Cacheable(key = "'team:' + #teamId + '_user:' + #userId + '_count'")
    public long getProjectCount(UUID userId, UUID teamId) {
        log.info("Fetching project count for user and team");

        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id, {}", userId);
                    throw new RuntimeException("User not found");
                });

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> {
                    log.warn("Team not found with id, {}", teamId);
                    throw new RuntimeException("Team not found");
                });

        if (!team.getUser().getId().equals(userId)) {
            log.warn("Unauthorized access: user is not team owner");
            throw new RuntimeException("Unauthorized access");
        }

        return teamProjectRepo.countByTeamIdAndUserIsTeamMember(team.getId(), user.getId());
    }

    // count based on status
    @Cacheable(key = "'team:' + #teamId + '_user:' + #userId + '_countBasedOnStatus'")
    public long countBasedOnStatusProjects(UUID userId, UUID teamId, ProjectStatus status) {
        log.info("Fetching project count by status for user and team");

        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id, {}", userId);
                    throw new RuntimeException("User not found");
                });

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> {
                    log.warn("Team not found with id, {}", teamId);
                    throw new RuntimeException("Team not found");
                });

        if (!team.getUser().getId().equals(userId)) {
            log.warn("Unauthorized access: user is not team owner");
            throw new RuntimeException("Unauthorized access");
        }

        return teamProjectRepo.countByTeamIdAndUserAndStatus(team.getId(), user.getId(), status);
    }

    // view overdue projects using repository filtering
    @Cacheable(key = "'team:' + #teamId + '_user:' + #userId + '_overdue'")
    public List<TeamProjectSummaryDto> viewOverdueProjects(UUID userId, UUID teamId) {
        log.info("Fetching overdue projects for user {} in team {}", userId, teamId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id {}", userId);
                    return new RuntimeException("User not found");
                });

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> {
                    log.warn("Team not found with id {}", teamId);
                    return new RuntimeException("Team not found");
                });

        List<TeamProject> overdueProjects = teamProjectRepo.findOverdueProjects(team.getId(), user.getId());

        return overdueProjects.stream()
                .map(project -> {
                    long daysLeft = ChronoUnit.DAYS.between(
                            LocalDate.now(),
                            project.getEndDate()
                    );

                    return TeamProjectSummaryDto.builder()
                            .id(project.getId())
                            .name(project.getTitle())
                            .description(project.getDescription())
                            .dueDate(project.getEndDate())
                            .daysLeft(daysLeft) // negative for overdue
                            .build();
                })
                .toList();
    }

    // ==================== TEAM BASED TASKS ====================

    /**
     * Create a task for a team project
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "teamTasks", key = "'project:' + #projectId + '_tasks'"),
            @CacheEvict(cacheNames = "teamTasks", key = "'user:' + #userId + '_team:' + #teamId + '_tasks'"),
            @CacheEvict(cacheNames = "teamTasks", allEntries = true, condition = "#dto.assignedToUserId != null")
    })
    public TeamTaskResponseDTO createTask(UUID projectId, UUID userId, UUID teamId, TeamTaskCreateDTO dto) {
        log.info("Creating task for project ID: {} by user ID: {}", projectId, userId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify project belongs to team
        if (!project.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Project does not belong to the specified team");
        }

        // Verify user is team member or admin
        if (!isTeamMember(userId, team)) {
            throw new RuntimeException("Only team members can create tasks");
        }

        // Verify assigned user is team member if specified
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
        log.info("Task created successfully - ID: {}, Title: {}", savedTask.getId(), savedTask.getTitle());

        // Send email notification to assigned user
        if (assignedUser != null) {
            sendTaskAssignmentEmail(assignedUser, task, project);
        }

        return buildTaskResponse(savedTask);
    }

    /**
     * Update a task
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "teamTasks", key = "'task:' + #taskId"),
            @CacheEvict(cacheNames = "teamTasks", allEntries = true)  // Clear all task caches due to potential reassignments
    })
    public TeamTaskResponseDTO updateTask(UUID taskId, UUID userId, TeamTaskUpdateDTO dto) {
        log.info("Updating task ID: {} by user ID: {}", taskId, userId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamTask task = teamTaskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Verify user is team member
        if (!isTeamMember(userId, task.getTeamProject().getTeam())) {
            throw new RuntimeException("Only team members can update tasks");
        }

        boolean statusChanged = false;
        TaskStatus oldStatus = task.getStatus();

        // Update fields
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            task.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null && !dto.getStatus().equals(task.getStatus())) {
            task.setStatus(dto.getStatus());
            statusChanged = true;
        }
        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
        }
        if (dto.getDueDate() != null) {
            task.setDueDate(dto.getDueDate());
        }

        // Handle reassignment
        if (dto.getAssignedToUserId() != null) {
            User newAssignedUser = userRepo.findById(dto.getAssignedToUserId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));

            if (!isTeamMember(newAssignedUser.getId(), task.getTeamProject().getTeam())) {
                throw new RuntimeException("Can only assign tasks to team members");
            }

            User oldAssignedUser = task.getAssignedTo();
            task.setAssignedTo(newAssignedUser);

            // Send reassignment notification
            if (oldAssignedUser == null || !oldAssignedUser.getId().equals(newAssignedUser.getId())) {
                sendTaskReassignmentEmail(newAssignedUser, task, task.getTeamProject());
            }
        }

        task.setUpdatedAt(LocalDateTime.now());
        TeamTask updatedTask = teamTaskRepo.save(task);

        // Send status change notification
        if (statusChanged && task.getAssignedTo() != null) {
            sendTaskStatusChangeEmail(task.getAssignedTo(), task, oldStatus, task.getStatus());
        }

        log.info("Task updated successfully - ID: {}", updatedTask.getId());
        return buildTaskResponse(updatedTask);
    }

    /**
     * Delete a task
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "teamTasks", key = "'task:' + #taskId"),
            @CacheEvict(cacheNames = "teamTasks", allEntries = true)
    })
    public String deleteTask(UUID taskId, UUID userId) {
        log.info("Deleting task ID: {} by user ID: {}", taskId, userId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamTask task = teamTaskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Only creator or team admin can delete
        boolean isCreator = task.getCreatedBy().getId().equals(userId);
        boolean isAdmin = isTeamAdminOrOwner(userId, task.getTeamProject().getTeam());

        if (!isCreator && !isAdmin) {
            throw new RuntimeException("Only task creator or team admin can delete tasks");
        }

        teamTaskRepo.delete(task);
        log.info("Task deleted successfully - ID: {}", taskId);

        return "Task deleted successfully";
    }

    /**
     * Get all tasks for a project
     */
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "teamTasks",
            key = "'project:' + #projectId + '_tasks'"
    )
    public List<TeamTaskResponseDTO> getProjectTasks(UUID projectId, UUID userId) {
        log.info("Fetching tasks for project ID: {}", projectId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify user is team member
        if (!isTeamMember(userId, project.getTeam())) {
            throw new RuntimeException("Only team members can view tasks");
        }

        List<TeamTask> tasks = teamTaskRepo.findByTeamProjectId(projectId);

        return tasks.stream()
                .map(this::buildTaskResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get tasks assigned to a user
     */
    @Cacheable(
            cacheNames = "teamTasks",
            key = "'user:' + #userId + '_team:' + #teamId + '_tasks'"
    )
    @Transactional(readOnly = true)
    public List<TeamTaskResponseDTO> getUserTasks(UUID userId, UUID teamId) {
        log.info("Fetching tasks for user ID: {} in team ID: {}", userId, teamId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        List<TeamTask> tasks = teamTaskRepo.findByAssignedToIdAndTeamId(userId, teamId);

        return tasks.stream()
                .map(this::buildTaskResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get overdue tasks
     */
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "shortLivedCache",
            key = "'user:' + #userId + '_team:' + #teamId + '_overdueTasks'"
    )
    public List<TeamTaskResponseDTO> getOverdueTasks(UUID userId, UUID teamId) {
        log.info("Fetching overdue tasks for user ID: {} in team ID: {}", userId, teamId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        List<TeamTask> tasks = teamTaskRepo.findOverdueTasks(teamId, userId);

        return tasks.stream()
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
                .assignedToName(task.getAssignedTo() != null ?
                        task.getAssignedTo().getFirstName() + " " + task.getAssignedTo().getLastName() : null)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    // ==================== TEAM BASED REMINDERS ====================

    /**
     * Create a reminder for a task or project
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "teamReminders", key = "'user:' + #userId + '_team:' + #teamId + '_reminders'"),
            @CacheEvict(cacheNames = "teamReminders", key = "'project:' + #dto.projectId + '_reminders'", condition = "#dto.projectId != null"),
            @CacheEvict(cacheNames = "teamReminders", key = "'task:' + #dto.taskId + '_reminders'", condition = "#dto.taskId != null")
    })
    public TeamReminderResponseDTO createReminder(UUID userId, UUID teamId, TeamReminderCreateDTO dto) {
        log.info("Creating reminder for user ID: {}", userId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Verify user is team member
        if (!isTeamMember(userId, team)) {
            throw new RuntimeException("Only team members can create reminders");
        }

        TeamProject project = null;
        TeamTask task = null;

        if (dto.getProjectId() != null) {
            project = teamProjectRepo.findById(dto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            if (!project.getTeam().getId().equals(teamId)) {
                throw new RuntimeException("Project does not belong to the team");
            }
        }

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
        log.info("Reminder created successfully - ID: {}", savedReminder.getId());

        return buildReminderResponse(savedReminder);
    }

    /**
     * Update a reminder
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "teamReminders", allEntries = true)  // Simplified due to complexity
    })
    public TeamReminderResponseDTO updateReminder(UUID reminderId, UUID userId, TeamReminderUpdateDTO dto) {
        log.info("Updating reminder ID: {} by user ID: {}", reminderId, userId);

        TeamReminder reminder = teamReminderRepo.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));

        // Only creator can update
        if (!reminder.getUser().getId().equals(userId)) {
            throw new RuntimeException("Only reminder creator can update it");
        }

        if (dto.getTitle() != null) {
            reminder.setTitle(dto.getTitle());
        }
        if (dto.getMessage() != null) {
            reminder.setMessage(dto.getMessage());
        }
        if (dto.getReminderDateTime() != null) {
            reminder.setReminderDateTime(dto.getReminderDateTime());
        }
        if (dto.getIsRecurring() != null) {
            reminder.setRecurring(dto.getIsRecurring());
        }
        if (dto.getRecurringInterval() != null) {
            reminder.setRecurringInterval(dto.getRecurringInterval());
        }
        if (dto.getIsActive() != null) {
            reminder.setActive(dto.getIsActive());
        }

        TeamReminder updatedReminder = teamReminderRepo.save(reminder);
        log.info("Reminder updated successfully - ID: {}", updatedReminder.getId());

        return buildReminderResponse(updatedReminder);
    }

    /**
     * Delete a reminder
     */
    @Transactional
    @CacheEvict(cacheNames = "teamReminders", allEntries = true)
    public String deleteReminder(UUID reminderId, UUID userId) {
        log.info("Deleting reminder ID: {} by user ID: {}", reminderId, userId);

        TeamReminder reminder = teamReminderRepo.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));

        // Only creator can delete
        if (!reminder.getUser().getId().equals(userId)) {
            throw new RuntimeException("Only reminder creator can delete it");
        }

        teamReminderRepo.delete(reminder);
        log.info("Reminder deleted successfully - ID: {}", reminderId);

        return "Reminder deleted successfully";
    }

    /**
     * Get all reminders for a user in a team
     */
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "teamReminders",
            key = "'user:' + #userId + '_team:' + #teamId + '_reminders'"
    )
    public List<TeamReminderResponseDTO> getUserReminders(UUID userId, UUID teamId) {
        log.info("Fetching reminders for user ID: {} in team ID: {}", userId, teamId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        List<TeamReminder> reminders = teamReminderRepo.findByUserIdAndTeamIdAndIsActiveTrue(userId, teamId);

        return reminders.stream()
                .map(this::buildReminderResponse)
                .collect(Collectors.toList());
    }


    @Cacheable(
            cacheNames = "teamReminders",
            key = "'project:' + #projectId + '_reminders'"
    )
    @Transactional(readOnly = true)
    public List<TeamReminderResponseDTO> getProjectReminders(UUID projectId, UUID userId) {
        log.info("Fetching reminders for project ID: {}", projectId);

        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify user is team member
        if (!isTeamMember(userId, project.getTeam())) {
            throw new RuntimeException("Only team members can view reminders");
        }

        List<TeamReminder> reminders = teamReminderRepo.findByTeamProjectIdAndIsActiveTrue(projectId);

        return reminders.stream()
                .map(this::buildReminderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get reminders for a specific task
     */
    @Cacheable(
            cacheNames = "teamReminders",
            key = "'task:' + #taskId + '_reminders'"
    )
    @Transactional(readOnly = true)
    public List<TeamReminderResponseDTO> getTaskReminders(UUID taskId, UUID userId) {
        log.info("Fetching reminders for task ID: {}", taskId);

        TeamTask task = teamTaskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Verify user is team member
        if (!isTeamMember(userId, task.getTeamProject().getTeam())) {
            throw new RuntimeException("Only team members can view reminders");
        }

        List<TeamReminder> reminders = teamReminderRepo.findByTeamTaskIdAndIsActiveTrue(taskId);

        return reminders.stream()
                .map(this::buildReminderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Scheduled job to send reminders (runs every minute)
     */
    @Scheduled(cron = "0 * * * * *") // Every minute
    @Transactional
    public void sendScheduledReminders() {
        log.info("Running scheduled reminder check...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMinuteAhead = now.plusMinutes(1);

        List<TeamReminder> dueReminders = teamReminderRepo.findDueReminders(now, oneMinuteAhead);

        log.info("Found {} reminders to send", dueReminders.size());

        for (TeamReminder reminder : dueReminders) {
            try {
                sendReminderEmail(reminder);
                reminder.setSent(true);

                // Handle recurring reminders
                if (reminder.isRecurring() && reminder.getRecurringInterval() != null) {
                    LocalDateTime nextReminder = calculateNextReminderTime(
                            reminder.getReminderDateTime(),
                            reminder.getRecurringInterval()
                    );
                    reminder.setReminderDateTime(nextReminder);
                    reminder.setSent(false);
                } else {
                    reminder.setActive(false);
                }

                teamReminderRepo.save(reminder);
                log.info("Reminder sent successfully - ID: {}", reminder.getId());

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

    // ==================== TEAM MESSAGES THROUGH EMAIL ====================

    /**
     * Send a message to team members via email
     */
    @Transactional
    public String sendTeamMessage(UUID userId, UUID teamId, TeamMessageDTO dto) {
        log.info("Sending team message from user ID: {} to team ID: {}", userId, teamId);

        User sender = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Verify sender is team member
        if (!isTeamMember(userId, team)) {
            throw new RuntimeException("Only team members can send team messages");
        }

        // Get recipient emails
        List<String> recipientEmails = new ArrayList<>();

        if (dto.isSendToAll()) {
            // Send to all team members except sender
            recipientEmails = team.getMembers().stream()
                    .map(m -> m.getUser().getEmail())
                    .filter(email -> !email.equals(sender.getEmail()))
                    .collect(Collectors.toList());

            // Include team owner if not already included
            if (!team.getUser().getEmail().equals(sender.getEmail())) {
                recipientEmails.add(team.getUser().getEmail());
            }
        } else if (dto.getRecipientUserIds() != null && !dto.getRecipientUserIds().isEmpty()) {
            // Send to specific members
            for (UUID recipientId : dto.getRecipientUserIds()) {
                User recipient = userRepo.findById(recipientId)
                        .orElseThrow(() -> new RuntimeException("Recipient user not found: " + recipientId));

                // Verify recipient is team member
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

        // Prepare email content
        String subject = String.format("[%s] %s", team.getName(), dto.getSubject());
        String body = String.format(
                "From: %s %s (%s)\nTeam: %s\n\n%s",
                sender.getFirstName(),
                sender.getLastName(),
                sender.getEmail(),
                team.getName(),
                dto.getMessage()
        );

        // Send emails
        int successCount = 0;
        for (String email : recipientEmails) {
            try {
                emailService.sendEmail(email, subject, body);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send email to: {}", email, e);
            }
        }

        log.info("Team message sent successfully to {}/{} recipients", successCount, recipientEmails.size());

        return String.format("Message sent successfully to %d/%d recipients",
                successCount, recipientEmails.size());
    }

    /**
     * Send a project update to all team members
     */
    @Transactional
    public String sendProjectUpdate(UUID projectId, UUID userId, ProjectUpdateMessageDTO dto) {
        log.info("Sending project update for project ID: {}", projectId);

        User sender = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TeamProject project = teamProjectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Team team = project.getTeam();

        // Verify sender is team member
        if (!isTeamMember(userId, team)) {
            throw new RuntimeException("Only team members can send project updates");
        }

        // Get all team member emails except sender
        List<String> recipientEmails = team.getMembers().stream()
                .map(m -> m.getUser().getEmail())
                .filter(email -> !email.equals(sender.getEmail()))
                .collect(Collectors.toList());

        if (!team.getUser().getEmail().equals(sender.getEmail())) {
            recipientEmails.add(team.getUser().getEmail());
        }

        // Prepare email
        String subject = String.format("[%s] Project Update: %s", team.getName(), project.getTitle());
        String body = String.format(
                "Project: %s\nFrom: %s %s\n\n%s\n\nProject Status: %s\nDue Date: %s",
                project.getTitle(),
                sender.getFirstName(),
                sender.getLastName(),
                dto.getMessage(),
                project.getStatus(),
                project.getEndDate()
        );

        // Send emails
        int successCount = 0;
        for (String email : recipientEmails) {
            try {
                emailService.sendEmail(email, subject, body);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send project update to: {}", email, e);
            }
        }

        log.info("Project update sent to {}/{} team members", successCount, recipientEmails.size());

        return String.format("Project update sent to %d/%d team members",
                successCount, recipientEmails.size());
    }

    /**
     * Send task notification via email
     */
    private void sendTaskAssignmentEmail(User assignedUser, TeamTask task, TeamProject project) {
        try {
            String subject = String.format("New Task Assigned: %s", task.getTitle());
            String body = String.format(
                    "Hi %s,\n\nYou have been assigned a new task:\n\nTask: %s\nProject: %s\nDescription: %s\nDue Date: %s\nPriority: %s\n\nPlease log in to view details.",
                    assignedUser.getFirstName(),
                    task.getTitle(),
                    project.getTitle(),
                    task.getDescription(),
                    task.getDueDate(),
                    task.getPriority()
            );

            emailService.sendEmail(assignedUser.getEmail(), subject, body);
            log.info("Task assignment email sent to: {}", assignedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send task assignment email", e);
        }
    }

    private void sendTaskReassignmentEmail(User newAssignedUser, TeamTask task, TeamProject project) {
        try {
            String subject = String.format("Task Reassigned: %s", task.getTitle());
            String body = String.format(
                    "Hi %s,\n\nA task has been reassigned to you:\n\nTask: %s\nProject: %s\nDescription: %s\nDue Date: %s\nPriority: %s\n\nPlease log in to view details.",
                    newAssignedUser.getFirstName(),
                    task.getTitle(),
                    project.getTitle(),
                    task.getDescription(),
                    task.getDueDate(),
                    task.getPriority()
            );

            emailService.sendEmail(newAssignedUser.getEmail(), subject, body);
            log.info("Task reassignment email sent to: {}", newAssignedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send task reassignment email", e);
        }
    }

    private void sendTaskStatusChangeEmail(User assignedUser, TeamTask task, TaskStatus oldStatus, TaskStatus newStatus) {
        try {
            String subject = String.format("Task Status Updated: %s", task.getTitle());
            String body = String.format(
                    "Hi %s,\n\nThe status of your task has been updated:\n\nTask: %s\nOld Status: %s\nNew Status: %s\n\nPlease log in to view details.",
                    assignedUser.getFirstName(),
                    task.getTitle(),
                    oldStatus,
                    newStatus
            );

            emailService.sendEmail(assignedUser.getEmail(), subject, body);
            log.info("Task status change email sent to: {}", assignedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send task status change email", e);
        }
    }

    private void sendReminderEmail(TeamReminder reminder) {
        try {
            String subject = String.format("Reminder: %s", reminder.getTitle());
            String body = String.format(
                    "Hi %s,\n\nThis is a reminder:\n\n%s\n\n",
                    reminder.getUser().getFirstName(),
                    reminder.getMessage()
            );

            if (reminder.getTeamProject() != null) {
                body += String.format("Related Project: %s\n", reminder.getTeamProject().getTitle());
            }

            if (reminder.getTeamTask() != null) {
                body += String.format("Related Task: %s\n", reminder.getTeamTask().getTitle());
            }

            emailService.sendEmail(reminder.getUser().getEmail(), subject, body);
            log.info("Reminder email sent to: {}", reminder.getUser().getEmail());
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