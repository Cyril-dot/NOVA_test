package com.novaTech.Nova.DTO;

public record ChangePasswordDTO(
        String currentPassword,
        String newPassword,
        String confirmPassword
) {
}
