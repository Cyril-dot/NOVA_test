package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.TeamReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TeamReminderRepo extends JpaRepository<TeamReminder, UUID> {

    // Find active reminders for a user in a team
    List<TeamReminder> findByUserIdAndTeamIdAndIsActiveTrue(UUID userId, UUID teamId);

    // Find active reminders for a project
    List<TeamReminder> findByTeamProjectIdAndIsActiveTrue(UUID projectId);

    // Find active reminders for a task
    List<TeamReminder> findByTeamTaskIdAndIsActiveTrue(UUID taskId);

    // Find all reminders for a team
    List<TeamReminder> findByTeamIdAndIsActiveTrue(UUID teamId);

    // Find due reminders (for scheduled job)
    @Query("SELECT r FROM TeamReminder r " +
            "WHERE r.isActive = true " +
            "AND r.isSent = false " +
            "AND r.reminderDateTime BETWEEN :startTime AND :endTime")
    List<TeamReminder> findDueReminders(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // Find upcoming reminders for a user (next 7 days)
    @Query("SELECT r FROM TeamReminder r " +
            "WHERE r.user.id = :userId " +
            "AND r.isActive = true " +
            "AND r.isSent = false " +
            "AND r.reminderDateTime BETWEEN :now AND :futureDate")
    List<TeamReminder> findUpcomingReminders(
            @Param("userId") UUID userId,
            @Param("now") LocalDateTime now,
            @Param("futureDate") LocalDateTime futureDate);

    // Count active reminders for a user
    long countByUserIdAndIsActiveTrue(UUID userId);
}