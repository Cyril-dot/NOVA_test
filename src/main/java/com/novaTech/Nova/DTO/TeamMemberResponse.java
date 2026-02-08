package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.TeamStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TeamMemberResponse {
    private UUID userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private TeamStatus role;
}
