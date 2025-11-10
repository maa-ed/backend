package com.firomsa.maaedBackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.firomsa.maaedBackend.model.RefreshToken;
import com.firomsa.maaedBackend.model.User;

@DataJpaTest
public class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

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
    public void RefreshTokenRepository_SaveAndFindByIdAndExpiresAtAfter_ReturnsToken() {
        // Arrange
        User savedUser = userRepository.save(testUser);

        RefreshToken token = RefreshToken.builder()
                .user(savedUser)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        // Act
        RefreshToken saved = refreshTokenRepository.save(token);

        var found = refreshTokenRepository.findByIdAndExpiresAtAfter(saved.getId(), LocalDateTime.now());

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    public void RefreshTokenRepository_FindByIdAndExpiresAtAfter_ReturnsEmptyWhenExpired() {
        // Arrange
        User savedUser = userRepository.save(testUser);

        RefreshToken expired = RefreshToken.builder()
                .user(savedUser)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        RefreshToken saved = refreshTokenRepository.save(expired);

        // Act
        var found = refreshTokenRepository.findByIdAndExpiresAtAfter(saved.getId(), LocalDateTime.now());

        // Assert
        assertThat(found).isNotPresent();
    }

    @Test
    public void RefreshTokenRepository_DeleteAllByUser_RemovesEntriesForUser() {
        // Arrange
        User savedUser = userRepository.save(testUser);

        RefreshToken a = RefreshToken.builder()
                .user(savedUser)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        RefreshToken b = RefreshToken.builder()
                .user(savedUser)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        refreshTokenRepository.save(a);
        refreshTokenRepository.save(b);

        // Sanity check
        var before = refreshTokenRepository.findAll();
        assertThat(before).hasSizeGreaterThanOrEqualTo(2);

        // Act
        refreshTokenRepository.deleteAllByUser(savedUser);

        // Assert
        var after = refreshTokenRepository.findAll();
        assertThat(after).noneMatch(t -> t.getUser() != null && savedUser.getId().equals(t.getUser().getId()));
    }
}
