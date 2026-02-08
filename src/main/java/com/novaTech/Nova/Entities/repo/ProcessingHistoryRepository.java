package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.Document;
import com.novaTech.Nova.Entities.ProcessingHistory;
import com.novaTech.Nova.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProcessingHistoryRepository extends JpaRepository<ProcessingHistory, Long> {

    // User-specific queries (via document relationship)
    @Query("SELECT ph FROM ProcessingHistory ph WHERE ph.document.user = :user ORDER BY ph.processedAt DESC")
    List<ProcessingHistory> findByUserOrderByProcessedAtDesc(@Param("user") User user);

    @Query("SELECT ph FROM ProcessingHistory ph WHERE ph.document.user = :user AND ph.functionalityType = :type ORDER BY ph.processedAt DESC")
    List<ProcessingHistory> findByUserAndFunctionalityTypeOrderByProcessedAtDesc(@Param("user") User user, @Param("type") String functionalityType);

    @Query("SELECT ph FROM ProcessingHistory ph WHERE ph.document.user = :user AND ph.success = :success ORDER BY ph.processedAt DESC")
    List<ProcessingHistory> findByUserAndSuccessOrderByProcessedAtDesc(@Param("user") User user, @Param("success") Boolean success);

    // Document-specific queries
    List<ProcessingHistory> findByDocumentOrderByProcessedAtDesc(Document document);

    Long countByDocumentAndSuccess(Document document, Boolean success);

    // Admin queries (all users)
    List<ProcessingHistory> findByFunctionalityTypeOrderByProcessedAtDesc(String functionalityType);

    List<ProcessingHistory> findBySuccessOrderByProcessedAtDesc(Boolean success);

    List<ProcessingHistory> findByProcessedAtBetween(LocalDateTime start, LocalDateTime end);

    List<ProcessingHistory> findAllByOrderByProcessedAtDesc();
}