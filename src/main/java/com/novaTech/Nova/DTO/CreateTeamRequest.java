package com.novaTech.Nova.DTO;

import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class CreateTeamRequest {
    private String name;
    private String description;
    private Set<UUID> memberIds; // IDs of users to add
}
