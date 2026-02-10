package com.novaTech.Nova.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMessageResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime sentAt;
    private LocalDateTime editedAt;
    private Boolean isDeleted;
    private Long replyToMessageId;
    private String fileUrl;
    private String fileType;
    private List<Long> mentionedUserIds;
    private Boolean mentionTeam;
    private Boolean mentionAdmins;
    private Integer threadCount;
}