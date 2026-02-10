package com.novaTech.Nova.Entities.chats;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "team_messages", indexes = {
        @Index(name = "idx_team_sent", columnList = "team_id, sent_at"),
        @Index(name = "idx_team_chat_room", columnList = "chat_room_id, sent_at") // Changed from idx_chat_room
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID teamId;

    @Column(nullable = false)
    private Long chatRoomId;

    @Column(nullable = false)
    private UUID senderId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    private LocalDateTime editedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    private Long replyToMessageId; // For threaded conversations

    private String fileUrl;

    private String fileType;

    private Long fileSizeBytes;

    @ElementCollection
    @CollectionTable(name = "team_message_mentions",
            joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "user_id")
    private List<Long> mentionedUserIds = new ArrayList<>();

    @Column(nullable = false)
    private Boolean mentionTeam = false;

    @Column(nullable = false)
    private Boolean mentionAdmins = false;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
        isDeleted = false;
        mentionTeam = false;
        mentionAdmins = false;
    }
}