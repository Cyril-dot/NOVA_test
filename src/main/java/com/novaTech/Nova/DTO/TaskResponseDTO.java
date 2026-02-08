package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.Priority;
import com.novaTech.Nova.Entities.Enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TaskResponseDTO {
    private UUID id;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private LocalDate dueDate;
    private UUID userId;
    private String userName; // Optional: helpful for UI
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
