package com.novaTech.Nova.Entities;

import com.novaTech.Nova.Entities.Enums.TaskPriority;
import com.novaTech.Nova.Entities.Enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "team_project_tasks",
        indexes = {
                @Index(name = "idx_team_task_id", columnList = "id"),
                @Index(name = "idx_team_task_project_id", columnList = "team_project_id"),
                @Index(name = "idx_team_task_assigned_to", columnList = "assigned_to"),
                @Index(name = "idx_team_task_status", columnList = "status"),
                @Index(name = "idx_team_task_priority", columnList = "priority"),
                @Index(name = "idx_team_task_due_date", columnList = "due_date")
        }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamProjectTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_project_id", nullable = false)
    private TeamProject teamProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
    public boolean isCompleted() {
        return TaskStatus.COMPLETED.equals(this.status);
    }

    public boolean isInProgress() {
        return TaskStatus.IN_PROGRESS.equals(this.status);
    }

    public boolean isTodo() {
        return TaskStatus.TODO.equals(this.status);
    }

    public boolean isOverdue() {
        return dueDate != null && LocalDate.now().isAfter(dueDate) && !isCompleted();
    }

    public boolean isAssigned() {
        return assignedTo != null;
    }

    public void markAsCompleted() {
        this.status = TaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsDeleted() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}