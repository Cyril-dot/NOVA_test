package com.novaTech.Nova.Entities.meeting;

import com.novaTech.Nova.Entities.Enums.ParticipantRole;
import com.novaTech.Nova.Entities.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nova_meeting_participants")   // ← renamed
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    // For registered users
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // For guests (non-registered users)
    @Column
    private String guestName;

    @Column
    private String guestEmail;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isGuest = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role; // HOST, MODERATOR, PARTICIPANT

    // ── WebRTC / Daily session info ───────────────────────────────────────────
    // sessionId and peerId are still useful for presence tracking via your
    // WebSocket handler even though Daily handles the actual video/audio.

    @Column(unique = true)
    private String sessionId; // WebSocket session ID (server-side presence)

    @Column(unique = true)
    private String peerId;    // WebRTC peer ID (Daily participant identifier)

    // ── Participant media status ───────────────────────────────────────────────
    // These mirror what Daily reports so the dashboard can show accurate icons
    // without having to call the Daily API directly.

    @Column(nullable = false)
    @Builder.Default
    private Boolean videoEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean audioEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean screenSharing = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isOnline = false;

    // ── Connection info ───────────────────────────────────────────────────────

    @Column
    private String ipAddress;

    @Column
    private String userAgent;

    // ── Timestamps ────────────────────────────────────────────────────────────

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @Column
    private LocalDateTime leftAt;

    @Column
    private Long durationSeconds; // Total seconds the participant was in the meeting

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    public String getDisplayName() {
        if (isGuest) {
            return guestName != null ? guestName : "Guest";
        }
        return user != null
                ? user.getFirstName() + " " + user.getLastName()
                : "Unknown";
    }

    public void leave() {
        leftAt    = LocalDateTime.now();
        isOnline  = false;
        if (joinedAt != null) {
            durationSeconds = java.time.Duration.between(joinedAt, leftAt).getSeconds();
        }
    }

    public boolean isHost() {
        return role == ParticipantRole.HOST;
    }

    public boolean isModerator() {
        return role == ParticipantRole.MODERATOR || role == ParticipantRole.HOST;
    }
}