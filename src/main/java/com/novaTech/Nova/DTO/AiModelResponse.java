package com.novaTech.Nova.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelResponse {

    private String response;
    private Double confidenceScore;
    private String modelUsed;
    private Integer tokensUsed;
}