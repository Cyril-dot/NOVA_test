package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.TaskPriority;
import com.novaTech.Nova.Entities.Enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamTaskResponseDTO {
    private UUID id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private UUID projectId;
    private String projectName;
    private UUID createdById;
    private String createdByName;
    private UUID assignedToId;
    private String assignedToName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}