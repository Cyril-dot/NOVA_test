package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.TeamStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TeamSummaryResponse {
    private UUID teamId;
    private String teamName;
    private String description;
    private int memberCount;
    private TeamStatus myRole; // The requesting user's role in the team
}
