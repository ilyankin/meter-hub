package com.ilynkin.coding_assignment.scheduling;

import com.ilynkin.coding_assignment.repository.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final AuthTokenRepository authTokenRepository;

    @Scheduled(cron = "${app.auth.cleanup-cron}")
    @Transactional
    public void purgeExpiredTokens() {
        int deleted = authTokenRepository.deleteAllByExpiresAtBefore(Instant.now());
        if (deleted > 0) {
            log.info("Удалено истёкших токенов: {}", deleted);
        }
    }
}
