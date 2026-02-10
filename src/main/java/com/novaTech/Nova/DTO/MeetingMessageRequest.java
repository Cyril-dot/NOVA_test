package com.novaTech.Nova.DTO;

import lombok.Data;

import java.util.UUID;

@Data
public class MeetingMessageRequest {
    private String content;
    private Boolean isPrivate;
    private UUID privateToUserId;
    private String fileUrl;
    private String fileType;
}