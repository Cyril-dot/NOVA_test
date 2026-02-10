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
public class TeamReminderCreateDTO {
    private String title;
    private String message;
    private LocalDateTime reminderDateTime;
    private boolean isRecurring;
    private String recurringInterval; // DAILY, WEEKLY, MONTHLY
    private UUID projectId;
    private UUID taskId;
}