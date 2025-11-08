package com.firomsa.maaedBackend.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.firomsa.maaedBackend.v1.dto.ErrorResponseDTO;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        log.warn("Validation error: {}", errors);
        var response = new ErrorResponseDTO(400, "error occured when validating fields", LocalDateTime.now(), errors);
        return response;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        var response = new ErrorResponseDTO(404, "Resource not found, " + ex.getMessage(), LocalDateTime.now(), null);
        return response;
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        var response = new ErrorResponseDTO(400, "User already exists, " + ex.getMessage(), LocalDateTime.now(), null);
        return response;
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleAuthenticationException(AuthenticationException ex) {
        var response = new ErrorResponseDTO(400, "Authentication error occured, " + ex.getMessage(),
                LocalDateTime.now(), null);
        return response;
    }

    @ExceptionHandler(MailException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponseDTO handleMailException(MailException ex) {
        log.warn("Error when sending mail, " + ex.getMessage());
        var response = new ErrorResponseDTO(503, "Error when sending mail",
                LocalDateTime.now(), null);
        return response;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDTO handleGenericException(Exception ex) {
        log.warn("An unexpected error occurred: {}", ex.getMessage(), ex);
        var response = new ErrorResponseDTO(500, "An unexpected error occurred on the server.", LocalDateTime.now(),
                null);
        return response;
    }
}
