package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.Enums.TaskStatus;
import com.novaTech.Nova.Entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaskRepo extends JpaRepository<Task, UUID> {

    List<Task> findByUserId(UUID userId);
    List<Task> findByProjectId(UUID projectId);

    // to get task by id for user
    Task findByIdAndUserId(UUID taskId, UUID userId);
    Task findByIdAndProjectId(UUID taskId, UUID projectId);

    // find by user and status (FIXED)
    List<Task> findByUser_IdAndStatus(UUID userId, TaskStatus status);
    List<Task> findByProject_IdAndStatus(UUID projectId, TaskStatus status);

    @Query("""
        SELECT t FROM Task t
        WHERE t.user.id = :userId
        AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    List<Task> searchByUser(
            @Param("userId") UUID userId,
            @Param("keyword") String keyword
    );

    @Query("""
        SELECT t FROM Task t
        WHERE t.project.id = :projectId
        AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    List<Task> searchByProject(
            @Param("projectId") UUID projectId,
            @Param("keyword") String keyword
    );

    long countByUserId(UUID userId);
    long countByUserIdAndStatus(UUID userId, TaskStatus status);

    // ========================
    // NOTIFICATION METHODS
    // ========================

    // Find tasks with exact due date and status not equal to specified status
    List<Task> findByDueDateAndStatusNot(LocalDate dueDate, TaskStatus status);

    // Find tasks with due date before specified date and status not equal to specified status
    List<Task> findByDueDateBeforeAndStatusNot(LocalDate dueDate, TaskStatus status);

    // Find tasks with due date between two dates and status not equal to specified status
    List<Task> findByDueDateBetweenAndStatusNot(LocalDate startDate, LocalDate endDate, TaskStatus status);
}