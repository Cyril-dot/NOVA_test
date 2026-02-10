package com.novaTech.Nova.DTO;

import lombok.Data;

@Data
public class CreateAnnouncementRequest {
    private String title;
    private String content;
    private Boolean isPinned;
}
