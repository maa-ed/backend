package com.firomsa.maaedBackend.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.firomsa.maaedBackend.exception.InvalidOtpException;
import com.firomsa.maaedBackend.exception.UserAlreadyExistsException;
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
import com.firomsa.maaedBackend.v1.dto.LoginResponseDTO;
import com.firomsa.maaedBackend.v1.dto.RegisterRequestDTO;
import com.firomsa.maaedBackend.v1.dto.RegisterResponseDTO;
import com.firomsa.maaedBackend.v1.dto.ResendOtpRequestDTO;
import com.firomsa.maaedBackend.v1.dto.ResendOtpResponseDTO;
import com.firomsa.maaedBackend.v1.dto.UserResponseDTO;
import com.firomsa.maaedBackend.v1.dto.RefreshTokenRequestDTO;
import com.firomsa.maaedBackend.v1.dto.LogoutRequestDTO;
import com.firomsa.maaedBackend.v1.dto.LogoutResponseDTO;
import com.firomsa.maaedBackend.v1.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EmailService emailService;
    @Mock
    private ConfirmationOtpRepository confirmationOtpRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JWTAuthService jwtAuthService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    private Role customerRole;
    private User user;
    private RegisterRequestDTO registerRequestDTO;

    @BeforeEach
    void setup() {
        customerRole = new Role();
        customerRole.setName(Roles.CUSTOMER);

        user = User.builder()
                .username("firo")
                .email("test@example.com")
                .firstName("Firomsa")
                .lastName("Assefa")
                .password("rawpwd")
                .phone("+251900000000")
                .active(false)
                .build();

        registerRequestDTO = new RegisterRequestDTO(
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                "supersecret",
                user.getEmail(),
                Roles.CUSTOMER,
                user.getPhone());
    }

    @Test
    public void create_Success_ReturnsRegisterResponse() {
        // Arrange
        given(userRepository.findByUsername(registerRequestDTO.username())).willReturn(Optional.empty());
        given(userRepository.findByEmail(registerRequestDTO.email())).willReturn(Optional.empty());
        given(roleRepository.findByName(registerRequestDTO.role())).willReturn(Optional.of(customerRole));
        given(userMapper.toModel(registerRequestDTO)).willReturn(user);
        given(passwordEncoder.encode(registerRequestDTO.password())).willReturn("encodedpwd");
        User saved = User.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .password("encodedpwd")
                .phone(user.getPhone())
                .active(false)
                .build();
        given(userRepository.save(user)).willReturn(saved);
        given(userMapper.toDTO(saved)).willReturn(new UserResponseDTO(null,
                saved.getFirstName(), saved.getLastName(), saved.getUsername(), saved.getEmail(), saved.getPhone(),
                saved.getRole() == null ? null : saved.getRole().getName().name(), null, saved.isActive()));

        // Act
        RegisterResponseDTO response = authService.create(registerRequestDTO);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.message()).containsIgnoringCase("successfully registered");
        verify(confirmationOtpRepository, times(1)).save(ArgumentMatchers.any(ConfirmationOTP.class));
        verify(emailService, times(1)).sendOtp(ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(user.getEmail()));
    }

    @Test
    public void create_WhenUsernameExists_Throws() {
        // Arrange
        given(userRepository.findByUsername(registerRequestDTO.username())).willReturn(Optional.of(user));

        // Act / Assert
        assertThrows(UserAlreadyExistsException.class, () -> authService.create(registerRequestDTO));
    }

    @Test
    public void confirmOtp_Success_ConfirmsAndReturnsMessage() {
        // Arrange
        ConfirmationOTP otp = ConfirmationOTP.builder()
                .otp("12345")
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .confirmed(false)
                .build();

        given(confirmationOtpRepository.findByOtpAndExpiresAtAfterAndConfirmedFalse(any(String.class),
                any(LocalDateTime.class)))
                .willReturn(Optional.of(otp));
        given(userRepository.save(ArgumentMatchers.any(User.class))).willReturn(user);

        // Act
        ConfirmOtpRequestDTO req = new ConfirmOtpRequestDTO("12345");
        var resp = authService.confirmOtp(req);

        // Assert
        assertThat(resp).isNotNull();
        assertThat(resp.message()).containsIgnoringCase("Successfully confirmed OTP");
        verify(confirmationOtpRepository, times(1)).deleteAllByUser(user);
    }

    @Test
    public void confirmOtp_Invalid_Throws() {
        // Arrange
        given(confirmationOtpRepository.findByOtpAndExpiresAtAfterAndConfirmedFalse(any(String.class),
                any(LocalDateTime.class)))
                .willReturn(Optional.empty());

        // Act / Assert
        assertThrows(InvalidOtpException.class, () -> authService.confirmOtp(new ConfirmOtpRequestDTO("00000")));
    }

    @Test
    public void generateOtp_ReturnsFiveDigitString() {
        String otp = authService.generateOtp();
        assertThat(otp).isNotNull();
        assertThat(otp).hasSize(5);
        assertThat(otp).matches("\\d{5}");
    }

    @Test
    public void resendOtp_Success_SendsOtp() {
        // Arrange
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));

        // Act
        ResendOtpResponseDTO resp = authService.resendOtp(new ResendOtpRequestDTO(user.getEmail()));

        // Assert
        assertThat(resp).isNotNull();
        assertThat(resp.message()).containsIgnoringCase("Successfully resent OTP");
        verify(confirmationOtpRepository, times(1)).save(ArgumentMatchers.any(ConfirmationOTP.class));
        verify(emailService, times(1)).sendOtp(ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(user.getEmail()));
    }

    @Test
    public void login_Success_ReturnsTokens() {
        // Arrange
        user.setRole(customerRole);
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(authenticationManager
                .authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(Mockito.mock(Authentication.class));
        given(jwtAuthService.generateToken(user.getEmail())).willReturn("access-token");
        RefreshToken savedToken = new RefreshToken();
        UUID tokenId = UUID.randomUUID();
        savedToken.setId(tokenId);
        savedToken.setUser(user);
        savedToken.setExpiresAt(LocalDateTime.now().plusDays(15));
        given(refreshTokenRepository.save(ArgumentMatchers.any(RefreshToken.class))).willReturn(savedToken);

        // Act
        LoginResponseDTO loginResp = authService.login(new LoginRequestDTO("supersecret", user.getEmail()));

        // Assert
        assertThat(loginResp).isNotNull();
        assertThat(loginResp.accessToken()).isEqualTo("access-token");
        assertThat(loginResp.refreshToken()).isEqualTo(tokenId.toString());
    }

    @Test
    public void refreshAccessToken_Success_ReturnsNewAccessToken() {
        // Arrange
        UUID tokenId = UUID.randomUUID();
        user.setRole(customerRole);
        RefreshToken token = new RefreshToken();
        token.setId(tokenId);
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusDays(10));

        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(refreshTokenRepository.findByIdAndExpiresAtAfter(any(UUID.class),
                any(LocalDateTime.class)))
                .willReturn(Optional.of(token));
        given(jwtAuthService.generateToken(user.getEmail())).willReturn("new-access-token");

        // Act
        LoginResponseDTO resp = authService.refreshAccessToken(new RefreshTokenRequestDTO(tokenId, user.getEmail()));

        // Assert
        assertThat(resp).isNotNull();
        assertThat(resp.accessToken()).isEqualTo("new-access-token");
    }

    @Test
    public void logoutUser_Success_DeletesRefreshTokens() {
        // Arrange
        UUID tokenId = UUID.randomUUID();
        user.setRole(customerRole);
        RefreshToken token = new RefreshToken();
        token.setId(tokenId);
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusDays(10));

        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(refreshTokenRepository.findByIdAndExpiresAtAfter(any(UUID.class),
                any(LocalDateTime.class)))
                .willReturn(Optional.of(token));

        // Act
        LogoutResponseDTO resp = authService.logoutUser(new LogoutRequestDTO(tokenId, user.getEmail()));

        // Assert
        assertThat(resp).isNotNull();
        assertThat(resp.message()).containsIgnoringCase("Successfully logged out");
        verify(refreshTokenRepository, times(1)).deleteAllByUser(user);
    }
}
