package com.novaTech.Nova.Entities.chats;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"messageId", "userId"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long messageId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String emoji;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}