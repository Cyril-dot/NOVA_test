package com.novaTech.Nova.DTO;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ScheduleMeetingRequest {
    private String title;
    private String description;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private String timeZone;
    private List<UUID> invitedUserIds;
    private UUID teamId;
    private UUID projectId;
    private Boolean isRecurring;
    private String recurrencePattern;
}