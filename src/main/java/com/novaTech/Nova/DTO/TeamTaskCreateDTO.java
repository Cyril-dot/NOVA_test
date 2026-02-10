package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.TaskPriority;
import com.novaTech.Nova.Entities.Enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamTaskCreateDTO {
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private UUID assignedToUserId;
}