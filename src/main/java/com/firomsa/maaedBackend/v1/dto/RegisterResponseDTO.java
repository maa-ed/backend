package com.firomsa.maaedBackend.v1.dto;

import java.util.UUID;

public record RegisterResponseDTO(UUID id,
        String firstName,
        String lastName,
        String username,
        String email,
        String phone,
        String role,
        String createdAt,
        boolean active) {
}
