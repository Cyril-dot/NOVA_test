package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.chats.StarredMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StarredMessageRepository extends JpaRepository<StarredMessage, Long> {
    List<StarredMessage> findByUserIdOrderByStarredAtDesc(Long userId);
    Optional<StarredMessage> findByMessageIdAndUserId(Long messageId, Long userId);
    boolean existsByMessageIdAndUserId(Long messageId, Long userId);
    void deleteByMessageIdAndUserId(Long messageId, Long userId);
}