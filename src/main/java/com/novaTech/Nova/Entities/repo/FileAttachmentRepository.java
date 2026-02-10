package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.models.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {
    List<FileAttachment> findByContextAndContextIdOrderByUploadedAtDesc(String context, Long contextId);
    List<FileAttachment> findByUploadedByIdOrderByUploadedAtDesc(Long uploadedById);
}