package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.Priority;
import com.novaTech.Nova.Entities.Enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class TaskCreationDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Priority is required")
    private Priority priority;

    private LocalDate dueDate;
}
