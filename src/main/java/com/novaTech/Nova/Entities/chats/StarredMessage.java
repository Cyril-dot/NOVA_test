package com.novaTech.Nova.Entities.chats;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "starred_messages",
        uniqueConstraints = @UniqueConstraint(columnNames = {"messageId", "userId"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StarredMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long messageId;

    @Column(nullable = false)
    private Long userId;

    private String notes;

    @Column(nullable = false)
    private LocalDateTime starredAt;

    @PrePersist
    protected void onCreate() {
        starredAt = LocalDateTime.now();
    }
}