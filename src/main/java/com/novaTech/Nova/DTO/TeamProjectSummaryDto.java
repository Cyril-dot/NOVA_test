package com.novaTech.Nova.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeamProjectSummaryDto {
    private UUID id;
    private String name;
    private String description;
    private LocalDate dueDate;
    private long daysLeft;
}
