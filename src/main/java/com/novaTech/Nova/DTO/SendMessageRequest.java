package com.novaTech.Nova.DTO;

import lombok.Data;

@Data
public class SendMessageRequest {
    private String content;
    private Long replyToMessageId;
    private String fileUrl;
    private String fileType;
    private Long fileSizeBytes;
}