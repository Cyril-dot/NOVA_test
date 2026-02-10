package com.novaTech.Nova.Entities.chats;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_updates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teamId;

    private Long taskId; // Reference to task in your project management system

    @Column(nullable = false)
    private Long updatedById;

    @Column(nullable = false)
    private String updateType; // CREATED, COMPLETED, ASSIGNED, UPDATED

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}