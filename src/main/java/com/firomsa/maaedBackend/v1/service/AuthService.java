package com.firomsa.maaedBackend.v1.service;

import java.util.Random;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.firomsa.maaedBackend.exception.AuthenticationException;
import com.firomsa.maaedBackend.exception.ResourceNotFoundException;
import com.firomsa.maaedBackend.exception.UserAlreadyExistsException;
import com.firomsa.maaedBackend.model.Role;
import com.firomsa.maaedBackend.model.Roles;
import com.firomsa.maaedBackend.model.User;
import com.firomsa.maaedBackend.repository.RoleRepository;
import com.firomsa.maaedBackend.repository.UserRepository;
import com.firomsa.maaedBackend.v1.dto.RegisterRequestDTO;
import com.firomsa.maaedBackend.v1.dto.RegisterResponseDTO;
import com.firomsa.maaedBackend.v1.mapper.RegisterMapper;
import com.firomsa.maaedBackend.v1.mapper.RegisterMapperImpl;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RegisterMapper registerMapper;
    private final EmailService emailService;

    public AuthService(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            RoleRepository roleRepository,
            RegisterMapperImpl registerMapper,
            EmailService emailService) {
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.registerMapper = registerMapper;
    }

    public RegisterResponseDTO create(RegisterRequestDTO registerRequestDTO) {
        if (userRepository.findByUsername(registerRequestDTO.username()).isPresent()) {
            throw new UserAlreadyExistsException(
                    registerRequestDTO.username());
        }

        if (userRepository.findByEmail(registerRequestDTO.email()).isPresent()) {
            throw new UserAlreadyExistsException(
                    registerRequestDTO.email());
        }

        Role role = roleRepository
                .findByName(registerRequestDTO.role())
                .orElseThrow(() -> new ResourceNotFoundException("Role: " + registerRequestDTO.role().name()));
        if (role.getName().equals(Roles.ADMIN)) {
            throw new AuthenticationException("Not allowed to register an admin");
        }

        User user = registerMapper.toModel(registerRequestDTO);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(registerRequestDTO.password()));

        var otp = generateOtp();
        emailService.sendOtp(otp, user.getEmail());

        var response = registerMapper.toDTO(userRepository.save(user));

        return response;
    }

    public String generateOtp() {
        var random = new Random();
        var numbers = new StringBuffer();

        for (int i = 0; i < 5; i++) {
            numbers.append(random.nextInt(10));
        }

        return numbers.toString();
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER: " + email));
    }
}
