package com.firomsa.maaedBackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.firomsa.maaedBackend.model.User;

@DataJpaTest
public class UserRepositoryTest {

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
    public void UserRepository_Save_ReturnsSavedUser() {
        // Act
        User savedUser = userRepository.save(testUser);

        // Assert
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();

        // Use recursive comparison for a deep check of all fields
        assertThat(savedUser).usingRecursiveComparison().ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(testUser);
    }

    @Test
    public void UserRepository_FindByEmail_ReturnsUser() {
        // Arrange
        User saved = userRepository.save(testUser);

        // Act
        var found = userRepository.findByEmail(saved.getEmail());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    public void UserRepository_FindByUsername_ReturnsUser() {
        // Arrange
        User saved = userRepository.save(testUser);

        // Act
        var found = userRepository.findByUsername(saved.getUsername());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }
}
