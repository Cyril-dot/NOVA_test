package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.FunctionalityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingHistoryResponse {

    private Long id;
    private Long documentId;
    private String documentName;
    private FunctionalityType functionalityType;
    private String inputQuestion;
    private String inputPrompt;
    private String aiResponse;
    private String modelUsed;
    private LocalDateTime processedAt;
    private Double confidenceScore;
    private Boolean success;
    private String errorMessage;
}