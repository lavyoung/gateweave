package com.lavyoung.gateweave.center.gateway.persistence;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.lavyoung.gateweave.center.GateWeaveCenterApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayClusterMigrationTests {

    private static final String INSERT_NODE = """
            INSERT INTO gw_gateway_node
                (node_id, gateway_group_id, node_name, advertised_address, status, current_config_version)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    @Test
    void shouldCreateGatewayClusterTablesIndexesAndDefaultGroup() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner();

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
                            """, String.class)
                    .stream()
                    .map(String::toLowerCase)
                    .toList();

            assertThat(tableNames).contains("gw_gateway_group", "gw_gateway_node");
            assertThat(indexNames).contains(
                    "idx_gw_gateway_group_environment_status",
                    "idx_gw_gateway_node_group_status",
                    "idx_gw_gateway_node_status_heartbeat");
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT group_name
                    FROM gw_gateway_group
                    WHERE gateway_group_id = 'default'
                    """, String.class)).isEqualTo("Default Gateway Group");
        });
    }

    @Test
    void shouldPersistGatewayNodeAndEnforceClusterConstraints() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner();

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            jdbcTemplate.update(INSERT_NODE, "gateway-node-1", "default", "Gateway Node 1",
                    "http://127.0.0.1:7397", "ONLINE", 1L);

            assertThat(jdbcTemplate.queryForObject("""
                    SELECT current_config_version
                    FROM gw_gateway_node
                    WHERE node_id = 'gateway-node-1'
                    """, Long.class)).isEqualTo(1L);
            assertThatThrownBy(() -> jdbcTemplate.update(INSERT_NODE, "gateway-node-1", "default",
                    "Duplicated Node", "http://127.0.0.1:7398", "ONLINE", 1L))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbcTemplate.update(INSERT_NODE, "gateway-node-2", "default",
                    "Duplicated Address", "http://127.0.0.1:7397", "ONLINE", 1L))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbcTemplate.update(INSERT_NODE, "gateway-node-3", "missing-group",
                    "Missing Group", "http://127.0.0.1:7399", "ONLINE", 1L))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbcTemplate.update(INSERT_NODE, "gateway-node-4", "default",
                    "Invalid Version", "http://127.0.0.1:7400", "ONLINE", -1L))
                    .isInstanceOf(DataIntegrityViolationException.class);
        });
    }

    private static WebApplicationContextRunner contextRunner() {
        return new WebApplicationContextRunner()
                .withUserConfiguration(GateWeaveCenterApplication.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:gateweave-center-gateway;MODE=MySQL;DB_CLOSE_DELAY=0",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=");
    }
}
