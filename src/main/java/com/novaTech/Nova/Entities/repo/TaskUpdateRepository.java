package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.chats.TaskUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskUpdateRepository extends JpaRepository<TaskUpdate, Long> {
    List<TaskUpdate> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    List<TaskUpdate> findByTaskIdOrderByCreatedAtDesc(Long taskId);
}