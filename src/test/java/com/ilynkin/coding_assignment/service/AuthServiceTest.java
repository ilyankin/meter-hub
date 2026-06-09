package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.config.AppProperties;
import com.ilynkin.coding_assignment.dto.request.LoginRequest;
import com.ilynkin.coding_assignment.entity.AuthToken;
import com.ilynkin.coding_assignment.entity.Role;
import com.ilynkin.coding_assignment.entity.User;
import com.ilynkin.coding_assignment.exception.UnauthorizedException;
import com.ilynkin.coding_assignment.repository.AuthTokenRepository;
import com.ilynkin.coding_assignment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final Duration TOKEN_TTL = Duration.ofDays(7);

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthTokenRepository authTokenRepository = mock(AuthTokenRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AppProperties appProperties = new AppProperties(
            null, null, new AppProperties.Auth(TOKEN_TTL, "0 0 3 * * *"));

    private final AuthService authService = new AuthService(
            userRepository, authTokenRepository, passwordEncoder, appProperties);

    @Test
    void login_setsTokenExpiry_fromConfiguredTtl() {
        User user = User.builder()
                .email("ivan@example.com")
                .password("{bcrypt}hash")
                .role(Role.MANAGER)
                .build();
        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(authTokenRepository.save(any(AuthToken.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.login(new LoginRequest("ivan@example.com", "password1"));

        ArgumentCaptor<AuthToken> captor = ArgumentCaptor.forClass(AuthToken.class);
        verify(authTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getExpiresAt())
                .isCloseTo(Instant.now().plus(TOKEN_TTL), within(10, ChronoUnit.SECONDS));
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        User user = User.builder()
                .email("ivan@example.com")
                .password("{bcrypt}hash")
                .role(Role.MANAGER)
                .build();
        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ivan@example.com", "wrong-pass")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
