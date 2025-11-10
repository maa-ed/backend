package com.firomsa.maaedBackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.firomsa.maaedBackend.model.ConfirmationOTP;
import com.firomsa.maaedBackend.model.User;

@DataJpaTest
public class ConfirmationOtpRepositoryTest {

    @Autowired
    private ConfirmationOtpRepository confirmationOtpRepository;

    @Autowired
    private UserRepository userRepository;

    private final User testUser = User.builder()
            .username("firo")
            .email("example@gmail.com")
            .firstName("Firomsa")
            .lastName("Assefa")
            .password("123")
            .phone("+251900000000")
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    @Test
    public void ConfirmationOtpRepository_SaveAndFindByOtp_ReturnsOtp() {
        // Arrange
        User savedUser = userRepository.save(testUser);

        ConfirmationOTP otp = ConfirmationOTP.builder()
                .otp("12345")
                .confirmed(false)
                .user(savedUser)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        // Act
        ConfirmationOTP saved = confirmationOtpRepository.save(otp);

        var found = confirmationOtpRepository.findByOtpAndExpiresAtAfterAndConfirmedFalse("12345", LocalDateTime.now());

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getOtp()).isEqualTo("12345");
        assertThat(found.get().getUser().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    public void ConfirmationOtpRepository_FindByOtp_ReturnsEmptyWhenExpiredOrConfirmed() {
        // Arrange
        User savedUser = userRepository.save(testUser);

        ConfirmationOTP expiredOtp = ConfirmationOTP.builder()
                .otp("expired")
                .confirmed(false)
                .user(savedUser)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        ConfirmationOTP confirmedOtp = ConfirmationOTP.builder()
                .otp("confirmed")
                .confirmed(true)
                .user(savedUser)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        confirmationOtpRepository.saveAll(List.of(expiredOtp, confirmedOtp));

        // Act
        var foundExpired = confirmationOtpRepository.findByOtpAndExpiresAtAfterAndConfirmedFalse("expired",
                LocalDateTime.now());
        var foundConfirmed = confirmationOtpRepository.findByOtpAndExpiresAtAfterAndConfirmedFalse("confirmed",
                LocalDateTime.now());

        // Assert
        assertThat(foundExpired).isNotPresent();
        assertThat(foundConfirmed).isNotPresent();
    }

    @Test
    public void ConfirmationOtpRepository_DeleteAllByUser_RemovesEntriesForUser() {
        // Arrange
        User savedUser = userRepository.save(testUser);

        ConfirmationOTP a = ConfirmationOTP.builder()
                .otp("a")
                .confirmed(false)
                .user(savedUser)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        ConfirmationOTP b = ConfirmationOTP.builder()
                .otp("b")
                .confirmed(false)
                .user(savedUser)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        confirmationOtpRepository.saveAll(List.of(a, b));

        // Sanity check
        var before = confirmationOtpRepository.findAll();
        assertThat(before).hasSizeGreaterThanOrEqualTo(2);

        // Act
        confirmationOtpRepository.deleteAllByUser(savedUser);

        // Assert
        var after = confirmationOtpRepository.findAll();
        // All entries for the savedUser should be removed
        assertThat(after).noneMatch(otp -> otp.getUser() != null && savedUser.getId().equals(otp.getUser().getId()));
    }

}
