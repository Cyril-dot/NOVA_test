package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.Team;
import com.novaTech.Nova.Entities.TeamMember;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.Enums.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    Optional<TeamMember> findByTeamAndUser(Team team, User user);

    List<TeamMember> findByUser(User user);

    List<TeamMember> findByTeamIdAndIsActiveTrue(UUID teamId);

    boolean existsByTeamIdAndUserIdAndIsActiveTrue(UUID teamId, UUID userId);

    // DEACTIVATE TEAM MEMBER
    @Modifying
    @Transactional
    @Query("UPDATE TeamMember tm SET tm.isActive = false WHERE tm.id = :memberId")
    int deactivateMember(@Param("memberId") UUID memberId);

    // CHANGE ROLE OF TEAM MEMBER
    @Modifying
    @Transactional
    @Query("UPDATE TeamMember tm SET tm.role = :role WHERE tm.id = :memberId")
    int updateMemberRole(@Param("memberId") UUID memberId, @Param("role") TeamStatus role);
}
