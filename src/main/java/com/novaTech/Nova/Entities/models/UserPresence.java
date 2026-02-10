package com.novaTech.Nova.Entities.models;

import com.novaTech.Nova.Entities.Enums.PresenceStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_presence")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPresence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PresenceStatus status;

    private String customStatus;

    @Column(nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(nullable = false)
    private Boolean isTyping = false;

    private Long typingInChatWith; // For private chat

    private Long typingInTeam; // For team chat

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastSeenAt = LocalDateTime.now();
    }
}