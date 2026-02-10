package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.Enums.PresenceStatus;
import com.novaTech.Nova.Entities.models.UserPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPresenceRepository extends JpaRepository<UserPresence, Long> {
    Optional<UserPresence> findByUserId(Long userId);
    List<UserPresence> findByStatus(PresenceStatus status);
}
