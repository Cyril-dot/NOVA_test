package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.PresenceStatus;
import lombok.Data;

@Data
public class UpdatePresenceRequest {
    private PresenceStatus status;
    private String customStatus;
}
