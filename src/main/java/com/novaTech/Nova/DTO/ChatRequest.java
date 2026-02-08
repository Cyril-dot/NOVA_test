package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.Model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String message;

    private Model model;

    private Long chatId;
    @Builder.Default
    private Boolean stream = true; // Optional: default false
}