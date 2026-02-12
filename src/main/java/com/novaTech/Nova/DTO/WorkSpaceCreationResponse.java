package com.novaTech.Nova.DTO;

import java.time.LocalDateTime;

public record WorkSpaceCreationResponse(Long id,
                                        String title,
                                        String description,
                                        String username,
                                        String email,
                                        LocalDateTime createdAt,
                                        String message) {
}
