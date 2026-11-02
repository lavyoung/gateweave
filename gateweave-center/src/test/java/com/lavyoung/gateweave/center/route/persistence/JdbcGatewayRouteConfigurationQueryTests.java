package com.lavyoung.gateweave.center.route.persistence;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.lavyoung.gateweave.center.GateWeaveCenterApplication;
import com.lavyoung.gateweave.center.route.application.GatewayRouteConfigurationQuery;
import com.lavyoung.gateweave.contract.configuration.InvocationParameterConfiguration;
import com.lavyoung.gateweave.contract.configuration.RouteAuthenticationMode;
import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证路由编辑数据和服务目录能够聚合为稳定配置候选集。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
class JdbcGatewayRouteConfigurationQueryTests {

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
    void shouldProjectActiveRoutesAndBatchTheirParameters() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner("gateweave-configuration-candidates");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            GatewayRouteConfigurationQuery query = context.getBean(GatewayRouteConfigurationQuery.class);
            insertCatalog(jdbcTemplate);
            insertRoutes(jdbcTemplate);
            insertQueryParameters(jdbcTemplate);

            List<RouteConfiguration> candidates = query.findCandidates(" default ");

            assertThat(candidates).extracting(RouteConfiguration::routeId)
                    .containsExactly("route-query", "route-health");
            RouteConfiguration queryRoute = candidates.get(0);
            assertThat(queryRoute.path()).isEqualTo("/activities/query");
            assertThat(queryRoute.method()).isEqualTo("POST");
            assertThat(queryRoute.serviceId()).isEqualTo("activity-service");
            assertThat(queryRoute.authenticationMode()).isEqualTo(RouteAuthenticationMode.REQUIRED);
            assertThat(queryRoute.attributes()).containsEntry("source", "center");
            assertThat(queryRoute.target().protocol()).isEqualTo("dubbo");
            assertThat(queryRoute.target().service()).isEqualTo("cn.example.ActivityService");
            assertThat(queryRoute.target().operation()).isEqualTo("query");
            assertThat(queryRoute.target().timeout()).isEqualTo(Duration.ofSeconds(3));
            assertThat(queryRoute.target().attributes()).containsEntry("version", "1.0.0");
            assertThat(queryRoute.target().parameters())
                    .extracting(InvocationParameterConfiguration::name)
                    .containsExactly("userName", "userAge");
            assertThat(queryRoute.target().parameters())
                    .extracting(InvocationParameterConfiguration::typeName)
                    .containsExactly("java.lang.String", "java.lang.Integer");
            assertThat(candidates.get(1).target().parameters()).isEmpty();
        });
    }

    @Test
    void shouldReturnEmptyCandidatesAndRejectInvalidGroupId() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner("gateweave-configuration-empty");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            GatewayRouteConfigurationQuery query = context.getBean(GatewayRouteConfigurationQuery.class);

            assertThat(query.findCandidates("missing")).isEmpty();
            assertThatThrownBy(() -> query.findCandidates(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("gatewayGroupId must not be null");
            assertThatThrownBy(() -> query.findCandidates(" "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("gatewayGroupId must not be blank");
        });
    }

    @Test
    void shouldRejectUnsupportedAuthenticationMode() {
        // Given
        WebApplicationContextRunner contextRunner = contextRunner("gateweave-configuration-authentication");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            GatewayRouteConfigurationQuery query = context.getBean(GatewayRouteConfigurationQuery.class);
            insertActiveService(jdbcTemplate);
            insertRoute(jdbcTemplate, new RouteFixture(
                    "route-invalid", "/invalid", "GET", "activity-health", "UNSUPPORTED",
                    "{}", "{}", "ENABLED"));

            assertThatThrownBy(() -> query.findCandidates("default"))
                    .isInstanceOf(DataRetrievalFailureException.class)
                    .hasMessage("Route contains an unsupported authentication mode: route-invalid");
        });
    }

    private static void insertCatalog(JdbcTemplate jdbcTemplate) {
        insertActiveService(jdbcTemplate);
        jdbcTemplate.update(
                INSERT_OPERATION, "activity-disabled", "activity-service",
                "cn.example.ActivityService", "disabled", "DISABLED");
        jdbcTemplate.update(
                INSERT_SERVICE, "disabled-service", "disabled-application", "dubbo", "", "1.0.0", "DISABLED");
        jdbcTemplate.update(
                INSERT_OPERATION, "disabled-service-operation", "disabled-service",
                "cn.example.DisabledService", "invoke", "ACTIVE");
    }

    private static void insertActiveService(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update(
                INSERT_SERVICE, "activity-service", "activity-application", "dubbo", "", "1.0.0", "ACTIVE");
        jdbcTemplate.update(
                INSERT_OPERATION, "activity-query", "activity-service",
                "cn.example.ActivityService", "query", "ACTIVE");
        jdbcTemplate.update(
                INSERT_OPERATION, "activity-health", "activity-service",
                "cn.example.ActivityService", "health", "ACTIVE");
        jdbcTemplate.update(
                INSERT_OPERATION_PARAMETER, "activity-query", 0, "name", "java.lang.String");
        jdbcTemplate.update(
                INSERT_OPERATION_PARAMETER, "activity-query", 1, "age", "java.lang.Integer");
    }

    private static void insertRoutes(JdbcTemplate jdbcTemplate) {
        insertRoute(jdbcTemplate, new RouteFixture(
                "route-query", "/activities/query", "POST", "activity-query", "REQUIRED",
                "{\"version\":\"1.0.0\"}", "{\"source\":\"center\"}", "ENABLED"));
        insertRoute(jdbcTemplate, new RouteFixture(
                "route-health", "/activities/health", "GET", "activity-health", "ANONYMOUS",
                "{\"version\":\"1.0.0\"}", "{}", "ENABLED"));
        insertRoute(jdbcTemplate, new RouteFixture(
                "route-disabled", "/activities/disabled", "GET", "activity-query", "ANONYMOUS",
                "{}", "{}", "DISABLED"));
        insertRoute(jdbcTemplate, new RouteFixture(
                "route-disabled-operation", "/activities/disabled-operation", "GET", "activity-disabled",
                "ANONYMOUS", "{}", "{}", "ENABLED"));
        insertRoute(jdbcTemplate, new RouteFixture(
                "route-disabled-service", "/disabled-service", "GET", "disabled-service-operation",
                "ANONYMOUS", "{}", "{}", "ENABLED"));
    }

    private static void insertQueryParameters(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update(INSERT_ROUTE_PARAMETER, "route-query", "activity-query", 0, "userName");
        jdbcTemplate.update(INSERT_ROUTE_PARAMETER, "route-query", "activity-query", 1, "userAge");
    }

    private static void insertRoute(JdbcTemplate jdbcTemplate, RouteFixture fixture) {
        jdbcTemplate.update(
                INSERT_ROUTE, fixture.routeId(), "default", fixture.path(), fixture.method(), fixture.operationId(),
                fixture.authenticationMode(), 3000L, fixture.targetAttributes(), fixture.routeAttributes(),
                fixture.status());
    }

    private static WebApplicationContextRunner contextRunner(String databaseName) {
        return new WebApplicationContextRunner()
                .withUserConfiguration(GateWeaveCenterApplication.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:%s;MODE=MySQL;DB_CLOSE_DELAY=0".formatted(databaseName),
                        "spring.datasource.username=sa",
                        "spring.datasource.password=");
    }

    private record RouteFixture(
            String routeId, String path, String method, String operationId, String authenticationMode,
            String targetAttributes, String routeAttributes, String status) {
    }
}
