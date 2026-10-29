package com.example.carservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.util.stream.Stream;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

class ApplicationContextTest extends PostgresIntegrationTest {
    private static String failedMigrationCountQuery =
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false";
    private static String latestMigrationVersionQuery =
            "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1";
    private static String migrationResourcePattern = "classpath*:db/migration/V*__*.sql";
    private static String migrationVersionPrefix = "V";
    private static String migrationNameDelimiter = "__";
    private static String noMigrationFilesMessage = "No Flyway migration files were found.";
    private static int expectedFailedMigrationCount = 0;
    private JdbcTemplate jdbcTemplate;
    private ResourcePatternResolver resourcePatternResolver =
            new PathMatchingResourcePatternResolver();

    @Autowired
    ApplicationContextTest(JdbcTemplate jdbcTemplate, EntityManager entityManager) {
        super(jdbcTemplate, entityManager);
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    @DisplayName("Loads the application context and applies all Flyway migrations")
    void loadsContextAndAppliesAllMigrations() throws IOException {
        // Given
        Resource[] migrationFiles = resourcePatternResolver.getResources(migrationResourcePattern);
        MigrationVersion highestMigrationVersion = Stream.of(migrationFiles)
                .map(Resource::getFilename)
                .map(filename -> filename.substring(
                        migrationVersionPrefix.length(),
                        filename.indexOf(migrationNameDelimiter)))
                .map(MigrationVersion::fromVersion)
                .max(MigrationVersion::compareTo)
                .orElseThrow(() -> new IllegalStateException(noMigrationFilesMessage));
        Integer failedMigrationCount =
                jdbcTemplate.queryForObject(failedMigrationCountQuery, Integer.class);

        // When
        String latestMigrationVersion =
                jdbcTemplate.queryForObject(latestMigrationVersionQuery, String.class);

        // Then
        assertEquals(expectedFailedMigrationCount, failedMigrationCount);
        assertEquals(highestMigrationVersion.toString(), latestMigrationVersion);
    }
}
