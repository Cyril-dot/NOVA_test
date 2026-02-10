package com.novaTech.Nova.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamReminderResponseDTO {
    private UUID id;
    private String title;
    private String message;
    private LocalDateTime reminderDateTime;
    private boolean isRecurring;
    private String recurringInterval;
    private UUID projectId;
    private String projectName;
    private UUID taskId;
    private String taskName;
    private UUID userId;
    private String userName;
    private UUID teamId;
    private String teamName;
    private boolean isSent;
    private boolean isActive;
    private LocalDateTime createdAt;
}