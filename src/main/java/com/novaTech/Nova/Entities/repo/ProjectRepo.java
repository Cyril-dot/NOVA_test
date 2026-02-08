package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.Project;
import com.novaTech.Nova.Entities.Enums.ProjectStatus;
import com.novaTech.Nova.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepo extends JpaRepository<Project, UUID> {

    List<Project> findByUser(User user);

    List<Project> findByUserAndStatus(User user, ProjectStatus status);

    Optional<Project> findByIdAndUser(UUID id, User user);

    @Query("SELECT p FROM Project p WHERE p.user.id = :userId")
    List<Project> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT p FROM Project p WHERE p.user.id = :userId AND p.status = :status")
    List<Project> findAllByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") ProjectStatus status);

    @Query("""
        SELECT p FROM Project p
        WHERE p.user = :user
          AND (
                LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    List<Project> searchByUserAndKeyword(@Param("user") User user,
                                         @Param("keyword") String keyword);

    long countByUser(User user);

    long countByUserAndStatus(User user, ProjectStatus status);

    // ========================
    // NOTIFICATION METHODS
    // ========================

    // Find projects with exact end date and status not equal to specified status
    List<Project> findByEndDateAndStatusNot(LocalDate endDate, ProjectStatus status);

    // Find projects with end date before specified date and status not equal to specified status
    List<Project> findByEndDateBeforeAndStatusNot(LocalDate endDate, ProjectStatus status);

    // Find projects with end date between two dates and status not equal to specified status
    List<Project> findByEndDateBetweenAndStatusNot(LocalDate startDate, LocalDate endDate, ProjectStatus status);
}