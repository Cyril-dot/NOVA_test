package com.novaTech.Nova.DTO;

import lombok.Data;
import java.util.List;

@Data
public class TeamMessageRequest {
    private String content;
    private Long replyToMessageId;
    private String fileUrl;
    private String fileType;
    private List<Long> mentionedUserIds;
    private Boolean mentionTeam;
    private Boolean mentionAdmins;
}