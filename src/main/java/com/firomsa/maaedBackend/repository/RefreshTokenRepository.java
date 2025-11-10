package com.firomsa.maaedBackend.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.firomsa.maaedBackend.model.RefreshToken;
import com.firomsa.maaedBackend.model.User;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByIdAndExpiresAtAfter(UUID id, LocalDateTime date);

    void deleteAllByUser(User user);
}
