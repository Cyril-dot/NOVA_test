package com.novaTech.Nova.Entities;

import com.novaTech.Nova.Entities.Enums.DocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "team_project_documents",
        indexes = {
                @Index(name = "idx_team_doc_id", columnList = "id"),
                @Index(name = "idx_team_doc_project_id", columnList = "team_project_id"),
                @Index(name = "idx_team_doc_type", columnList = "document_type"),
                @Index(name = "idx_team_doc_uploaded_by", columnList = "uploaded_by"),
                @Index(name = "idx_team_doc_created_at", columnList = "created_at")
        }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamProjectDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    /**
     * File content stored as Base64 encoded string
     * Supports: PDF, DOCX, PPTX, XLSX, images
     * Max size: 10MB per file before encoding
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_content", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String fileContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType;

    @Column(name = "file_size") // Size in bytes (before Base64 encoding)
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_project_id", nullable = false)
    private TeamProject teamProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ===== Helper Methods =====
    public boolean hasContent() {
        return fileContent != null && !fileContent.isEmpty();
    }

    public long getBase64Size() {
        return hasContent() ? fileContent.length() : 0;
    }

    public void markAsDeleted() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public String getFileExtension() {
        if (originalFileName != null && originalFileName.contains(".")) {
            return originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return "";
    }
}