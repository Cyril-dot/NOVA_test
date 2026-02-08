package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.ProjectStatus;
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
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponseDTO {
    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private String ownerEmail;
    private String ownerName;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long documentCount;
    private List<ProjectDocumentResponseDTO> documents;
}