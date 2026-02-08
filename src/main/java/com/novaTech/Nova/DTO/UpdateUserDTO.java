package com.novaTech.Nova.DTO;

import org.springframework.web.multipart.MultipartFile;

public record UpdateUserDTO(
        String firstName,
        String lastName,
        String username,
        String email,
        MultipartFile profileImage
) {
}
