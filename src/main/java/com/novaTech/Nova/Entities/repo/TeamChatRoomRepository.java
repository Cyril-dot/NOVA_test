package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.chats.TeamChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;

@Repository
public interface TeamChatRoomRepository extends JpaRepository<TeamChatRoom, Long> {

    Optional<TeamChatRoom> findByTeamIdAndIsActiveTrue(UUID teamId);

    // UPDATE LAST MESSAGE TIMESTAMP
    @Modifying
    @Transactional
    @Query("UPDATE TeamChatRoom t SET t.lastMessageAt = CURRENT_TIMESTAMP WHERE t.teamId = :teamId")
    int updateLastMessageTime(@Param("teamId") UUID teamId);

    // DEACTIVATE CHAT ROOM
    @Modifying
    @Transactional
    @Query("UPDATE TeamChatRoom t SET t.isActive = false WHERE t.teamId = :teamId")
    int deactivateChatRoom(@Param("teamId") UUID teamId);
}
