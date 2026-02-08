package com.novaTech.Nova.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {

    private boolean success;
    private String message;
    private Long documentId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer extractedTextLength;
    private LocalDateTime uploadedAt;
    private String error;
}