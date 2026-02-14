package com.novaTech.Nova.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDTO {
    private UUID id;
    private String displayName;
    private String email;
    private Boolean isGuest;
    private String role;
    private Boolean videoEnabled;
    private Boolean audioEnabled;
    private Boolean screenSharing;
    private Boolean isOnline;
    private LocalDateTime joinedAt;
}
