package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private String content;
    private MessageStatus status;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime editedAt;
    private Boolean isDeleted;
    private Long replyToMessageId;
    private String fileUrl;
    private String fileType;
    private Integer reactionCount;
}