package com.ilynkin.coding_assignment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Bootstrap bootstrap, Report report) {

    public record Bootstrap(boolean enabled, Account admin, Account manager) {
        public record Account(String email, String password, String fullName) {
        }
    }

    public record Report(String recipient,
                         String from,
                         String subject,
                         Duration interval,
                         Duration initialDelay,
                         int periodDays) {
    }
}