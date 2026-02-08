package com.novaTech.Nova.Entities;


import com.novaTech.Nova.Entities.Enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "projects",
        indexes = {
                @Index(name = "idx_projects_user_id", columnList = "user_id"),
                @Index(name = "idx_projects_status", columnList = "status"),
                @Index(name = "idx_projects_created_at", columnList = "created_at")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== Relationship with Documents =====
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectDocument> documents = new ArrayList<>();

    // ===== Helper Methods =====

    public void addDocument(ProjectDocument document) {
        documents.add(document);
        document.setProject(this);
    }

    public void removeDocument(ProjectDocument document) {
        documents.remove(document);
        document.setProject(null);
    }

    public boolean isActive() {
        return ProjectStatus.ACTIVE.equals(this.status);
    }

    public boolean isCompleted() {
        return ProjectStatus.COMPLETED.equals(this.status);
    }

    public boolean isArchived() {
        return ProjectStatus.ARCHIVED.equals(this.status);
    }

    public long getDocumentCount() {
        return documents.stream()
                .filter(doc -> !doc.isDeleted())
                .count();
    }
}