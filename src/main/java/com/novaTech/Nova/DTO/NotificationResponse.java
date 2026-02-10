package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String content;
    private Long relatedEntityId;
    private Long fromUserId;
    private String fromUserName;
    private Boolean isRead;
    private LocalDateTime createdAt;
}