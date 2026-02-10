package com.novaTech.Nova.Entities.meeting;

import com.novaTech.Nova.Entities.Enums.MeetingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OPTIONAL: Enhanced Meeting entity with dedicated callId field
 *
 * This is recommended over storing callId in the description field.
 *
 * If you use this version, you'll need to create a migration to add the call_id column:
 *
 * ALTER TABLE meetings ADD COLUMN call_id VARCHAR(50);
 * UPDATE meetings SET call_id = description WHERE description LIKE 'call-%';
 */
@Entity
@Table(name = "meetings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_link", nullable = false, length = 500)
    private String meetingLink;

    @Column(name = "call_id", length = 50) // NEW FIELD: Store GetStream call ID
    private String callId;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "waiting_room_enabled", nullable = false)
    private Boolean waitingRoomEnabled;

    @Column(name = "recording_enabled", nullable = false)
    private Boolean recordingEnabled;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "project_id") // Keep existing field
    private UUID projectId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MeetingStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}