package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.config.AppProperties;
import com.ilynkin.coding_assignment.dto.request.LoginRequest;
import com.ilynkin.coding_assignment.dto.response.AuthResponse;
import com.ilynkin.coding_assignment.entity.AuthToken;
import com.ilynkin.coding_assignment.entity.User;
import com.ilynkin.coding_assignment.exception.UnauthorizedException;
import com.ilynkin.coding_assignment.repository.AuthTokenRepository;
import com.ilynkin.coding_assignment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return createToken(user);
    }

    @Transactional
    public void logout(String token) {
        authTokenRepository.findByToken(token).ifPresent(authTokenRepository::delete);
    }

    private AuthResponse createToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        AuthToken authToken = AuthToken.builder()
                .user(user)
                .token(tokenValue)
                .expiresAt(Instant.now().plus(appProperties.auth().tokenTtl()))
                .build();
        authToken = authTokenRepository.save(authToken);

        return new AuthResponse(authToken.getToken(), user.getId(), user.getEmail(), user.getRole());
    }
}
