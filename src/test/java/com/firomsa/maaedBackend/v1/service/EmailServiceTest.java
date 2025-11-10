package com.firomsa.maaedBackend.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.firomsa.maaedBackend.config.AdminConfig;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private AdminConfig adminConfig;

    @InjectMocks
    private EmailService emailService;

    @Test
    public void sendOtp_SendsEmailWithCorrectFields() {
        // Arrange
        String otp = "12345";
        String to = "test@example.com";
        given(adminConfig.getEmail()).willReturn("no-reply@maaed.com");

        // Act
        emailService.sendOtp(otp, to);

        // Assert
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).isNotNull();
        assertThat(sent.getTo()).containsExactly(to);
        assertThat(sent.getFrom()).isEqualTo("no-reply@maaed.com");
        assertThat(sent.getText()).isEqualTo(otp);
    }

    @Test
    public void sendOtp_WhenMailSenderThrows_PropagatesException() {
        // Arrange
        String otp = "00000";
        String to = "fail@example.com";
        given(adminConfig.getEmail()).willReturn("no-reply@maaed.com");
        org.mockito.Mockito.doThrow(new MailException("fail") {
        }).when(javaMailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        // Act / Assert
        assertThrows(MailException.class, () -> emailService.sendOtp(otp, to));
    }
}
