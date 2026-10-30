package com.lavyoung.gateweave.center.catalog.adapter.out.persistence;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.lavyoung.gateweave.center.GateWeaveCenterApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceCatalogMigrationTests {

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
    private static final String INSERT_PARAMETER = """
            INSERT INTO gw_service_operation_parameter
                (operation_id, parameter_index, parameter_name, type_name)
            VALUES (?, ?, ?, ?)
            """;

    @Test
    void shouldCreateServiceCatalogTablesAtVersionOne() {
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
            assertThat(tableNames).contains(
                    "gw_service", "gw_service_operation", "gw_service_operation_parameter");
        });
    }

    @Test
    void shouldPersistOrderedParametersAndEnforceCatalogConstraints() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner();

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

            jdbcTemplate.update(
                    INSERT_SERVICE, "activity-service", "activity-application", "dubbo", "", "1.0.0", "ACTIVE");
            jdbcTemplate.update(INSERT_OPERATION, "activity-say-hi", "activity-service",
                    "cn.example.ActivityBooth", "sayHi", "ACTIVE");
            jdbcTemplate.update(INSERT_PARAMETER, "activity-say-hi", 0, "name", "java.lang.String");
            jdbcTemplate.update(INSERT_PARAMETER, "activity-say-hi", 1, "age", "java.lang.Integer");

            List<Integer> parameterIndexes = jdbcTemplate.queryForList("""
                    SELECT parameter_index
                    FROM gw_service_operation_parameter
                    ORDER BY parameter_index
                    """, Integer.class);
            assertThat(parameterIndexes).containsExactly(0, 1);
            assertThatThrownBy(() -> jdbcTemplate.update(
                    INSERT_SERVICE, "activity-service", "another-application", "dubbo", "", "1.0.0", "ACTIVE"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbcTemplate.update(
                    INSERT_PARAMETER, "activity-say-hi", 1, "duplicated", "java.lang.String"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbcTemplate.update(
                    INSERT_OPERATION, "missing-service-operation", "missing-service",
                    "cn.example.MissingService", "invoke", "ACTIVE"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        });
    }

    private static WebApplicationContextRunner contextRunner() {
        return new WebApplicationContextRunner()
                .withUserConfiguration(GateWeaveCenterApplication.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:gateweave-center-catalog;MODE=MySQL;DB_CLOSE_DELAY=0",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=",
                        "spring.flyway.target=1");
    }
}
