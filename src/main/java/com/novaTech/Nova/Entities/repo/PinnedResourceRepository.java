package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.chats.PinnedResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PinnedResourceRepository extends JpaRepository<PinnedResource, Long> {
    List<PinnedResource> findByTeamIdOrderByPinnedAtDesc(Long teamId);
}