package com.firomsa.maaedBackend.v1.dto;

import com.firomsa.maaedBackend.model.Roles;

public record LoginResponseDTO(
        Roles role,
        String accessToken,
        String refreshToken,
        String username,
        String email) {

}
