package com.example.carservice.integration;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresIntegrationTestConfiguration.class)
public abstract class PostgresIntegrationTest {
    private static String cleanupSql =
            "TRUNCATE TABLE service_works, repair_orders, cars, mechanics, customers RESTART IDENTITY CASCADE";

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = startPostgres();

    private JdbcTemplate jdbcTemplate;
    private EntityManager entityManager;

    @Autowired
    protected PostgresIntegrationTest(JdbcTemplate jdbcTemplate, EntityManager entityManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
    }

    private static PostgreSQLContainer<?> startPostgres() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16");
        container.start();
        return container;
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute(cleanupSql);
        entityManager.clear();
    }
}
