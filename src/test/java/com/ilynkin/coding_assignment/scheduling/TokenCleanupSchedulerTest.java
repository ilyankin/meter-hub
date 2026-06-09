package com.ilynkin.coding_assignment.scheduling;

import com.ilynkin.coding_assignment.entity.AuthToken;
import com.ilynkin.coding_assignment.entity.User;
import com.ilynkin.coding_assignment.repository.AuthTokenRepository;
import com.ilynkin.coding_assignment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TokenCleanupSchedulerTest {

    @Autowired
    private TokenCleanupScheduler scheduler;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void purge_deletesExpiredTokens_keepsValidOnes() {
        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();
        // Истёкший токен нельзя создать через сущность: @CreationTimestamp ставит created_at = now,
        // а CHECK (expires_at > created_at) не пропустит прошлую дату — вставляем напрямую
        UUID expiredId = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO auth_tokens (id, user_id, token, expires_at, created_at)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                expiredId, admin.getId(), "expired-" + System.nanoTime(),
                Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)),
                Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS)));
        AuthToken valid = authTokenRepository.save(AuthToken.builder()
                .user(admin)
                .token("valid-" + System.nanoTime())
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build());

        scheduler.purgeExpiredTokens();

        assertThat(authTokenRepository.findById(expiredId)).isEmpty();
        assertThat(authTokenRepository.findById(valid.getId())).isPresent();
    }
}
