package com.firomsa.maaedBackend.v1.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmOtpRequestDTO(
                @NotBlank String otp) {
}
