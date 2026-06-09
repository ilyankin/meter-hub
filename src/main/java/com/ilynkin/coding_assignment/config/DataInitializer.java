package com.ilynkin.coding_assignment.config;

import com.ilynkin.coding_assignment.config.AppProperties.Bootstrap.Account;
import com.ilynkin.coding_assignment.entity.Role;
import com.ilynkin.coding_assignment.entity.User;
import com.ilynkin.coding_assignment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.bootstrap", name = "enabled", havingValue = "true")
public class DataInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUser(appProperties.bootstrap().admin(), Role.ADMIN);
        seedUser(appProperties.bootstrap().manager(), Role.MANAGER);
    }

    private void seedUser(Account account, Role role) {
        if (account == null || !StringUtils.hasText(account.email())) return;
        if (!StringUtils.hasText(account.password()) || !StringUtils.hasText(account.fullName())) {
            throw new IllegalStateException(
                    "app.bootstrap.enabled=true, но для роли " + role
                            + " password/full-name not set (set via environment variables)");
        }
        if (userRepository.existsByEmail(account.email())) return;

        User user = User.builder()
                .email(account.email())
                .fullName(account.fullName())
                .password(passwordEncoder.encode(account.password()))
                .role(role)
                .build();
        userRepository.save(user);

        log.info("Создан пользователь по умолчанию: {} ({})", account.email(), role);
    }
}
