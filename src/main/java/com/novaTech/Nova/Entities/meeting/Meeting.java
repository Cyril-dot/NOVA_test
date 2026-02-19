package com.novaTech.Nova.Entities.meeting;

import com.novaTech.Nova.Entities.Enums.MeetingStatus;
import com.novaTech.Nova.Entities.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "nova_meetings")   // ← renamed
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String meetingCode;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false)
    private LocalDateTime scheduledStartTime;

    @Column
    private LocalDateTime actualStartTime;

    @Column
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxParticipants = 50;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean requiresPassword = false;

    @Column
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowGuests = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean recordingEnabled = false;

    @Column
    private String recordingUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean videoEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean audioEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean screenShareEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean chatEnabled = true;

    // ── Daily.co fields ───────────────────────────────────────────────────────

    /**
     * The full Daily.co room URL for this meeting.
     * e.g. https://noav.daily.co/abc-def-ghi
     * Populated by MeetingService when the Daily room is created.
     * The frontend uses this URL to initialise callFrame.join({ url: roomUrl }).
     */
    @Column(name = "daily_room_url")
    private String dailyRoomUrl;

    /**
     * The Daily.co room name (URL slug portion only).
     * e.g. abc-def-ghi
     * Used server-side when generating meeting tokens or deleting the room.
     */
    @Column(name = "daily_room_name")
    private String dailyRoomName;

    // ── Participants ──────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<MeetingParticipant> participants = new HashSet<>();

    // ── Timestamps ────────────────────────────────────────────────────────────

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    public void addParticipant(MeetingParticipant participant) {
        participants.add(participant);
        participant.setMeeting(this);
    }

    public void removeParticipant(MeetingParticipant participant) {
        participants.remove(participant);
        participant.setMeeting(null);
    }

    public int getCurrentParticipantCount() {
        return (int) participants.stream()
                .filter(p -> p.getLeftAt() == null)
                .count();
    }

    public boolean isFull() {
        return getCurrentParticipantCount() >= maxParticipants;
    }

    public boolean isActive() {
        return status == MeetingStatus.ACTIVE;
    }
}