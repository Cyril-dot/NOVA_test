package com.novaTech.Nova.DTO;


import com.novaTech.Nova.Entities.Enums.ProjectStatus;

import java.time.LocalDate;

public record ProjectCreationDto(
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        ProjectStatus status,
        String documentDescription
) {}