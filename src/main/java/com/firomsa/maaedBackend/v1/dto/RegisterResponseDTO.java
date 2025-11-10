package com.firomsa.maaedBackend.v1.dto;

public record RegisterResponseDTO(
        UserResponseDTO data,
        String message) {
}
