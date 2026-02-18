package com.novaTech.Nova.Services.workSpace;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.DocType;
import com.novaTech.Nova.Entities.Enums.TeamStatus;
import com.novaTech.Nova.Entities.Team;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.TeamRepository;
import com.novaTech.Nova.Entities.repo.UserRepo;
import com.novaTech.Nova.Entities.repo.WorkSpaceRepo;
import com.novaTech.Nova.Entities.workSpace.WorkSpaceDocs;
import com.novaTech.Nova.Entities.workSpace.WorkSpaceContribution;
import com.novaTech.Nova.Entities.repo.WorkSpaceContributionRepo;
import com.novaTech.Nova.Exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeamWorkSpaceService {
    
    private final UserRepo userRepo;
    private final WorkSpaceRepo workSpaceRepo;
    private final TeamRepository teamRepository;
    private final WorkSpaceContributionRepo contributionRepo;

    // ==================== HELPER METHODS ====================

    private User getUserById(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Team getTeamById(UUID teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
    }

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

    // ==================== WORKSPACE CREATION (ADMIN ONLY) ====================

    /**
     * Create a team workspace - Only admins can create
     */
    @Transactional
    public WorkSpaceCreationResponse createTeamWorkspace(UUID userId, UUID teamId, WorkSpaceCreationDto creationDto) {
        log.info("Creating team workspace for team id {}, by user id {}", teamId, userId);

        User user = getUserById(userId);
        Team team = getTeamById(teamId);

        // Verify user is admin or owner
        if (!isTeamAdminOrOwner(userId, team)) {
            log.error("User {} is not authorized to create workspace in team {}", userId, teamId);
            throw new UnauthorizedException("Only team admins can create workspaces");
        }

        WorkSpaceDocs workSpaceDocs = new WorkSpaceDocs();
        workSpaceDocs.setTeam(team);
        workSpaceDocs.setUser(user); // Creator
        workSpaceDocs.setTitle(creationDto.title());
        workSpaceDocs.setDescription(creationDto.description());
        workSpaceDocs.setCreatedAt(LocalDateTime.now());

        workSpaceRepo.save(workSpaceDocs);

        return workSpaceBuilderResponse(workSpaceDocs);
    }

    private WorkSpaceCreationResponse workSpaceBuilderResponse(WorkSpaceDocs workSpaceDocs) {
        log.info("Building workspace creation response");
        return new WorkSpaceCreationResponse(
                workSpaceDocs.getId(),
                workSpaceDocs.getTitle(),
                workSpaceDocs.getDescription(),
                workSpaceDocs.getUser().getUsername(),
                workSpaceDocs.getUser().getEmail(),
                workSpaceDocs.getCreatedAt(),
                "Team workspace created successfully"
        );
    }

    // ==================== TEMPLATE CREATION (ADMIN ONLY) ====================

    /**
     * Create template for workspace - Only admins can create templates
     */
    @Transactional
    public ActiveWorkSpaceDocs createWorkSpaceTemplate(DocType docType, UUID userId, Long docId, UUID teamId) {
        User user = getUserById(userId);
        Team team = getTeamById(teamId);

        // Verify user is admin or owner
        if (!isTeamAdminOrOwner(userId, team)) {
            log.error("User {} is not authorized to create template in team {}", userId, teamId);
            throw new UnauthorizedException("Only team admins can create templates");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new NoSuchElementException("Workspace with id " + docId + " not found"));

        // Verify workspace belongs to team
        if (!docs.getTeam().getId().equals(teamId)) {
            throw new UnauthorizedException("Workspace does not belong to this team");
        }

        String template = getTemplateForDocType(docType);

        docs.setWorkSpaceData(template.getBytes(StandardCharsets.UTF_8));
        docs.setDocType(docType);
        docs.setUpdatedAt(LocalDateTime.now());
        docs.setLastViewed(LocalDateTime.now());
        workSpaceRepo.save(docs);

        return new ActiveWorkSpaceDocs(
                docs.getId(),
                docs.getTitle(),
                docs.getDescription(),
                template,
                docType
        );
    }

    // ==================== TEAM MEMBER CONTRIBUTIONS ====================

    /**
     * Team member submits contribution - Requires admin approval
     */
    @Transactional
    public WorkSpaceContributionResponse submitContribution(UUID userId, Long docId, UUID teamId, UpdateWorkSpaceDocsDto updateDto) {
        log.info("User {} submitting contribution to workspace {}", userId, docId);

        User user = getUserById(userId);
        Team team = getTeamById(teamId);

        // Verify user is team member
        if (!isTeamMember(userId, team)) {
            throw new UnauthorizedException("Only team members can contribute");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new NoSuchElementException("Workspace with id " + docId + " not found"));

        // Verify workspace belongs to team
        if (!docs.getTeam().getId().equals(teamId)) {
            throw new UnauthorizedException("Workspace does not belong to this team");
        }

        // Create contribution record
        WorkSpaceContribution contribution = new WorkSpaceContribution();
        contribution.setWorkSpace(docs);
        contribution.setContributor(user);
        contribution.setContributionData(updateDto.content().getBytes(StandardCharsets.UTF_8));
        contribution.setSubmittedAt(LocalDateTime.now());
        contribution.setApproved(false);
        contribution.setPending(true);

        contributionRepo.save(contribution);

        log.info("Contribution submitted successfully - ID: {}", contribution.getId());

        return WorkSpaceContributionResponse.builder()
                .contributionId(contribution.getId())
                .workspaceId(docs.getId())
                .workspaceName(docs.getTitle())
                .contributorName(user.getUsername())
                .contributorEmail(user.getEmail())
                .submittedAt(contribution.getSubmittedAt())
                .status("PENDING")
                .message("Contribution submitted successfully. Awaiting admin approval.")
                .build();
    }

    /**
     * Admin approves contribution and merges it to main workspace
     */
    @Transactional
    public ActiveWorkSpaceDocs approveContribution(UUID adminId, Long contributionId, UUID teamId) {
        log.info("Admin {} approving contribution {}", adminId, contributionId);

        User admin = getUserById(adminId);
        Team team = getTeamById(teamId);

        // Verify admin permissions
        if (!isTeamAdminOrOwner(adminId, team)) {
            throw new UnauthorizedException("Only team admins can approve contributions");
        }

        WorkSpaceContribution contribution = contributionRepo.findById(contributionId)
                .orElseThrow(() -> new NoSuchElementException("Contribution not found"));

        WorkSpaceDocs docs = contribution.getWorkSpace();

        // Verify workspace belongs to team
        if (!docs.getTeam().getId().equals(teamId)) {
            throw new UnauthorizedException("Contribution does not belong to this team");
        }

        // Get existing content
        String existingContent = new String(docs.getWorkSpaceData(), StandardCharsets.UTF_8);
        String contributionContent = new String(contribution.getContributionData(), StandardCharsets.UTF_8);

        // Merge contribution into existing content
        String updatedContent = updateContentWithinTemplate(existingContent, contributionContent, docs.getDocType());

        docs.setWorkSpaceData(updatedContent.getBytes(StandardCharsets.UTF_8));
        docs.setUpdatedAt(LocalDateTime.now());
        workSpaceRepo.save(docs);

        // Mark contribution as approved
        contribution.setApproved(true);
        contribution.setPending(false);
        contribution.setApprovedBy(admin);
        contribution.setApprovedAt(LocalDateTime.now());
        contributionRepo.save(contribution);

        log.info("Contribution approved and merged successfully");

        return new ActiveWorkSpaceDocs(
                docs.getId(),
                docs.getTitle(),
                docs.getDescription(),
                updatedContent,
                docs.getDocType()
        );
    }

    /**
     * Admin rejects contribution
     */
    @Transactional
    public String rejectContribution(UUID adminId, Long contributionId, UUID teamId, String reason) {
        log.info("Admin {} rejecting contribution {}", adminId, contributionId);

        User admin = getUserById(adminId);
        Team team = getTeamById(teamId);

        // Verify admin permissions
        if (!isTeamAdminOrOwner(adminId, team)) {
            throw new UnauthorizedException("Only team admins can reject contributions");
        }

        WorkSpaceContribution contribution = contributionRepo.findById(contributionId)
                .orElseThrow(() -> new NoSuchElementException("Contribution not found"));

        // Verify workspace belongs to team
        if (!contribution.getWorkSpace().getTeam().getId().equals(teamId)) {
            throw new UnauthorizedException("Contribution does not belong to this team");
        }

        contribution.setApproved(false);
        contribution.setPending(false);
        contribution.setRejectionReason(reason);
        contribution.setApprovedBy(admin);
        contribution.setApprovedAt(LocalDateTime.now());
        contributionRepo.save(contribution);

        log.info("Contribution rejected successfully");
        return "Contribution rejected: " + reason;
    }

    /**
     * View all pending contributions for a workspace (Admin only)
     */
    @Transactional(readOnly = true)
    public List<WorkSpaceContributionResponse> viewPendingContributions(UUID adminId, Long workspaceId, UUID teamId) {
        User admin = getUserById(adminId);
        Team team = getTeamById(teamId);

        if (!isTeamAdminOrOwner(adminId, team)) {
            throw new UnauthorizedException("Only team admins can view pending contributions");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(workspaceId)
                .orElseThrow(() -> new NoSuchElementException("Workspace not found"));

        if (!docs.getTeam().getId().equals(teamId)) {
            throw new UnauthorizedException("Workspace does not belong to this team");
        }

        List<WorkSpaceContribution> contributions = contributionRepo.findByWorkSpaceIdAndPendingTrue(workspaceId);

        return contributions.stream()
                .map(this::buildContributionResponse)
                .collect(Collectors.toList());
    }

    // ==================== VIEW WORKSPACE (ALL MEMBERS) ====================

    /**
     * View workspace - All team members can view
     */
    @Transactional
    public ViewWorkSpaceDocsData viewTeamWorkspace(UUID userId, Long docId, UUID teamId) {
        User user = getUserById(userId);
        Team team = getTeamById(teamId);

        if (!isTeamMember(userId, team)) {
            throw new UnauthorizedException("Only team members can view workspace");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new NoSuchElementException("Workspace with id " + docId + " not found"));

        if (!docs.getTeam().getId().equals(teamId)) {
            throw new UnauthorizedException("Workspace does not belong to this team");
        }

        docs.setLastViewed(LocalDateTime.now());
        workSpaceRepo.save(docs);

        return docsBuilder(docs);
    }

    private ViewWorkSpaceDocsData docsBuilder(WorkSpaceDocs spaceDocs) {
        return new ViewWorkSpaceDocsData(
                spaceDocs.getTitle(),
                spaceDocs.getDescription(),
                new String(spaceDocs.getWorkSpaceData(), StandardCharsets.UTF_8)
        );
    }

    // ==================== DOWNLOAD (ALL MEMBERS) ====================

    /**
     * Download workspace file - All team members can download
     */
    @Transactional
    public ResponseEntity<byte[]> downloadTeamWorkspaceFile(Long docId, UUID userId, UUID teamId) {
        User user = getUserById(userId);
        Team team = getTeamById(teamId);

        if (!isTeamMember(userId, team)) {
            throw new UnauthorizedException("Only team members can download workspace files");
        }

        WorkSpaceDocs document = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new NoSuchElementException("Workspace not found"));

        if (!document.getTeam().getId().equals(teamId)) {
            throw new UnauthorizedException("Workspace does not belong to this team");
        }

        String mimeType = document.getDocType().getMimeType();
        String extension = document.getDocType().getExtension();
        String fileName = document.getTitle() + extension;

        document.setLastViewed(LocalDateTime.now());
        workSpaceRepo.save(document);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentLength(document.getWorkSpaceData().length)
                .body(document.getWorkSpaceData());
    }

    /**
     * Download all team workspaces as ZIP - All team members can download
     */
    @Transactional
    public ResponseEntity<byte[]> downloadAllTeamWorkspaces(UUID userId, UUID teamId) {
        User user = getUserById(userId);
        Team team = getTeamById(teamId);

        if (!isTeamMember(userId, team)) {
            throw new UnauthorizedException("Only team members can download workspace files");
        }

        List<WorkSpaceDocs> docs = workSpaceRepo.findByTeamId(teamId);

        if (docs.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(baos)) {

            for (WorkSpaceDocs doc : docs) {
                String extension = doc.getDocType().getExtension();
                String fileName = doc.getTitle() + extension;
                byte[] fileData = doc.getWorkSpaceData();

                ZipEntry zipEntry = new ZipEntry(fileName);
                zipEntry.setSize(fileData.length);
                zipOut.putNextEntry(zipEntry);
                zipOut.write(fileData);
                zipOut.closeEntry();
            }

            zipOut.finish();
            byte[] zipBytes = baos.toByteArray();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"team_" + teamId + "_workspaces.zip\"")
                    .contentLength(zipBytes.length)
                    .body(zipBytes);

        } catch (IOException e) {
            log.error("Failed to create zip for team {}", teamId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== DELETE (ADMIN ONLY) ====================

    /**
     * Delete workspace - Only admins can delete
     */
    @Transactional
    public String deleteTeamWorkspace(UUID adminId, Long docId, UUID teamId) {
        User admin = getUserById(adminId);
        Team team = getTeamById(teamId);

        if (!isTeamAdminOrOwner(adminId, team)) {
            throw new UnauthorizedException("Only team admins can delete workspaces");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new NoSuchElementException("Workspace with id " + docId + " not found"));

        if (!docs.getTeam().getId().equals(teamId)) {
            throw new UnauthorizedException("Workspace does not belong to this team");
        }

        workSpaceRepo.delete(docs);

        log.info("Team workspace with id {} deleted successfully by admin {}", docId, adminId);
        return "Team workspace deleted successfully";
    }

    // ==================== LIST WORKSPACES ====================

    /**
     * View all team workspaces - All members can view list
     */
    @Transactional(readOnly = true)
    public List<UserWorkSpaceDocs> viewAllTeamWorkspaces(UUID userId, UUID teamId) {
        User user = getUserById(userId);
        Team team = getTeamById(teamId);

        if (!isTeamMember(userId, team)) {
            throw new UnauthorizedException("Only team members can view workspaces");
        }

        List<WorkSpaceDocs> docs = workSpaceRepo.findByTeamId(teamId);

        return docs.stream()
                .map(doc -> new UserWorkSpaceDocs(
                        doc.getId(),
                        doc.getTitle(),
                        doc.getDescription(),
                        new String(doc.getWorkSpaceData(), StandardCharsets.UTF_8),
                        doc.getUser().getUsername(),
                        doc.getDocType()
                ))
                .collect(Collectors.toList());
    }

    /**
     * View workspaces by doc type
     */
    @Transactional(readOnly = true)
    public List<UserWorkSpaceDocs> viewTeamWorkspacesByType(UUID userId, UUID teamId, DocType docType) {
        User user = getUserById(userId);
        Team team = getTeamById(teamId);

        if (!isTeamMember(userId, team)) {
            throw new UnauthorizedException("Only team members can view workspaces");
        }

        List<WorkSpaceDocs> docs = workSpaceRepo.findByTeamIdAndDocType(teamId, docType);

        return docs.stream()
                .map(doc -> new UserWorkSpaceDocs(
                        doc.getId(),
                        doc.getTitle(),
                        doc.getDescription(),
                        new String(doc.getWorkSpaceData(), StandardCharsets.UTF_8),
                        doc.getUser().getUsername(),
                        doc.getDocType()
                ))
                .collect(Collectors.toList());
    }

    // ==================== HELPER METHODS FOR TEMPLATES ====================

    private String getTemplateForDocType(DocType docType) {
        return switch (docType) {
            case HTML -> """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>My Document</title>
                    </head>
                    <body>
                        <h1>Hello, World!</h1>
                    </body>
                    </html>
                    """;
            case JS -> """
                    // JavaScript Template
                    console.log("Hello, World!");
                    
                    function main() {
                        // Your code here
                    }
                    
                    main();
                    """;
            case CSS -> """
                    /* CSS Template */
                    body {
                        font-family: Arial, sans-serif;
                        margin: 0;
                        padding: 0;
                        background-color: #f4f4f4;
                    }
                    
                    h1 {
                        color: #333;
                    }
                    """;
            case JAVA -> """
                    public class Main {
                        public static void main(String[] args) {
                            System.out.println("Hello, World!");
                        }
                    }
                    """;
            case PYTHON -> """
                    # Python Template
                    def main():
                        print("Hello, World!")
                    
                    if __name__ == "__main__":
                        main()
                    """;
            case C_SHARP -> """
                    using System;
                    
                    namespace HelloWorld
                    {
                        class Program
                        {
                            static void Main(string[] args)
                            {
                                Console.WriteLine("Hello World!");
                            }
                        }
                    }
                    """;
            case C_PLUS_PLUS -> """
                    #include <iostream>
                    
                    int main() {
                        std::cout << "Hello World!";
                        return 0;
                    }
                    """;
            case MARKDOWN -> """
                    # Hello, World!
                    
                    This is a markdown template.
                    """;
            case TEXT -> "Hello, World!";
            default -> {
                log.warn("Unknown DocType: {}", docType);
                yield "";
            }
        };
    }

    private String updateContentWithinTemplate(String existingFullDocument, String newContent, DocType docType) {
        return switch (docType) {
            case HTML -> existingFullDocument.replaceAll(
                    "(?s)(<body>)(.*?)(</body>)",
                    "$1\n    " + newContent + "\n$3"
            );
            case JAVA -> existingFullDocument.replaceAll(
                    "(?s)(public static void main\\(String\\[\\] args\\) \\{)(.*?)(\\}\\s*\\})",
                    "$1\n        " + newContent + "\n    $3"
            );
            case PYTHON -> existingFullDocument.replaceAll(
                    "(?s)(def main\\(\\):)(.*?)(\\nif __name__)",
                    "$1\n    " + newContent + "\n$3"
            );
            case JS, TYPESCRIPT -> existingFullDocument.replaceAll(
                    "(?s)(function main\\(\\) \\{)(.*?)(\\})",
                    "$1\n    " + newContent + "\n$3"
            );
            case TEXT -> newContent;
            default -> {
                log.warn("Unknown DocType for content update: {}", docType);
                yield newContent;
            }
        };
    }

    private WorkSpaceContributionResponse buildContributionResponse(WorkSpaceContribution contribution) {
        String status = contribution.isPending() ? "PENDING" :
                       contribution.isApproved() ? "APPROVED" : "REJECTED";

        return WorkSpaceContributionResponse.builder()
                .contributionId(contribution.getId())
                .workspaceId(contribution.getWorkSpace().getId())
                .workspaceName(contribution.getWorkSpace().getTitle())
                .contributorName(contribution.getContributor().getUsername())
                .contributorEmail(contribution.getContributor().getEmail())
                .submittedAt(contribution.getSubmittedAt())
                .approvedAt(contribution.getApprovedAt())
                .approvedBy(contribution.getApprovedBy() != null ? 
                           contribution.getApprovedBy().getUsername() : null)
                .status(status)
                .rejectionReason(contribution.getRejectionReason())
                .build();
    }
}