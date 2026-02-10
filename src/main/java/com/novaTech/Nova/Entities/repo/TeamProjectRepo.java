package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.Enums.ProjectStatus;
import com.novaTech.Nova.Entities.TeamProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamProjectRepo extends JpaRepository<TeamProject, UUID> {

    /**
     * Fetch a project by its ID only if the user is a member of the team
     */
    @Query("""
           SELECT p 
           FROM TeamProject p
           JOIN p.team.members m
           WHERE p.id = :projectId
           AND m.user.id = :userId
           """)
    Optional<TeamProject> findByIdAndUserIsTeamMember(
            @Param("projectId") UUID projectId,
            @Param("userId") UUID userId
    );

    /**
     * Fetch all projects where a given user is a member of the team
     */
    @Query("""
           SELECT p 
           FROM TeamProject p
           JOIN p.team.members m
           WHERE m.user.id = :userId
           """)
    List<TeamProject> findAllByUserIsTeamMember(@Param("userId") UUID userId);

    // ================= NEW METHOD =================

    /**
     * Fetch all projects under a specific team
     * only if the user is a member of that team
     */
    @Query("""
           SELECT p
           FROM TeamProject p
           JOIN p.team.members m
           WHERE p.team.id = :teamId
           AND m.user.id = :userId
           """)
    List<TeamProject> findAllByTeamIdAndUserIsTeamMember(
            @Param("teamId") UUID teamId,
            @Param("userId") UUID userId
    );

    // Count projects in a team where the user is a member
    @Query("""
       SELECT COUNT(p)
       FROM TeamProject p
       JOIN p.team.members m
       WHERE p.team.id = :teamId
       AND m.user.id = :userId
       """)
    long countByTeamIdAndUserIsTeamMember(
            @Param("teamId") UUID teamId,
            @Param("userId") UUID userId
    );


    @Query("""
       SELECT COUNT(p)
       FROM TeamProject p
       JOIN p.team.members m
       WHERE p.team.id = :teamId
       AND m.user.id = :userId
       AND p.status = :status
       """)
    long countByTeamIdAndUserAndStatus(
            @Param("teamId") UUID teamId,
            @Param("userId") UUID userId,
            @Param("status") ProjectStatus status
    );

    @Query("""
       SELECT p
       FROM TeamProject p
       JOIN p.team.members m
       WHERE p.team.id = :teamId
       AND m.user.id = :userId
       AND p.endDate < CURRENT_DATE
       """)
    List<TeamProject> findOverdueProjects(
            @Param("teamId") UUID teamId,
            @Param("userId") UUID userId
    );


}
