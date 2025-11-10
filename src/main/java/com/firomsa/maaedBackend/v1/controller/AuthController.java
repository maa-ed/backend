package com.firomsa.maaedBackend.v1.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.firomsa.maaedBackend.v1.dto.RegisterRequestDTO;
import com.firomsa.maaedBackend.v1.dto.RegisterResponseDTO;
import com.firomsa.maaedBackend.v1.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "API for performing authentication")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "For registering a user")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.OK)
    public RegisterResponseDTO registerUser(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        var response = authService.create(registerRequestDTO);
        return response;
    }
}
