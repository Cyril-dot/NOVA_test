package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.TeamTask;
import com.novaTech.Nova.Entities.Enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TeamTaskRepo extends JpaRepository<TeamTask, UUID> {

    // Find all tasks for a specific project
    List<TeamTask> findByTeamProjectId(UUID projectId);

    // Find tasks assigned to a specific user in a team
    @Query("SELECT t FROM TeamTask t " +
            "WHERE t.assignedTo.id = :userId " +
            "AND t.teamProject.team.id = :teamId")
    List<TeamTask> findByAssignedToIdAndTeamId(
            @Param("userId") UUID userId,
            @Param("teamId") UUID teamId);

    // Find overdue tasks for a user in a team
    @Query("SELECT t FROM TeamTask t " +
            "WHERE t.teamProject.team.id = :teamId " +
            "AND (t.assignedTo.id = :userId OR t.createdBy.id = :userId) " +
            "AND t.dueDate < :currentDate " +
            "AND t.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<TeamTask> findOverdueTasks(
            @Param("teamId") UUID teamId,
            @Param("userId") UUID userId,
            @Param("currentDate") LocalDate currentDate);

    // Overloaded method for default current date
    default List<TeamTask> findOverdueTasks(UUID teamId, UUID userId) {
        return findOverdueTasks(teamId, userId, LocalDate.now());
    }

    // Find tasks by status
    @Query("SELECT t FROM TeamTask t " +
            "WHERE t.teamProject.id = :projectId " +
            "AND t.status = :status")
    List<TeamTask> findByProjectIdAndStatus(
            @Param("projectId") UUID projectId,
            @Param("status") TaskStatus status);

    // Count tasks for a project
    long countByTeamProjectId(UUID projectId);

    // Count tasks assigned to a user
    long countByAssignedToId(UUID userId);
}