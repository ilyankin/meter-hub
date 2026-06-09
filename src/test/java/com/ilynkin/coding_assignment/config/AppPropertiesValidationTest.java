package com.ilynkin.coding_assignment.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AppPropertiesValidationTest {

    @Configuration
    @EnableConfigurationProperties(AppProperties.class)
    static class PropsConfig {
    }

    private ApplicationContextRunner runnerWithValidProps() {
        return new ApplicationContextRunner()
                .withUserConfiguration(PropsConfig.class)
                .withPropertyValues(
                        "app.report.recipient=user@example.com",
                        "app.report.from=noreply@meterhub.local",
                        "app.report.subject=Показания",
                        "app.report.interval=14d",
                        "app.report.initial-delay=14d",
                        "app.report.period-days=14",
                        "app.auth.token-ttl=30d",
                        "app.auth.cleanup-cron=0 0 3 * * *");
    }

    @Test
    void validProperties_contextStarts() {
        runnerWithValidProps().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(AppProperties.class).auth().tokenTtl()).hasDays(30);
        });
    }

    @Test
    void blankReportRecipient_failsStartup() {
        runnerWithValidProps()
                .withPropertyValues("app.report.recipient=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void missingTokenTtl_failsStartup() {
        runnerWithValidProps()
                .withPropertyValues("app.auth.token-ttl=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void nonPositiveReportPeriod_failsStartup() {
        runnerWithValidProps()
                .withPropertyValues("app.report.period-days=0")
                .run(context -> assertThat(context).hasFailed());
    }
}
