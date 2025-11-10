package com.firomsa.maaedBackend.v1.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firomsa.maaedBackend.config.JpaAuditingConfig;
import com.firomsa.maaedBackend.v1.dto.ConfirmOtpRequestDTO;
import com.firomsa.maaedBackend.v1.dto.ConfirmOtpResponseDTO;
import com.firomsa.maaedBackend.v1.dto.LoginRequestDTO;
import com.firomsa.maaedBackend.v1.dto.LoginResponseDTO;
import com.firomsa.maaedBackend.v1.dto.LogoutRequestDTO;
import com.firomsa.maaedBackend.v1.dto.LogoutResponseDTO;
import com.firomsa.maaedBackend.v1.dto.RefreshTokenRequestDTO;
import com.firomsa.maaedBackend.v1.dto.RegisterRequestDTO;
import com.firomsa.maaedBackend.v1.dto.RegisterResponseDTO;
import com.firomsa.maaedBackend.v1.dto.ResendOtpRequestDTO;
import com.firomsa.maaedBackend.v1.dto.ResendOtpResponseDTO;
import com.firomsa.maaedBackend.v1.dto.UserResponseDTO;
import com.firomsa.maaedBackend.v1.service.AuthService;
import com.firomsa.maaedBackend.v1.service.JWTAuthService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = JpaAuditingConfig.class)
public class AuthControllerTest {

    @MockitoBean
    private JWTAuthService jwtAuthService;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequestDTO registerRequest;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setup() {
        registerRequest = new RegisterRequestDTO(
                "First",
                "Last",
                "firo",
                "supersecret",
                "test@example.com",
                com.firomsa.maaedBackend.model.Roles.CUSTOMER,
                "+251900000000");

        userResponseDTO = new UserResponseDTO(
                UUID.randomUUID(),
                "First",
                "Last",
                "firo",
                "test@example.com",
                "+251900000000",
                "CUSTOMER",
                null,
                false);
    }

    @Test
    public void registerEndpoint_ReturnsRegisterResponse() throws Exception {
        // Arrange
        RegisterResponseDTO resp = new RegisterResponseDTO(userResponseDTO, "You have successfully registered");
        given(authService.create(any(RegisterRequestDTO.class))).willReturn(resp);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", CoreMatchers.containsString("successfully registered")))
                .andExpect(jsonPath("$.data.username", CoreMatchers.is("firo")));

        verify(authService, times(1)).create(any(RegisterRequestDTO.class));
    }

    @Test
    public void confirmOtpEndpoint_ReturnsMessage() throws Exception {
        // Arrange
        ConfirmOtpRequestDTO req = new ConfirmOtpRequestDTO("12345");
        ConfirmOtpResponseDTO resp = new ConfirmOtpResponseDTO("Successfully confirmed OTP, please login");
        given(authService.confirmOtp(any(ConfirmOtpRequestDTO.class))).willReturn(resp);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/confirm-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", CoreMatchers.containsString("Successfully confirmed OTP")));

        verify(authService, times(1)).confirmOtp(any(ConfirmOtpRequestDTO.class));
    }

    @Test
    public void resendOtpEndpoint_ReturnsMessage() throws Exception {
        ResendOtpRequestDTO req = new ResendOtpRequestDTO("test@example.com");
        ResendOtpResponseDTO resp = new ResendOtpResponseDTO("Successfully resent OTP, check your inbox");
        given(authService.resendOtp(any(ResendOtpRequestDTO.class))).willReturn(resp);

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", CoreMatchers.containsString("Successfully resent OTP")));

        verify(authService, times(1)).resendOtp(any(ResendOtpRequestDTO.class));
    }

    @Test
    public void loginEndpoint_ReturnsTokens() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO("supersecret", "test@example.com");
        LoginResponseDTO resp = new LoginResponseDTO(com.firomsa.maaedBackend.model.Roles.CUSTOMER, "access-token",
                "refresh-id", "firo", "test@example.com");
        given(authService.login(any(LoginRequestDTO.class))).willReturn(resp);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", CoreMatchers.is("access-token")))
                .andExpect(jsonPath("$.username", CoreMatchers.is("firo")));

        verify(authService, times(1)).login(any(LoginRequestDTO.class));
    }

    @Test
    public void refreshEndpoint_ReturnsNewAccessToken() throws Exception {
        UUID tokenId = UUID.randomUUID();
        RefreshTokenRequestDTO req = new RefreshTokenRequestDTO(tokenId, "test@example.com");
        LoginResponseDTO resp = new LoginResponseDTO(com.firomsa.maaedBackend.model.Roles.CUSTOMER, "new-access",
                tokenId.toString(), "firo", "test@example.com");
        given(authService.refreshAccessToken(any(RefreshTokenRequestDTO.class))).willReturn(resp);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", CoreMatchers.is("new-access")));

        verify(authService, times(1)).refreshAccessToken(any(RefreshTokenRequestDTO.class));
    }

    @Test
    public void logoutEndpoint_ReturnsMessage() throws Exception {
        UUID tokenId = UUID.randomUUID();
        LogoutRequestDTO req = new LogoutRequestDTO(tokenId, "test@example.com");
        LogoutResponseDTO resp = new LogoutResponseDTO("Successfully logged out");
        given(authService.logoutUser(any(LogoutRequestDTO.class))).willReturn(resp);

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", CoreMatchers.containsString("Successfully logged out")));

        verify(authService, times(1)).logoutUser(any(LogoutRequestDTO.class));
    }
}
