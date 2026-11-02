package com.lavyoung.gateweave.center.release.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.lavyoung.gateweave.center.GateWeaveCenterApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurationReleaseMigrationTests {

    private static final String INSERT_RELEASE = """
            INSERT INTO gw_config_release
                (gateway_group_id, config_version, checksum, snapshot_content, release_status, published_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final Timestamp PUBLISHED_AT = Timestamp.from(Instant.parse("2026-11-02T10:15:30Z"));

    @Test
    void shouldCreateConfigurationReleaseTableAndQueryIndex() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner("gateweave-center-release-schema");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            List<String> tableNames = jdbcTemplate.queryForList("""
                            SELECT TABLE_NAME
                            FROM INFORMATION_SCHEMA.TABLES
                            WHERE TABLE_SCHEMA = 'PUBLIC'
                            """, String.class)
                    .stream()
                    .map(String::toLowerCase)
                    .toList();
            List<String> indexNames = jdbcTemplate.queryForList("""
                            SELECT INDEX_NAME
                            FROM INFORMATION_SCHEMA.INDEXES
                            WHERE TABLE_SCHEMA = 'PUBLIC'
                              AND TABLE_NAME = 'GW_CONFIG_RELEASE'
                            """, String.class)
                    .stream()
                    .map(String::toLowerCase)
                    .toList();

            assertThat(tableNames).contains("gw_config_release");
            assertThat(indexNames).contains("idx_gw_config_release_group_status_version");
        });
    }

    @Test
    void shouldPersistVersionedReleasesAndFindLatestPublishedVersion() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner("gateweave-center-release-version");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            insertRelease(jdbcTemplate, 1, "sha256:first", "{\"routes\":[]}");
            insertRelease(jdbcTemplate, 2, "sha256:second", "{\"routes\":[{\"routeId\":\"route-1\"}]}");

            Long latestVersion = jdbcTemplate.queryForObject("""
                    SELECT config_version
                    FROM gw_config_release
                    WHERE gateway_group_id = ?
                      AND release_status = ?
                    ORDER BY config_version DESC
                    LIMIT 1
                    """, Long.class, "default", "PUBLISHED");

            assertThat(latestVersion).isEqualTo(2L);
        });
    }

    @Test
    void shouldEnforceReleaseVersionAndGatewayGroupConstraints() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner("gateweave-center-release-constraints");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            insertRelease(jdbcTemplate, 1, "sha256:first", "{\"routes\":[]}");

            assertThatThrownBy(() -> insertRelease(jdbcTemplate, 1, "sha256:duplicate", "{\"routes\":[]}"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbcTemplate.update(
                    INSERT_RELEASE, "default", 0L, "sha256:invalid", "{\"routes\":[]}", "PUBLISHED", PUBLISHED_AT))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbcTemplate.update(
                    INSERT_RELEASE, "missing", 1L, "sha256:missing", "{\"routes\":[]}", "PUBLISHED", PUBLISHED_AT))
                    .isInstanceOf(DataIntegrityViolationException.class);
        });
    }

    private static void insertRelease(JdbcTemplate jdbcTemplate, long version, String checksum,
            String snapshotContent) {
        jdbcTemplate.update(
                INSERT_RELEASE, "default", version, checksum, snapshotContent, "PUBLISHED", PUBLISHED_AT);
    }

    private static WebApplicationContextRunner contextRunner(String databaseName) {
        return new WebApplicationContextRunner()
                .withUserConfiguration(GateWeaveCenterApplication.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:%s;MODE=MySQL;DB_CLOSE_DELAY=0".formatted(databaseName),
                        "spring.datasource.username=sa",
                        "spring.datasource.password=");
    }
}
