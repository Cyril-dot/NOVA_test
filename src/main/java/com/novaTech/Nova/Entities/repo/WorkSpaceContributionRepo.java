// WorkSpaceContributionRepo.java (Repository)
package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.workSpace.WorkSpaceContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkSpaceContributionRepo extends JpaRepository<WorkSpaceContribution, Long> {
    List<WorkSpaceContribution> findByWorkSpaceIdAndPendingTrue(Long workspaceId);
    List<WorkSpaceContribution> findByContributorId(UUID contributorId);
}