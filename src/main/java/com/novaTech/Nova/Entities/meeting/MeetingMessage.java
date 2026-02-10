package com.novaTech.Nova.Entities.meeting;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meeting_messages", indexes = {
    @Index(name = "idx_meeting_sent", columnList = "meetingId,sentAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long meetingId;

    @Column(nullable = false)
    private UUID senderId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private Boolean isPrivate = false;

    private UUID privateToUserId;

    private String fileUrl;

    private String fileType;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
        isPrivate = false;
    }
}