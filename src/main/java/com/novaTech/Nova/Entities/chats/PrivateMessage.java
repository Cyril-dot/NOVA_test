package com.novaTech.Nova.Entities.chats;

import com.novaTech.Nova.Entities.Enums.MessageStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "private_messages", indexes = {
        @Index(name = "idx_chat_room", columnList = "chatRoomId,sentAt"),
        @Index(name = "idx_receiver_status", columnList = "receiverId,status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivateMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long chatRoomId;

    @Column(nullable = false)
    private UUID senderId;

    @Column(nullable = false)
    private UUID receiverId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    private LocalDateTime deliveredAt;

    private LocalDateTime readAt;

    private LocalDateTime editedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(nullable = false)
    private Boolean isForwarded = false;

    private Long replyToMessageId;

    private String fileUrl;

    private String fileType;

    private Long fileSizeBytes;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
        status = MessageStatus.SENT;
        isDeleted = false;
        isForwarded = false;
    }
}