package com.novaTech.Nova.DTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class TaskSummaryDTO {
    private UUID id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private long daysLeft;
}
