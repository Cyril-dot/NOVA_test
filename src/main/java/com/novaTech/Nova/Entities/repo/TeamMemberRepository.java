package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.Team;
import com.novaTech.Nova.Entities.TeamMember;
import com.novaTech.Nova.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    Optional<TeamMember> findByTeamAndUser(Team team, User user);
    List<TeamMember> findByUser(User user);
}
