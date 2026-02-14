package com.novaTech.Nova.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMeetingDTO {
    private String title;
    private String description;
    private LocalDateTime scheduledStartTime;
    private Integer maxParticipants = 50;
    private Boolean isPublic = false;
    private Boolean requiresPassword = false;
    private String password;
    private Boolean allowGuests = true;
    private Boolean videoEnabled = true;
    private Boolean audioEnabled = true;
    private Boolean screenShareEnabled = true;
    private Boolean chatEnabled = true;
}