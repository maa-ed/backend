package com.firomsa.maaedBackend.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.firomsa.maaedBackend.model.ConfirmationOTP;
import com.firomsa.maaedBackend.model.User;

@Repository
public interface ConfirmationOtpRepository extends JpaRepository<ConfirmationOTP, Integer> {

    void deleteAllByUser(User user);

    Optional<ConfirmationOTP> findByOtpAndExpiresAtAfterAndConfirmedFalse(String otp, LocalDateTime date);
}
