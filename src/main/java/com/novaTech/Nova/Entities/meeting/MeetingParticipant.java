package com.novaTech.Nova.Entities.meeting;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meeting_participants",
       uniqueConstraints = @UniqueConstraint(columnNames = {"meetingId", "userId"}),
       indexes = {
           @Index(name = "idx_meeting_active", columnList = "meetingId,leftAt")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long meetingId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    @Column(nullable = false)
    private Boolean isMuted = false;

    @Column(nullable = false)
    private Boolean isVideoOn = true;

    @Column(nullable = false)
    private Boolean isHandRaised = false;

    @Column(nullable = false)
    private Boolean isInWaitingRoom = false;

    @Column(nullable = false)
    private Boolean isScreenSharing = false;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
        isMuted = false;
        isVideoOn = true;
        isHandRaised = false;
        isInWaitingRoom = false;
        isScreenSharing = false;
    }
}