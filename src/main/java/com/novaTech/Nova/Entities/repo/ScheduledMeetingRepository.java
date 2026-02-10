package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.meeting.ScheduledMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledMeetingRepository extends JpaRepository<ScheduledMeeting, Long> {
    
    List<ScheduledMeeting> findByOrganizerIdOrderByScheduledStartTimeDesc(UUID organizerId);
    
    List<ScheduledMeeting> findByTeamIdOrderByScheduledStartTimeDesc(UUID teamId);
    
    @Query("SELECT sm FROM ScheduledMeeting sm WHERE :userId MEMBER OF sm.invitedUserIds " +
           "ORDER BY sm.scheduledStartTime DESC")
    List<ScheduledMeeting> findByInvitedUser(@Param("userId") UUID userId);
    
    @Query("SELECT sm FROM ScheduledMeeting sm WHERE " +
           "sm.scheduledStartTime BETWEEN :startDate AND :endDate " +
           "ORDER BY sm.scheduledStartTime ASC")
    List<ScheduledMeeting> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT sm FROM ScheduledMeeting sm WHERE " +
           "sm.scheduledStartTime <= :now AND sm.reminderSent = false")
    List<ScheduledMeeting> findUpcomingMeetingsNeedingReminders(@Param("now") LocalDateTime now);
}