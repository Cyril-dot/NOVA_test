package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.chats.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
    List<MessageReaction> findByMessageId(Long messageId);
    Optional<MessageReaction> findByMessageIdAndUserId(Long messageId, Long userId);
    void deleteByMessageIdAndUserId(Long messageId, Long userId);
}