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
        name = "team_projects",
        indexes = {
                @Index(name = "idx_team_project_id", columnList = "id"),
                @Index(name = "idx_team_project_title", columnList = "title"),
                @Index(name = "idx_team_project_team_id", columnList = "team_id"),
                @Index(name = "idx_team_project_status", columnList = "status"),
                @Index(name = "idx_team_project_created_at", columnList = "created_at")
        }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

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

    // ===== Relationships =====
    @OneToMany(mappedBy = "teamProject", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamProjectTask> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "teamProject", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamProjectDocument> documents = new ArrayList<>();

    // ===== Helper Methods =====
    public boolean isActive() {
        return ProjectStatus.ACTIVE.equals(this.status);
    }

    public boolean isCompleted() {
        return ProjectStatus.COMPLETED.equals(this.status);
    }

    public boolean isArchived() {
        return ProjectStatus.ARCHIVED.equals(this.status);
    }

    public long getTaskCount() {
        return tasks != null ? tasks.size() : 0;
    }

    public long getCompletedTaskCount() {
        return tasks != null ? tasks.stream()
                .filter(TeamProjectTask::isCompleted)
                .count() : 0;
    }

    public long getDocumentCount() {
        return documents != null ? documents.stream()
                .filter(doc -> !doc.isDeleted())
                .count() : 0;
    }

    public void addTask(TeamProjectTask task) {
        tasks.add(task);
        task.setTeamProject(this);
    }

    public void removeTask(TeamProjectTask task) {
        tasks.remove(task);
        task.setTeamProject(null);
    }

    public void addDocument(TeamProjectDocument document) {
        documents.add(document);
        document.setTeamProject(this);
    }

    public void removeDocument(TeamProjectDocument document) {
        documents.remove(document);
        document.setTeamProject(null);
    }
}