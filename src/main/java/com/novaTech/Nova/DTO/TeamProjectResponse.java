package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.ProjectStatus;
import com.novaTech.Nova.Entities.Enums.TeamStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamProjectResponse {
    private UUID id;
    private String name;
    private String description;
    private String ownerName;
    private TeamStatus role;
    private String teamName;
    private UUID teamId;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long documentCount;
    private List<TeamProjectDocumentResponseDTO> documents;
}
