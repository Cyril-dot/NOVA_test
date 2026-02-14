package com.novaTech.Nova.Services;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Project;
import com.novaTech.Nova.Entities.ProjectDocument;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.Enums.DocumentType;
import com.novaTech.Nova.Entities.Enums.ProjectStatus;
import com.novaTech.Nova.Entities.repo.ProjectDocumentRepo;
import com.novaTech.Nova.Entities.repo.ProjectRepo;
import com.novaTech.Nova.Entities.repo.UserRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@CacheConfig(cacheNames = "projects")
public class ProjectService {

    private final ProjectRepo projectRepo;
    private final ProjectDocumentRepo projectDocumentRepo;
    private final UserRepo userRepo;
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

    // ========================
    // CREATE PROJECT
    // ========================
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'owner:' + #ownerId + '_userProjects'")
    })
    public ProjectResponseDTO createProject(UUID ownerId, ProjectCreateDTO dto) throws IOException {
        log.info("Creating project for user ID: {}", ownerId);

        User owner = userRepo.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create project
        Project project = Project.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .user(owner)
                .status(dto.getStatus() != null ? dto.getStatus() : ProjectStatus.ACTIVE)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();

        Project savedProject = projectRepo.save(project);
        entityManager.flush(); // ✅ Ensure project is persisted

        log.info("Project created successfully - ID: {}, Name: {}", savedProject.getId(), savedProject.getTitle());

        // Send project creation email
            log.info("Sending project creation email to: {}", owner.getEmail());
            emailService.createProjectMail(owner.getEmail(), savedProject.getTitle());
            log.info("Project creation email sent successfully to: {}", owner.getEmail());

        // Upload documents if provided
        if (dto.getDocuments() != null && !dto.getDocuments().isEmpty()) {
            log.info("Uploading {} documents for project {}", dto.getDocuments().size(), savedProject.getId());
            uploadDocumentsForProject(savedProject, owner, dto.getDocuments(), dto.getDocumentDescription());

            entityManager.flush();    // ✅ Ensure documents are persisted
            entityManager.refresh(savedProject); // ✅ Refresh project to load documents
        }

        return buildProjectResponse(savedProject, true);
    }


    // ========================
    // UPDATE PROJECT
    // ========================
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(key = "'owner:' + #ownerId + '_userProjects'"),
                    @CacheEvict(key = "'project:' + #projectId + '_projectDocuments'")
            }
    )
    public ProjectResponseDTO updateProject(UUID projectId, UUID ownerId, ProjectUpdateDTO dto) throws IOException {
        log.info("Updating project ID: {} for user ID: {}", projectId, ownerId);

        User owner = userRepo.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepo.findByIdAndUser(projectId, owner)
                .orElseThrow(() -> new RuntimeException("Project not found or you don't have permission"));

        boolean statusChanged = false;
        ProjectStatus newStatus = null;

        // Update fields
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            project.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            project.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null && !dto.getStatus().equals(project.getStatus())) {
            statusChanged = true;
            newStatus = dto.getStatus();
            project.setStatus(dto.getStatus());
        }
        if (dto.getStartDate() != null) {
            project.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            project.setEndDate(dto.getEndDate());
        }

        Project updatedProject = projectRepo.save(project);
        entityManager.flush();

        log.info("Project updated successfully - ID: {}", updatedProject.getId());

        // Send status update email if status changed
        if (statusChanged) {
                log.info("Sending project status update email to: {}", owner.getEmail());
                emailService.updateProjectMail(owner.getEmail(), updatedProject.getTitle(), newStatus);
                log.info("Project status update email sent successfully to: {}", owner.getEmail());
        }

        // Upload additional documents if provided
        if (dto.getDocuments() != null && !dto.getDocuments().isEmpty()) {
            log.info("Uploading {} additional documents for project {}", dto.getDocuments().size(), updatedProject.getId());
            uploadDocumentsForProject(updatedProject, owner, dto.getDocuments(), dto.getDocumentDescription());

            entityManager.flush();
            entityManager.refresh(updatedProject);
        }

        return buildProjectResponse(updatedProject, true);
    }


    // ========================
    // GET PROJECT BY ID
    // ========================
    @Transactional(readOnly = true)
    @Cacheable(key = "'project:' + #projectId + '_projectDetails' + #includeDocuments + 'user:' + #ownerId + '_userProjects'")
    public ProjectResponseDTO getProjectById(UUID projectId, UUID ownerId, boolean includeDocuments) {
        log.info("Fetching project ID: {} for user ID: {}", projectId, ownerId);

        User owner = userRepo.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepo.findByIdAndUser(projectId, owner)
                .orElseThrow(() -> new RuntimeException("Project not found or you don't have permission"));

        return buildProjectResponse(project, includeDocuments);
    }

    // ========================
    // GET ALL PROJECTS FOR USER
    // ========================
    @Caching(cacheable = {
            @Cacheable(key = "'owner:' + #ownerId + '_userProjects'"),
            @Cacheable(key = "'user:' + #ownerId + '_projects'"),
            @Cacheable(key = "'includeDocuments:' + #includeDocuments + '_user:' + #ownerId + '_projects'")
    })
    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getAllProjects(UUID ownerId, ProjectStatus status, boolean includeDocuments) {
        log.info("Fetching all projects for user ID: {}, status: {}", ownerId, status);

        User owner = userRepo.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Project> projects;
        if (status != null) {
            projects = projectRepo.findByUserAndStatus(owner, status);
        } else {
            projects = projectRepo.findByUser(owner);
        }

        return projects.stream()
                .map(project -> buildProjectResponse(project, includeDocuments))
                .collect(Collectors.toList());
    }

    // get all documents
    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_documents'")
    public List<ProjectResponseDTO> getAllDocumentsForUser(UUID userId) {
        log.info("Fetching all projects for user ID: {}", userId);
        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Project> projects = projectRepo.findByUser(owner);
        if (projects.isEmpty()) {
            log.info("No projects found for user {}", userId);
            return Collections.emptyList();
        }

        boolean includeDocuments = true;

        return projects.stream()
                .map(project -> buildProjectResponse(project, includeDocuments))
                .collect(Collectors.toList());

    }


    // view most recent projects
    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_projects'")
    public List<ProjectResponseDTO> getOrderedProjects(UUID userId) {
        log.info("Fetching all projects for user ID: {}", userId);
        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Project> projects = projectRepo.findByUserOrderByCreatedAtDesc(owner);
        if (projects.isEmpty()) {
            log.info("No projects found for user {}", userId);
            return Collections.emptyList();
        }
        boolean includeDocuments = true;
        return projects.stream()
                .map(project -> buildProjectResponse(project, includeDocuments))
                .collect(Collectors.toList());
    }


    // ========================
    // DELETE PROJECT
    // ========================
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'owner:' + #ownerId + '_userProjects'"),
            @CacheEvict(key = "'project:' + #projectId + '_projectDocuments'")
    })
    public void deleteProject(UUID projectId, UUID ownerId) {
        log.info("Deleting project ID: {} for user ID: {}", projectId, ownerId);

        User owner = userRepo.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepo.findByIdAndUser(projectId, owner)
                .orElseThrow(() -> new RuntimeException("Project not found or you don't have permission"));

        String projectName = project.getTitle();

        projectRepo.delete(project);
        log.info("Project deleted successfully - ID: {}", projectId);

        // Send project deletion email
            log.info("Sending project deletion email to: {}", owner.getEmail());
            emailService.deleteProjectMail(owner.getEmail(), projectName);
            log.info("Project deletion email sent successfully to: {}", owner.getEmail());
    }



    // ========================
    // UPLOAD DOCUMENTS TO PROJECT
    // ========================
    @Transactional
    public List<ProjectDocumentResponseDTO> uploadDocumentsToProject(UUID projectId, UUID userId, List<MultipartFile> files, String description) throws IOException {
        log.info("Uploading {} documents to project ID: {}", files.size(), projectId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Check if user is the owner
        if (!project.getUser().getId().equals(userId)) {
            throw new RuntimeException("Only project owner can upload documents");
        }

        List<ProjectDocumentResponseDTO> uploadedDocs = uploadDocumentsForProject(project, user, files, description);

        entityManager.flush();
        entityManager.refresh(project);

        // Send document upload email
            log.info("Sending document upload notification email to: {}", user.getEmail());
            emailService.uploadDocumentsToDocument(user.getEmail(), project.getTitle());
            log.info("Document upload email sent successfully to: {}", user.getEmail());

        return uploadedDocs;
    }

    // ========================
    // GET ALL DOCUMENTS FOR PROJECT
    // ========================
    @Transactional(readOnly = true)
    @Cacheable(key = "'project:' + #projectId + '_projectDocuments'")
    public List<ProjectDocumentResponseDTO> getProjectDocuments(UUID projectId, UUID userId, DocumentType type) {
        log.info("Fetching documents for project ID: {}, type: {}", projectId, type);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Check if user is the owner
        if (!project.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to access project documents");
        }

        List<ProjectDocument> documents;
        if (type != null) {
            documents = projectDocumentRepo.findByProjectAndDocumentTypeAndIsDeletedFalse(project, type);
        } else {
            documents = projectDocumentRepo.findByProjectAndIsDeletedFalse(project);
        }

        return documents.stream()
                .map(this::buildDocumentResponse)
                .collect(Collectors.toList());
    }

    // ========================
    // DELETE DOCUMENT
    // ========================
    @Transactional
    @Caching(evict ={
            @CacheEvict(key = "'project:' + #documentId + '_projectDocuments'"),
            @CacheEvict(key = "'user:' + #userId + '_documents'")
    })
    public void deleteDocument(UUID documentId, UUID userId) {
        log.info("Deleting document ID: {} for user ID: {}", documentId, userId);

        ProjectDocument document = projectDocumentRepo.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Check if user is the owner
        if (!document.getProject().getUser().getId().equals(userId)) {
            throw new RuntimeException("Only project owner can delete documents");
        }

        document.markAsDeleted();
        projectDocumentRepo.save(document);
        log.info("Document marked as deleted - ID: {}", documentId);
    }

    // ========================
    // HELPER: Upload Documents
    // ========================
    private List<ProjectDocumentResponseDTO> uploadDocumentsForProject(Project project, User user, List<MultipartFile> files, String description) throws IOException {
        List<ProjectDocumentResponseDTO> uploadedDocuments = new ArrayList<>();

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

                ProjectDocument document = ProjectDocument.builder()
                        .fileName(fileName)
                        .originalFileName(file.getOriginalFilename())
                        .fileContent(base64Content)
                        .documentType(docType)
                        .fileSize(file.getSize())
                        .mimeType(mimeType)
                        .description(description)
                        .project(project)
                        .uploadedBy(user)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                log.info("💾 Saving document to database...");
                ProjectDocument savedDocument = projectDocumentRepo.save(document);

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

    // ========================
    // HELPER: Build Project Response
    // ========================
    private ProjectResponseDTO buildProjectResponse(Project project, boolean includeDocuments) {
        log.info("🔨 Building project response for project ID: {}, includeDocuments: {}",
                project.getId(), includeDocuments);

        // ✅ FIX: Query documents directly from repository instead of lazy-loaded collection
        List<ProjectDocument> documents = new ArrayList<>();
        long documentCount = 0;

        if (includeDocuments) {
            documents = projectDocumentRepo.findByProjectAndIsDeletedFalse(project);
            documentCount = documents.size();
            log.info("📊 Found {} active documents for project {}", documentCount, project.getId());
        } else {
            documentCount = projectDocumentRepo.countByProjectAndIsDeletedFalse(project);
            log.info("📊 Document count for project {}: {}", project.getId(), documentCount);
        }

        ProjectResponseDTO.ProjectResponseDTOBuilder builder = ProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getTitle())
                .description(project.getDescription())
                .ownerId(project.getUser().getId())
                .ownerEmail(project.getUser().getEmail())
                .ownerName(project.getUser().getFirstName() + " " + project.getUser().getLastName())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .documentCount(documentCount);

        if (includeDocuments && !documents.isEmpty()) {
            List<ProjectDocumentResponseDTO> documentDTOs = documents.stream()
                    .map(this::buildDocumentResponse)
                    .collect(Collectors.toList());
            builder.documents(documentDTOs);
            log.info("📎 Added {} documents to response", documentDTOs.size());
        } else {
            builder.documents(new ArrayList<>()); // Empty list instead of null
        }

        ProjectResponseDTO response = builder.build();
        log.info("✅ Project response built successfully - Documents included: {}", response.getDocumentCount());

        return response;
    }


    // saerch for project
    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_projects'")
    public List<ProjectResponseDTO> searchForProject(UUID userId, String keyword){
        log.info("Searching projects for user ID: {} with keyword: {}", userId, keyword);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new RuntimeException("User not found");
                });
        List<Project> projects = projectRepo.searchByUserAndKeyword(user, keyword);
        if (projects == null || projects.isEmpty()){
            log.warn("No projects found for user ID: {} with keyword: {}", userId, keyword);
            return List.of();
        }

        List<ProjectResponseDTO> response = projects.stream()
                .map(project -> buildProjectResponse(project, true))
                .collect(Collectors.toList());

        log.info("Successfully found {} projects for user ID: {} with keyword: {}", response.size(), userId, keyword);
        return response;
    }

    // ========================
    // HELPER: Build Document Response
    // ========================
    private ProjectDocumentResponseDTO buildDocumentResponse(ProjectDocument document) {
        return ProjectDocumentResponseDTO.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .originalFileName(document.getOriginalFileName())
                .documentType(document.getDocumentType())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .description(document.getDescription())
                .projectId(document.getProject().getId())
                .projectName(document.getProject().getTitle())
                .uploadedById(document.getUploadedBy().getId())
                .uploadedByEmail(document.getUploadedBy().getEmail())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    // ========================
    // HELPER: Get File Extension
    // ========================
    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring   (filename.lastIndexOf("."));
        }
        return "";
    }

    // ========================
    // NEW METHODS FOR SUMMARY AND STATS
    // ========================

    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_projects'")
    public List<ProjectSummaryDTO> viewAllProjectsSummary(UUID userId) {
        log.info("Fetching project summaries for user ID: {}", userId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Project> projects = projectRepo.findByUser(user);

        return projects.stream()
                .map(project -> {
                    long daysLeft = 0;
                    if (project.getEndDate() != null) {
                        daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), project.getEndDate());
                    }
                    return ProjectSummaryDTO.builder()
                            .id(project.getId())
                            .name(project.getTitle())
                            .description(project.getDescription())
                            .dueDate(project.getEndDate())
                            .daysLeft(daysLeft)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_projectCount'")
    public long getProjectCount(UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return projectRepo.countByUser(user);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_completedProjectCount'")
    public long getCompletedProjectCount(UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return projectRepo.countByUserAndStatus(user, ProjectStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_inProgressProjectCount'")
    public long getInProgressProjectCount(UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return projectRepo.countByUserAndStatus(user, ProjectStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId + '_overdueProjectCount'")
    public List<ProjectSummaryDTO> viewOverdueProjects(UUID userId) {
        log.info("Fetching overdue projects for user ID: {}", userId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Project> projects = projectRepo.findByUser(user);

        return projects.stream()
                .filter(project -> project.getEndDate() != null && project.getEndDate().isBefore(LocalDate.now()))
                .map(project -> {
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), project.getEndDate());
                    return ProjectSummaryDTO.builder()
                            .id(project.getId())
                            .name(project.getTitle())
                            .description(project.getDescription())
                            .dueDate(project.getEndDate())
                            .daysLeft(daysLeft)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
