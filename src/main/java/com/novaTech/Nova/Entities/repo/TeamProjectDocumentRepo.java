package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.TeamProject;
import com.novaTech.Nova.Entities.TeamProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TeamProjectDocumentRepo extends JpaRepository<TeamProjectDocument, UUID> {

    /**
     * Find all non-deleted documents for a specific project
     */
    @Query("SELECT d FROM TeamProjectDocument d WHERE d.teamProject = :project AND d.isDeleted = false")
    List<TeamProjectDocument> findByProjectAndIsDeletedFalse(@Param("project") TeamProject project);

    /**
     * Count non-deleted documents for a specific project
     */
    @Query("SELECT COUNT(d) FROM TeamProjectDocument d WHERE d.teamProject = :project AND d.isDeleted = false")
    long countByProjectAndIsDeletedFalse(@Param("project") TeamProject project);

    /**
     * Find all documents for a specific project (including deleted)
     */
    List<TeamProjectDocument> findByTeamProject(TeamProject project);

    /**
     * Find all non-deleted documents for a specific project by project ID
     */
    @Query("SELECT d FROM TeamProjectDocument d WHERE d.teamProject.id = :projectId AND d.isDeleted = false")
    List<TeamProjectDocument> findByProjectIdAndIsDeletedFalse(@Param("projectId") UUID projectId);

    /**
     * Find a specific document by ID if not deleted
     */
    @Query("SELECT d FROM TeamProjectDocument d WHERE d.id = :id AND d.isDeleted = false")
    TeamProjectDocument findByIdAndIsDeletedFalse(@Param("id") UUID id);

    /**
     * Find all documents uploaded by a specific user
     */
    @Query("SELECT d FROM TeamProjectDocument d WHERE d.uploadedBy.id = :userId AND d.isDeleted = false")
    List<TeamProjectDocument> findByUploadedByIdAndIsDeletedFalse(@Param("userId") UUID userId);

    // ================= NEW METHODS =================

    /**
     * Get all documents for a specific project if the user is a team member
     */
    @Query("""
           SELECT d
           FROM TeamProjectDocument d
           JOIN d.teamProject p
           JOIN p.team.members m
           WHERE p.id = :projectId
           AND m.user.id = :userId
           AND d.isDeleted = false
           """)
    List<TeamProjectDocument> findAllByProjectAndUserIsTeamMember(
            @Param("projectId") UUID projectId,
            @Param("userId") UUID userId
    );

    /**
     * Get all documents across all projects where the user is a team member
     */
    @Query("""
           SELECT d
           FROM TeamProjectDocument d
           JOIN d.teamProject p
           JOIN p.team.members m
           WHERE m.user.id = :userId
           AND d.isDeleted = false
           """)
    List<TeamProjectDocument> findAllByUserIsTeamMember(
            @Param("userId") UUID userId
    );
}
