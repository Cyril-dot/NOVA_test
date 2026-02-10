package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.chats.TeamAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamAnnouncementRepository extends JpaRepository<TeamAnnouncement, Long> {
    List<TeamAnnouncement> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    List<TeamAnnouncement> findByTeamIdAndIsPinnedTrueOrderByCreatedAtDesc(Long teamId);
}