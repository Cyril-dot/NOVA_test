package com.novaTech.Nova.Entities.chats;

import com.novaTech.Nova.Entities.Enums.FriendshipStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "friendships", indexes = {
    @Index(name = "idx_requester_status", columnList = "requesterId,status"),
    @Index(name = "idx_addressee_status", columnList = "addresseeId,status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID requesterId;

    @Column(nullable = false)
    private UUID addresseeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = FriendshipStatus.PENDING;
    }
}