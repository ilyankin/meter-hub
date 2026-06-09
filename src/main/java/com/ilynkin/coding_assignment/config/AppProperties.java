package com.ilynkin.coding_assignment.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(Bootstrap bootstrap,
                            @Valid @NotNull Report report,
                            @Valid @NotNull Auth auth) {

    public record Bootstrap(boolean enabled, Account admin, Account manager) {
        public record Account(String email, String password, String fullName) {
        }
    }

    public record Report(@NotBlank String recipient,
                         @NotBlank String from,
                         @NotBlank String subject,
                         @NotNull Duration interval,
                         @NotNull Duration initialDelay,
                         @Positive int periodDays) {
    }

    public record Auth(@NotNull Duration tokenTtl, @NotBlank String cleanupCron) {
    }
}
