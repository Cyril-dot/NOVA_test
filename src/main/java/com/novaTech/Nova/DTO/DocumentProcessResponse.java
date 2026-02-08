package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.FunctionalityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessResponse {

    private boolean success;
    private String message;
    private Long documentId;
    private Long processingId;
    private FunctionalityType functionality;
    private String modelUsed;
    private Map<String, Object> data;
    private String error;
    private LocalDateTime processedAt;
}