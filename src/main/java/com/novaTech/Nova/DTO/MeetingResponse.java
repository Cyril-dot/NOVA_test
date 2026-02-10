package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingResponse {
    private Long id;
    private String meetingLink;
    private Long hostId;
    private String hostName;
    private String title;
    private MeetingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer participantCount;
    private Boolean waitingRoomEnabled;
    private Boolean recordingEnabled;
}