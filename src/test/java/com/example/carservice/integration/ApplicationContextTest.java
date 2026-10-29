package com.example.carservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

class ApplicationContextTest extends PostgresIntegrationTest {
    private static String migrationQuery =
            "SELECT version, success FROM flyway_schema_history WHERE version = ?";
    private static String initialSchemaVersion = "1";
    private JdbcTemplate jdbcTemplate;

    @Autowired
    ApplicationContextTest(JdbcTemplate jdbcTemplate, jakarta.persistence.EntityManager entityManager) {
        super(jdbcTemplate, entityManager);
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    @DisplayName("Loads the application context and applies Flyway V1")
    void loadsContextAndAppliesInitialMigration() {
        // Given
        Map<String, Object> migration = jdbcTemplate.queryForMap(migrationQuery, initialSchemaVersion);

        // When
        String version = (String) migration.get("version");
        boolean success = (boolean) migration.get("success");

        // Then
        assertEquals(initialSchemaVersion, version);
        assertTrue(success);
    }
}
