package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDocumentResponseDTO {
    private UUID id;
    private String fileName;
    private String originalFileName;
    private DocumentType documentType;
    private Long fileSize;
    private String mimeType;
    private String description;
    private UUID projectId;
    private String projectName;
    private UUID uploadedById;
    private String uploadedByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}