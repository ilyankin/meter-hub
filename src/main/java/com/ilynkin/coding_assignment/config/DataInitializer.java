package com.ilynkin.coding_assignment.config;

import com.ilynkin.coding_assignment.entity.Role;
import com.ilynkin.coding_assignment.entity.User;
import com.ilynkin.coding_assignment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private String adminPassword;
    @Value("${app.admin.full-name}")
    private String adminFullName;

    @Value("${app.manager.email}")
    private String managerEmail;
    @Value("${app.manager.password}")
    private String managerPassword;
    @Value("${app.manager.full-name}")
    private String managerFullName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUser(adminEmail, adminFullName, adminPassword, Role.ADMIN);
        seedUser(managerEmail, managerFullName, managerPassword, Role.MANAGER);
    }

    private void seedUser(String email, String fullName, String rawPassword, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = User.builder()
                .email(email)
                .fullName(fullName)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build();
        userRepository.save(user);

        log.info("Создан пользователь по умолчанию: {} ({})", email, role);
    }
}
