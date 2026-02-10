package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.Enums.MeetingStatus;
import com.novaTech.Nova.Entities.meeting.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    Optional<Meeting> findByMeetingLink(String meetingLink);
    List<Meeting> findByHostIdOrderByCreatedAtDesc(UUID hostId);
    List<Meeting> findByStatus(MeetingStatus status);
    List<Meeting> findByTeamIdOrderByCreatedAtDesc(UUID teamId);
    List<Meeting> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}