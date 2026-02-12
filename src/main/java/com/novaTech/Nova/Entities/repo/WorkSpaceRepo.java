package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.Enums.DocType;
import com.novaTech.Nova.Entities.ProjectDocument;
import com.novaTech.Nova.Entities.workSpace.WorkSpaceDocs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WorkSpaceRepo extends JpaRepository<WorkSpaceDocs, Long> {

    // Find all docs by user
    List<WorkSpaceDocs> findByUserId(UUID userId);

    // Find all docs by team
    List<WorkSpaceDocs> findByTeamId(UUID teamId);

    // Find all docs by project
    List<WorkSpaceDocs> findByProjectId(UUID projectId);

    // View workspaces by docType for a specific user
    List<WorkSpaceDocs> findByUserIdAndDocType(UUID userId, DocType docType);

    // View workspaces by docType for a specific team
    List<WorkSpaceDocs> findByTeamIdAndDocType(UUID teamId, DocType docType);

    // View workspaces by docType for a specific project
    List<WorkSpaceDocs> findByProjectIdAndDocType(UUID projectId, DocType docType);

    // General search by title or description for a user
    @Query("""
            SELECT w FROM WorkSpaceDocs w
            WHERE w.user.id = :userId
            AND (LOWER(w.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(w.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    List<WorkSpaceDocs> searchByUserAndKeyword(@Param("userId") UUID userId,
                                               @Param("keyword") String keyword);

    // General search by title or description for a team
    @Query("""
            SELECT w FROM WorkSpaceDocs w
            WHERE w.team.id = :teamId
            AND (LOWER(w.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(w.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    List<WorkSpaceDocs> searchByTeamAndKeyword(@Param("teamId") UUID teamId,
                                               @Param("keyword") String keyword);

    // General search by title or description for a project
    @Query("""
            SELECT w FROM WorkSpaceDocs w
            WHERE w.project.id = :projectId
            AND (LOWER(w.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(w.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    List<WorkSpaceDocs> searchByProjectAndKeyword(@Param("projectId") UUID projectId,
                                                  @Param("keyword") String keyword);



    // Top 10 most recent docs by user
    List<WorkSpaceDocs> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);

    // Top 10 most recent docs by team
    List<WorkSpaceDocs> findTop10ByTeamIdOrderByCreatedAtDesc(UUID teamId);

    // Top 10 most recent docs by project
    List<WorkSpaceDocs> findTop10ByProjectIdOrderByCreatedAtDesc(UUID projectId);

    // Top 10 most recently VIEWED docs by user
    List<WorkSpaceDocs> findTop10ByUserIdOrderByLastViewedDesc(UUID userId);


    List<WorkSpaceDocs> findTopByProjectDocumentOrderByCreatedAtDesc(
            ProjectDocument projectDocument
    );

    // -----------------------------
    // LAST VIEWED PROJECT DOCUMENT
    // -----------------------------

    List<WorkSpaceDocs> findTopByProjectDocumentOrderByLastViewedDesc(
            ProjectDocument projectDocument
    );

    @Query("""
    SELECT w FROM WorkSpaceDocs w
    WHERE w.projectDocument.id = :projectDocumentId
    AND (
        LOWER(w.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(w.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
""")
    List<WorkSpaceDocs> searchByProjectDocument(
            UUID projectDocumentId,
            String keyword
    );

    List<WorkSpaceDocs> findByProjectDocumentIdAndDocType(
            UUID projectDocumentId,
            DocType docType
    );


}
