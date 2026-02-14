package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.meeting.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, UUID> {
    
    List<MeetingParticipant> findByMeetingId(UUID meetingId);
    
    @Query("SELECT p FROM MeetingParticipant p WHERE p.meeting.id = :meetingId AND p.leftAt IS NULL")
    List<MeetingParticipant> findActiveParticipants(UUID meetingId);
    
    Optional<MeetingParticipant> findBySessionId(String sessionId);
    
    Optional<MeetingParticipant> findByPeerId(String peerId);
    
    @Query("SELECT p FROM MeetingParticipant p WHERE p.meeting.id = :meetingId AND p.user.id = :userId AND p.leftAt IS NULL")
    Optional<MeetingParticipant> findActiveUserParticipant(UUID meetingId, UUID userId);
    
    @Query("SELECT p FROM MeetingParticipant p WHERE p.meeting.meetingCode = :meetingCode AND p.isOnline = true")
    List<MeetingParticipant> findOnlineParticipantsByMeetingCode(String meetingCode);
}