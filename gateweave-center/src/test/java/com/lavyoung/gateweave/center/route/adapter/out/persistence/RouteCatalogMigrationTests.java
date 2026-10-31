package com.lavyoung.gateweave.center.route.adapter.out.persistence;

import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.lavyoung.gateweave.center.GateWeaveCenterApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteCatalogMigrationTests {

    private static final String INSERT_SERVICE = """
            INSERT INTO gw_service
                (service_id, application_name, protocol, namespace, service_version, status)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String INSERT_OPERATION = """
            INSERT INTO gw_service_operation
                (operation_id, service_id, interface_name, operation_name, status)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String INSERT_OPERATION_PARAMETER = """
            INSERT INTO gw_service_operation_parameter
                (operation_id, parameter_index, parameter_name, type_name)
            VALUES (?, ?, ?, ?)
            """;
    private static final String INSERT_ROUTE = """
            INSERT INTO gw_route
                (route_id, gateway_group_id, path, http_method, service_operation_id,
                 authentication_mode, invocation_timeout_ms, target_attributes, route_attributes, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String INSERT_ROUTE_PARAMETER = """
            INSERT INTO gw_route_parameter
                (route_id, service_operation_id, parameter_index, parameter_name)
            VALUES (?, ?, ?, ?)
            """;

    @Test
    void shouldCreateRouteCatalogTablesAtVersionOne() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner();

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            Flyway flyway = context.getBean(Flyway.class);
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            List<String> tableNames = jdbcTemplate.queryForList("""
                            SELECT TABLE_NAME
                            FROM INFORMATION_SCHEMA.TABLES
                            WHERE TABLE_SCHEMA = 'PUBLIC'
                            """, String.class)
                    .stream()
                    .map(String::toLowerCase)
                    .toList();

            assertThat(flyway.info().current().getVersion().toString()).isEqualTo("1");
            assertThat(tableNames).contains("gw_route", "gw_route_parameter");
        });
    }

    @Test
    void shouldPersistRouteParametersAndEnforceTargetRelationships() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner();

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            insertServiceCatalog(jdbcTemplate);

            jdbcTemplate.update(INSERT_ROUTE, "route-say-hi", "default", "/sayHi", "GET", "activity-say-hi",
                    "REQUIRED", 3000L, "{\"version\":\"1.0.0\"}", "{}", "ENABLED");
            jdbcTemplate.update(INSERT_ROUTE_PARAMETER, "route-say-hi", "activity-say-hi", 0, "name");
            jdbcTemplate.update(INSERT_ROUTE_PARAMETER, "route-say-hi", "activity-say-hi", 1, "age");

            List<Map<String, Object>> parameters = jdbcTemplate.queryForList("""
                    SELECT rp.parameter_index, rp.parameter_name, op.type_name
                    FROM gw_route_parameter rp
                    JOIN gw_service_operation_parameter op
                      ON op.operation_id = rp.service_operation_id
                     AND op.parameter_index = rp.parameter_index
                    WHERE rp.route_id = ?
                    ORDER BY rp.parameter_index
                    """, "route-say-hi");
            assertThat(parameters).extracting(row -> row.get("PARAMETER_NAME")).containsExactly("name", "age");
            assertThat(parameters).extracting(row -> row.get("TYPE_NAME"))
                    .containsExactly("java.lang.String", "java.lang.Integer");

            assertThatThrownBy(() -> jdbcTemplate.update(
                    INSERT_ROUTE, "route-duplicate", "default", "/sayHi", "GET", "activity-say-hi",
                    "ANONYMOUS", 3000L, "{}", "{}", "ENABLED"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbcTemplate.update(
                    INSERT_ROUTE, "route-invalid-timeout", "default", "/invalid", "GET", "activity-say-hi",
                    "ANONYMOUS", 0L, "{}", "{}", "ENABLED"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbcTemplate.update(
                    INSERT_ROUTE, "route-missing-group", "missing-group", "/missing-group", "GET", "activity-say-hi",
                    "ANONYMOUS", 3000L, "{}", "{}", "ENABLED"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbcTemplate.update(
                    INSERT_ROUTE_PARAMETER, "route-say-hi", "activity-create", 0, "request"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        });
    }

    private static void insertServiceCatalog(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update(
                INSERT_SERVICE, "activity-service", "activity-application", "dubbo", "", "1.0.0", "ACTIVE");
        jdbcTemplate.update(INSERT_OPERATION, "activity-say-hi", "activity-service",
                "cn.example.ActivityBooth", "sayHi", "ACTIVE");
        jdbcTemplate.update(INSERT_OPERATION, "activity-create", "activity-service",
                "cn.example.ActivityBooth", "create", "ACTIVE");
        jdbcTemplate.update(
                INSERT_OPERATION_PARAMETER, "activity-say-hi", 0, "name", "java.lang.String");
        jdbcTemplate.update(
                INSERT_OPERATION_PARAMETER, "activity-say-hi", 1, "age", "java.lang.Integer");
        jdbcTemplate.update(
                INSERT_OPERATION_PARAMETER, "activity-create", 0, "request", "cn.example.CreateRequest");
    }

    private static WebApplicationContextRunner contextRunner() {
        return new WebApplicationContextRunner()
                .withUserConfiguration(GateWeaveCenterApplication.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:gateweave-center-routes;MODE=MySQL;DB_CLOSE_DELAY=0",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=");
    }
}
