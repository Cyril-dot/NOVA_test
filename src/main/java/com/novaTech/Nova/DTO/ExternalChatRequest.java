package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.Model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalChatRequest {
    private Long chatId;
    private String message;
    private Model model;
    private Map<String, Object> params; // For external API parameters
}