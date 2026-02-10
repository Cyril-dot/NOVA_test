package com.novaTech.Nova.DTO;

import lombok.Data;

@Data
public class PinResourceRequest {
    private String title;
    private String description;
    private String url;
    private String fileUrl;
}
