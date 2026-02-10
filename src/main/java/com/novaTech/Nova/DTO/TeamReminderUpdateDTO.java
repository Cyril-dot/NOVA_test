package com.novaTech.Nova.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamReminderUpdateDTO {
    private String title;
    private String message;
    private LocalDateTime reminderDateTime;
    private Boolean isRecurring;
    private String recurringInterval;
    private Boolean isActive;
}