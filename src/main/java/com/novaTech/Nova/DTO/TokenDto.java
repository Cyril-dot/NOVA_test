package com.novaTech.Nova.DTO;

import lombok.Builder;

@Builder
public record TokenDto(String accessToken,
                       String refreshToken,
                       String message) {
}
