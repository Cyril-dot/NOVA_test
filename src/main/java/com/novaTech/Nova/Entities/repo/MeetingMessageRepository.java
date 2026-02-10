package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.meeting.MeetingMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingMessageRepository extends JpaRepository<MeetingMessage, Long> {
    
    @Query("SELECT m FROM MeetingMessage m WHERE " +
           "m.meetingId = :meetingId AND m.isPrivate = false " +
           "ORDER BY m.sentAt ASC")
    List<MeetingMessage> findPublicMessages(@Param("meetingId") Long meetingId);
    
    @Query("SELECT m FROM MeetingMessage m WHERE m.meetingId = :meetingId " +
           "AND m.isPrivate = true AND " +
           "((m.senderId = :userId) OR (m.privateToUserId = :userId)) " +
           "ORDER BY m.sentAt ASC")
    List<MeetingMessage> findPrivateMessagesForUser(@Param("meetingId") Long meetingId, 
                                                    @Param("userId") UUID userId);
    
    List<MeetingMessage> findByMeetingIdOrderBySentAtAsc(Long meetingId);
}