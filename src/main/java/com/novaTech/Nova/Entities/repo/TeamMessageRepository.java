package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.chats.TeamMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import jakarta.transaction.Transactional;

@Repository
public interface TeamMessageRepository extends JpaRepository<TeamMessage, Long> {

    // Existing queries
    List<TeamMessage> findByChatRoomIdAndIsDeletedFalseOrderBySentAtAsc(Long chatRoomId);

    List<TeamMessage> findByTeamIdAndIsDeletedFalseOrderBySentAtAsc(UUID teamId);

    @Query("SELECT m FROM TeamMessage m WHERE m.teamId = :teamId " +
            "AND :userId MEMBER OF m.mentionedUserIds AND m.isDeleted = false " +
            "ORDER BY m.sentAt DESC")
    List<TeamMessage> findMentionsForUser(@Param("teamId") UUID teamId,
                                          @Param("userId") UUID userId);

    @Query("SELECT m FROM TeamMessage m WHERE m.replyToMessageId = :messageId " +
            "AND m.isDeleted = false ORDER BY m.sentAt ASC")
    List<TeamMessage> findThreadReplies(@Param("messageId") Long messageId);

    @Query("SELECT m FROM TeamMessage m WHERE m.teamId = :teamId " +
            "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "AND m.isDeleted = false ORDER BY m.sentAt DESC")
    List<TeamMessage> searchMessages(@Param("teamId") UUID teamId,
                                     @Param("searchTerm") String searchTerm);

    // ========================
    // UPDATE METHODS
    // ========================

    // Mark a message as deleted
    @Modifying
    @Transactional
    @Query("UPDATE TeamMessage m SET m.isDeleted = true WHERE m.id = :messageId")
    int markMessageAsDeleted(@Param("messageId") Long messageId);

    // Update message content
    @Modifying
    @Transactional
    @Query("UPDATE TeamMessage m SET m.content = :content, m.editedAt = CURRENT_TIMESTAMP " +
            "WHERE m.id = :messageId AND m.isDeleted = false")
    int updateMessageContent(@Param("messageId") Long messageId,
                             @Param("content") String content);

    // REMOVED: addMentionedUser method - use TeamMessageMention entity instead
    // If you need to add mentions, use the junction table approach with TeamMessageMentionRepository
}