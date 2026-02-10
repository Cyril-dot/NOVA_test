package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.meeting.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
    
    List<MeetingParticipant> findByMeetingId(Long meetingId);
    
    Optional<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, UUID userId);
    
    @Query("SELECT p FROM MeetingParticipant p WHERE " +
           "p.meetingId = :meetingId AND p.leftAt IS NULL")
    List<MeetingParticipant> findActiveParticipants(@Param("meetingId") Long meetingId);
    
    List<MeetingParticipant> findByMeetingIdAndIsInWaitingRoomTrue(Long meetingId);
    
    @Query("SELECT p FROM MeetingParticipant p WHERE " +
           "p.meetingId = :meetingId AND p.isHandRaised = true AND p.leftAt IS NULL")
    List<MeetingParticipant> findRaisedHands(@Param("meetingId") Long meetingId);
}