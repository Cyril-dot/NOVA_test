package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.meeting.MeetingRecording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRecordingRepository extends JpaRepository<MeetingRecording, Long> {
    List<MeetingRecording> findByMeetingIdOrderByCreatedAtDesc(Long meetingId);
    Optional<MeetingRecording> findFirstByMeetingIdOrderByCreatedAtDesc(Long meetingId);
}