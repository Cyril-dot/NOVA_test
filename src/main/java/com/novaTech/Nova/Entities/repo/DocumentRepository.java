package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.Document;
import com.novaTech.Nova.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // User-specific queries
    List<Document> findByUserOrderByUploadedAtDesc(User user);

    List<Document> findByUserAndStatusOrderByUploadedAtDesc(User user, String status);

    Optional<Document> findByIdAndUser(Long id, User user);

    Long countByUser(User user);

    Long countByUserAndStatus(User user, String status);

    // Admin queries (all users)
    List<Document> findByStatusOrderByUploadedAtDesc(String status);

    List<Document> findByUploadedAtBetween(LocalDateTime start, LocalDateTime end);

    Optional<Document> findByFileName(String fileName);

    List<Document> findAllByOrderByUploadedAtDesc();
}