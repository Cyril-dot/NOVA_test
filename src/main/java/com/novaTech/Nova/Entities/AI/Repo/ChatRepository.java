package com.novaTech.Nova.Entities.AI.Repo;

import com.novaTech.Nova.Entities.AI.Chat;
import com.novaTech.Nova.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    // Find all active chats for a user, ordered by most recent
    List<Chat> findByUserAndIsActiveTrueOrderByUpdatedAtDesc(User user);

    // Find the most recent active chat for a user
    Optional<Chat> findFirstByUserAndIsActiveTrueOrderByUpdatedAtDesc(User user);

    // Find a specific chat by ID and user
    Optional<Chat> findByIdAndUserAndIsActiveTrue(Long id, User user);

    // Count active chats for a user
    long countByUserAndIsActiveTrue(User user);

    // Custom query to get chat with messages
    @Query("SELECT c FROM Chat c LEFT JOIN FETCH c.messages WHERE c.id = :chatId AND c.user = :user AND c.isActive = true")
    Optional<Chat> findByIdAndUserWithMessages(@Param("chatId") Long chatId,
                                               @Param("user") User user);
}
