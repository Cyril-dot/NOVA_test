package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.Enums.MeetingStatus;
import com.novaTech.Nova.Entities.meeting.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {
    
    Optional<Meeting> findByMeetingCode(String meetingCode);
    
    List<Meeting> findByHostId(UUID hostId);
    
    List<Meeting> findByStatus(MeetingStatus status);
    
    @Query("SELECT m FROM Meeting m WHERE m.host.id = :userId AND m.status = :status")
    List<Meeting> findByHostIdAndStatus(UUID userId, MeetingStatus status);
    
    @Query("SELECT m FROM Meeting m WHERE m.scheduledStartTime BETWEEN :start AND :end")
    List<Meeting> findMeetingsInTimeRange(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT m FROM Meeting m WHERE m.status = 'ACTIVE' AND m.host.id = :userId")
    Optional<Meeting> findActiveHostedMeeting(UUID userId);
    
    @Query("SELECT COUNT(p) FROM MeetingParticipant p WHERE p.meeting.id = :meetingId AND p.leftAt IS NULL")
    Integer countActiveParticipants(UUID meetingId);
}