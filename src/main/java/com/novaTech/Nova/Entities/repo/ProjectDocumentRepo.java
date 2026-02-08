package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.ProjectDocument;
import com.novaTech.Nova.Entities.Project;
import com.novaTech.Nova.Entities.Enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectDocumentRepo extends JpaRepository<ProjectDocument, UUID> {

    List<ProjectDocument> findByProjectAndIsDeletedFalse(Project project);

    List<ProjectDocument> findByProjectAndDocumentTypeAndIsDeletedFalse(Project project, DocumentType documentType);

    Optional<ProjectDocument> findByIdAndIsDeletedFalse(UUID id);

    @Query("SELECT d FROM ProjectDocument d WHERE d.project.id = :projectId AND d.isDeleted = false")
    List<ProjectDocument> findAllByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT d FROM ProjectDocument d WHERE d.project.id = :projectId AND d.documentType = :type AND d.isDeleted = false")
    List<ProjectDocument> findAllByProjectIdAndType(@Param("projectId") UUID projectId, @Param("type") DocumentType type);

    long countByProjectAndIsDeletedFalse(Project project);
}