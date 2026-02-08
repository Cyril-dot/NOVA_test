package com.novaTech.Nova.DTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class TeamResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private Set<String> memberUsernames;
    private LocalDateTime createdAt;
}
