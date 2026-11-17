package com.example.carservice.integration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
class IntegrationTestConfiguration {
    private static final String POSTGRES_IMAGE = "postgres:18";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-10-29T12:00:00Z");

    @Bean
    @Primary
    Clock integrationTestClock() {
        return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
