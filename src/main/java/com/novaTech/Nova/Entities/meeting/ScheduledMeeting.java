package com.novaTech.Nova.Entities.meeting;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "scheduled_meetings", indexes = {
    @Index(name = "idx_organizer_time", columnList = "organizerId,scheduledStartTime")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledMeeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private UUID organizerId;

    @Column(nullable = false)
    private LocalDateTime scheduledStartTime;

    @Column(nullable = false)
    private LocalDateTime scheduledEndTime;

    private String timeZone;

    @ElementCollection
    @CollectionTable(name = "scheduled_meeting_invites",
                     joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "user_id")
    private List<UUID> invitedUserIds = new ArrayList<>();

    private UUID teamId;

    private UUID projectId;

    @Column(nullable = false)
    private Boolean isRecurring = false;

    private String recurrencePattern; // DAILY, WEEKLY, MONTHLY

    @Column(nullable = false)
    private Boolean reminderSent = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Long actualMeetingId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isRecurring = false;
        reminderSent = false;
    }
}