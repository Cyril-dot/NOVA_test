package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.Team;
import com.novaTech.Nova.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    Optional<Team> findByIdAndUser(UUID id, User user);
}
