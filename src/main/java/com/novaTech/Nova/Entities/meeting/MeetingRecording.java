package com.novaTech.Nova.Entities.meeting;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_recordings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingRecording {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long meetingId;

    @Column(nullable = false)
    private String recordingUrl;

    private String transcriptUrl;

    private Long durationSeconds;

    private Long fileSizeBytes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}