package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.FunctionalityType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessRequest {

    @NotNull(message = "Functionality type is required")
    private FunctionalityType functionality;

    private String question; // Required for QUESTION_ANSWERING

    private String customPrompt; // Optional for CHAT and MULTI_FEATURE

    private Long documentId; // Optional: if reprocessing existing document
}