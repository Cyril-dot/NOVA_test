package com.novaTech.Nova.Entities.AI.Repo;

import com.novaTech.Nova.Entities.AI.Chat;
import com.novaTech.Nova.Entities.AI.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Find all messages for a chat, ordered by creation time
    List<Message> findByChatOrderByCreatedAtAsc(Chat chat);

    // Find last N messages for a chat (for context window)
    List<Message> findTop10ByChatOrderByCreatedAtDesc(Chat chat);

    // Count messages in a chat
    long countByChat(Chat chat);
}