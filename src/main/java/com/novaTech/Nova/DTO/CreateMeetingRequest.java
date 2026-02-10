package com.novaTech.Nova.DTO;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateMeetingRequest {
    private String title;
    private String description;
    private Boolean waitingRoomEnabled;
    private Boolean recordingEnabled;
    private UUID teamId;
    private UUID projectId;
}