package com.firomsa.maaedBackend.v1.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firomsa.maaedBackend.model.ConfirmationOTP;
import com.firomsa.maaedBackend.model.RefreshToken;
import com.firomsa.maaedBackend.model.Role;
import com.firomsa.maaedBackend.model.Roles;
import com.firomsa.maaedBackend.model.User;
import com.firomsa.maaedBackend.repository.ConfirmationOtpRepository;
import com.firomsa.maaedBackend.repository.RefreshTokenRepository;
import com.firomsa.maaedBackend.repository.RoleRepository;
import com.firomsa.maaedBackend.repository.UserRepository;
import com.firomsa.maaedBackend.v1.dto.ConfirmOtpRequestDTO;
import com.firomsa.maaedBackend.v1.dto.LoginRequestDTO;
import com.firomsa.maaedBackend.v1.dto.RefreshTokenRequestDTO;
import com.firomsa.maaedBackend.v1.dto.LogoutRequestDTO;
import com.firomsa.maaedBackend.v1.dto.ResendOtpRequestDTO;
import com.firomsa.maaedBackend.v1.dto.RegisterRequestDTO;
import com.firomsa.maaedBackend.v1.service.EmailService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class AuthControllerIntTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ConfirmationOtpRepository confirmationOtpRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    @Test
    public void registerEndpoint_Integration_SavesUserAndSendsOtp() throws Exception {

        RegisterRequestDTO req = new RegisterRequestDTO(
                "First",
                "Last",
                "firo_int",
                "supersecret",
                "int@example.com",
                Roles.CUSTOMER,
                "+251900000000");

        // Act
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", CoreMatchers.containsString("registered")))
                .andExpect(jsonPath("$.data.email", is(req.email())));

        // Assert: user saved
        var saved = userRepository.findByEmail(req.email());
        org.assertj.core.api.Assertions.assertThat(saved).isPresent();
        org.assertj.core.api.Assertions.assertThat(saved.get().getUsername()).isEqualTo(req.username());

        // Assert: OTP saved
        List<ConfirmationOTP> otps = confirmationOtpRepository.findAll();
        org.assertj.core.api.Assertions.assertThat(otps).isNotEmpty();
    }

    @Test
    public void confirmOtpEndpoint_Integration_ConfirmsUser() throws Exception {
        Role role = roleRepository.findByName(Roles.CUSTOMER).get();

        User user = User.builder()
                .username("firo_confirm")
                .email("confirm@example.com")
                .firstName("F")
                .lastName("L")
                .password("encoded")
                .phone("+251900000001")
                .active(false)
                .role(role)
                .build();
        userRepository.save(user);

        ConfirmationOTP otp = ConfirmationOTP.builder()
                .otp("54321")
                .user(user)
                .expiresAt(java.time.LocalDateTime.now().plusMinutes(10))
                .confirmed(false)
                .build();
        confirmationOtpRepository.save(otp);

        ConfirmOtpRequestDTO req = new ConfirmOtpRequestDTO("54321");

        // Act
        mockMvc.perform(post("/api/v1/auth/confirm-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", CoreMatchers.containsString("Successfully confirmed OTP")));

        // Assert: user active and otp removed/confirmed
        var updated = userRepository.findByEmail(user.getEmail()).get();
        org.assertj.core.api.Assertions.assertThat(updated.isActive()).isTrue();
    }

    @Test
    public void resendOtpEndpoint_Integration_CreatesOtp() throws Exception {
        // Arrange: ensure a user exists
        Role role = roleRepository.findByName(Roles.CUSTOMER).get();
        User user = User.builder()
                .username("firo_resend")
                .email("resend@example.com")
                .firstName("F")
                .lastName("L")
                .password(passwordEncoder.encode("supersecret"))
                .phone("+251900000010")
                .active(true)
                .role(role)
                .build();
        userRepository.save(user);

        int before = confirmationOtpRepository.findAll().size();

        ResendOtpRequestDTO req = new ResendOtpRequestDTO(user.getEmail());

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/resend-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", CoreMatchers.containsString("Successfully resent OTP")));

        int after = confirmationOtpRepository.findAll().size();
        org.assertj.core.api.Assertions.assertThat(after).isGreaterThan(before);
    }

    @Test
    public void loginEndpoint_Integration_ReturnsTokens() throws Exception {
        // Arrange: user exists with encoded password and active
        Role role = roleRepository.findByName(Roles.CUSTOMER).get();
        User user = User.builder()
                .username("firo_login")
                .email("login@example.com")
                .firstName("F")
                .lastName("L")
                .password(passwordEncoder.encode("supersecret"))
                .phone("+251900000011")
                .active(true)
                .role(role)
                .build();
        userRepository.save(user);

        LoginRequestDTO req = new LoginRequestDTO("supersecret", user.getEmail());

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.username", CoreMatchers.is(user.getUsername())))
                .andExpect(jsonPath("$.email", CoreMatchers.is(user.getEmail())));

        // Ensure a refresh token was created
        var tokens = refreshTokenRepository.findAll();
        org.assertj.core.api.Assertions
                .assertThat(tokens.stream().anyMatch(t -> t.getUser().getEmail().equals(user.getEmail()))).isTrue();
    }

    @Test
    public void refreshEndpoint_Integration_ReturnsNewAccessToken() throws Exception {
        // Arrange: user with a valid refresh token
        Role role = roleRepository.findByName(Roles.CUSTOMER).get();
        User user = User.builder()
                .username("firo_refresh")
                .email("refresh@example.com")
                .firstName("F")
                .lastName("L")
                .password(passwordEncoder.encode("supersecret"))
                .phone("+251900000012")
                .active(true)
                .role(role)
                .build();
        userRepository.save(user);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setExpiresAt(java.time.LocalDateTime.now().plusDays(10));
        rt = refreshTokenRepository.save(rt);

        RefreshTokenRequestDTO req = new RefreshTokenRequestDTO(rt.getId(), user.getEmail());

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken", CoreMatchers.is(rt.getId().toString())));
    }

    @Test
    public void logoutEndpoint_Integration_ClearsRefreshTokens() throws Exception {
        // Arrange: user with a valid refresh token
        Role role = roleRepository.findByName(Roles.CUSTOMER).get();
        User user = User.builder()
                .username("firo_logout")
                .email("logout@example.com")
                .firstName("F")
                .lastName("L")
                .password(passwordEncoder.encode("supersecret"))
                .phone("+251900000013")
                .active(true)
                .role(role)
                .build();
        userRepository.save(user);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setExpiresAt(java.time.LocalDateTime.now().plusDays(10));
        rt = refreshTokenRepository.save(rt);

        LogoutRequestDTO req = new LogoutRequestDTO(rt.getId(), user.getEmail());

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", CoreMatchers.containsString("Successfully logged out")));

        // Assert: all refresh tokens for the user are deleted
        var remaining = refreshTokenRepository.findAll();
        org.assertj.core.api.Assertions
                .assertThat(remaining.stream().noneMatch(t -> t.getUser().getEmail().equals(user.getEmail()))).isTrue();
    }

}
