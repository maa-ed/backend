package com.firomsa.maaedBackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.firomsa.maaedBackend.model.Roles;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JWTSecurityFilter jwtSecurityFilter;

    private final UnAuthorizedUserAuthenticationEntryPoint unAuthorizedUserAuthenticationEntryPoint;

    public SecurityConfig(
            JWTSecurityFilter jwtSecurityFilter,
            UnAuthorizedUserAuthenticationEntryPoint unAuthorizedUserAuthenticationEntryPoint) {
        this.jwtSecurityFilter = jwtSecurityFilter;

        this.unAuthorizedUserAuthenticationEntryPoint = unAuthorizedUserAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity)
            throws Exception {
        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(request -> request
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/docs",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/api/v1/customer/**").hasRole(Roles.CUSTOMER.name())
                        .requestMatchers("/api/v1/admin/**").hasRole(Roles.ADMIN.name())
                        .requestMatchers("/api/v1/restaurant/**").hasRole(Roles.RESTAURANT.name())
                        .requestMatchers("/api/v1/delivery/**").hasRole(Roles.DELIVERY.name())
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(
                        unAuthorizedUserAuthenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        jwtSecurityFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
