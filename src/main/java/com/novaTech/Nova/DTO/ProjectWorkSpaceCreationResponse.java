package com.novaTech.Nova.DTO;

import java.time.LocalDateTime;

public record ProjectWorkSpaceCreationResponse(Long id,
                                               String title,
                                               String description,
                                               String message,
                                               String projectTitle,
                                               String projectDescription,
                                               LocalDateTime createdAt) {
}
