package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingResponseDTO {
    private UUID id;
    private String meetingCode;
    private String title;
    private String description;
    private String hostName;
    private UUID hostId;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime endTime;
    private MeetingStatus status;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Boolean isPublic;
    private Boolean requiresPassword;
    private Boolean allowGuests;
    private Boolean videoEnabled;
    private Boolean audioEnabled;
    private Boolean screenShareEnabled;
    private Boolean chatEnabled;
    private List<ParticipantDTO> participants;
    private LocalDateTime createdAt;
}

