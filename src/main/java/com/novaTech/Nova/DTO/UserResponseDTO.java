package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.Role;
import com.novaTech.Nova.Entities.Enums.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponseDTO {
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private Role role;
    private Status status;
    private LocalDateTime createdAt;
    private boolean mfaEnabled;
    private String mfaSecret;
    private Integer mfaCode;
    private LocalDateTime generatedAt;
}
