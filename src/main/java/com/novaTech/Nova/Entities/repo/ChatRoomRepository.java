package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.chats.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    
    @Query("SELECT cr FROM ChatRoom cr WHERE " +
           "(cr.user1Id = :user1 AND cr.user2Id = :user2) OR " +
           "(cr.user1Id = :user2 AND cr.user2Id = :user1)")
    Optional<ChatRoom> findByUsers(@Param("user1") UUID user1, @Param("user2") UUID user2);
    
    @Query("SELECT cr FROM ChatRoom cr WHERE " +
           "cr.user1Id = :userId OR cr.user2Id = :userId " +
           "ORDER BY cr.lastMessageAt DESC NULLS LAST")
    List<ChatRoom> findByUserIdOrderByLastMessageDesc(@Param("userId") UUID userId);
}